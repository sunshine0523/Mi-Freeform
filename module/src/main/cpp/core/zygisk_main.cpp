#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <fcntl.h>
#include <nativehelper/scoped_utf_chars.h>
#include <sys/mman.h>
#include <logging.h>
#include <zygisk.hpp>
#include <socket.h>
#include <misc.h>
#include <dex_file.h>
#include <config.h>
#include <memory.h>
#include <sys/system_properties.h>
#include "settings_process.h"

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ServerSpecializeArgs;

inline constexpr auto kProcessNameMax = 256;

enum Identity : int {
    IGNORE = 0,
    SYSTEM_SERVER = 1,
    SYSTEM_UI = 2,
    SETTINGS = 3,
};

class ZygiskModule : public zygisk::ModuleBase {
public:
    void onLoad(Api *api, JNIEnv *env) override {
        this->api = api;
        this->env = env;
    }

    void preAppSpecialize(AppSpecializeArgs *args) override {
        char process_name[kProcessNameMax]{0};
        char app_data_dir[PATH_MAX]{0};

        if (args->nice_name) {
            ScopedUtfChars niceName{env, args->nice_name};
            strcpy(process_name, niceName.c_str());
        }

#ifdef DEBUG
        if (args->app_data_dir) {
            ScopedUtfChars appDataDir{env, args->app_data_dir};
            strcpy(app_data_dir, appDataDir.c_str());
        }
#endif
        //LOGD("preAppSpecialize: %s %s", process_name, app_data_dir);

        InitCompanion(false, args->uid, process_name);

        if (whoami == Identity::IGNORE) {
            api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
        }
    }

    void postAppSpecialize(const AppSpecializeArgs *args) override {
        char process_name[kProcessNameMax]{0};
        if (args->nice_name) {
            ScopedUtfChars niceName{env, args->nice_name};
            strcpy(process_name, niceName.c_str());
            char app_data_dir[PATH_MAX]{0};

            if (args->app_data_dir) {
                ScopedUtfChars appDataDir{env, args->app_data_dir};
                strcpy(app_data_dir, appDataDir.c_str());
            }

            if (strcmp(process_name, SETTINGS_APPLICATION_ID) == 0) {
                Settings::main(env, app_data_dir, dex);
            } else {
                api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
            }
        } else {
            api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
        }

    }

private:
    Api *api;
    JNIEnv *env;
    Identity whoami = Identity::IGNORE;
    Dex *dex = nullptr;

    void InitCompanion(bool is_system_server, int uid, const char *process_name = nullptr) {
        auto companion = api->connectCompanion();
        if (companion == -1) {
            LOGE("Zygote: failed to connect to companion");
            return;
        }

        if (is_system_server) {
            write_int(companion, 1);
            whoami = Identity::SYSTEM_SERVER;
        } else {
            write_int(companion, 0);
            write_int(companion, uid);
            write_full(companion, process_name, kProcessNameMax);
            whoami = static_cast<Identity>(read_int(companion));
        }

        if (whoami != Identity::IGNORE) {
            auto fd = recv_fd(companion);
            auto size = (size_t) read_int(companion);

            if (whoami == Identity::SETTINGS) {
                LOGI("Zygote: in Settings");
            } else if (whoami == Identity::SYSTEM_UI) {
                LOGI("Zygote: in SystemUi");
            } else {
                LOGI("Zygote: in SystemServer");
            }

            LOGI("Zygote: dex fd is %d, size is %zu", fd, size);
            dex = new Dex(fd, size);
            close(fd);
        }

        close(companion);
    }
};

static int dex_mem_fd = -1;
static size_t dex_size = 0;
static char settings_process[kProcessNameMax];

static bool PrepareCompanion() {
    bool result = false;

    auto path = "/data/adb/mi_freeform/" DEX_NAME;
    int fd = open(path, O_RDONLY);
    ssize_t size;

    if (fd == -1) {
        LOGE("open %s", path);
        goto cleanup;
    }

    size = lseek(fd, 0, SEEK_END);
    if (size == -1) {
        LOGE("lseek %s", path);
        goto cleanup;
    }
    lseek(fd, 0, SEEK_SET);

    LOGD("Companion: dex size is %zu", size);

    dex_mem_fd = CreateSharedMem("mi_freeform.dex", size);
    if (dex_mem_fd >= 0) {
        auto addr = (uint8_t *) mmap(nullptr, size, PROT_WRITE, MAP_SHARED, dex_mem_fd, 0);
        if (addr != MAP_FAILED) {
            read_full(fd, addr, size);
            dex_size = size;
            munmap(addr, size);
        }
        SetSharedMemProt(dex_mem_fd, PROT_READ);
    }

    LOGI("Companion: dex fd is %d", dex_mem_fd);

    result = true;

    cleanup:
    if (fd != -1) close(fd);

    return result;
}

static void companion_handler(int socket) {
    static auto prepare = PrepareCompanion();

    char process_name[kProcessNameMax]{0};
    Identity whoami;

    int is_system_server = read_int(socket) == 1;
    if (is_system_server != 0) {
        whoami = Identity::SYSTEM_SERVER;
    } else {
        int uid = read_int(socket);
        read_full(socket, process_name, kProcessNameMax);

        if (strcmp(process_name, SETTINGS_APPLICATION_ID) == 0) {
            whoami = Identity::SETTINGS;
        } else {
            whoami = Identity::IGNORE;
        }

        write_int(socket, whoami);
    }

    if (whoami != Identity::IGNORE) {
        send_fd(socket, dex_mem_fd);
        write_int(socket, dex_size);
    }

    close(socket);
}

// Register our module class and the companion handler function
REGISTER_ZYGISK_MODULE(ZygiskModule)
REGISTER_ZYGISK_COMPANION(companion_handler)
