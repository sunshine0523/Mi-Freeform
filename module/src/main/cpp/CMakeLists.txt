cmake_minimum_required(VERSION 3.22)
set(CMAKE_CXX_STANDARD 17)

project("mf")

set(LINKER_FLAGS "-ffixed-x18 -Wl,--hash-style=both")
set(C_FLAGS "-Werror=format -fdata-sections -ffunction-sections -fno-exceptions -fno-rtti -fno-threadsafe-statics")

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(C_FLAGS "${C_FLAGS} -O2 -fvisibility=hidden -fvisibility-inlines-hidden")
    set(LINKER_FLAGS "${LINKER_FLAGS} -Wl,-exclude-libs,ALL -Wl,--gc-sections")
else ()
    add_definitions(-DDEBUG)
    set(C_FLAGS "${C_FLAGS} -O0")
endif ()

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${C_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${C_FLAGS}")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

find_package(nativehelper REQUIRED CONFIG)

include_directories("include")
include_directories("util/include")

add_library(util STATIC
        util/misc.cpp
        util/dex_file.cpp
        util/android.cpp
        util/plt.c
        util/selinux.cpp
        util/memory.cpp
        util/app_process.cpp
        util/socket.cpp)
target_link_libraries(util log nativehelper::nativehelper_header_only)

add_library(mf SHARED
        core/zygisk_main.cpp
        core/settings_process.cpp
        core/binder_hook.cpp
        )
target_link_libraries(mf util log nativehelper::nativehelper_header_only)