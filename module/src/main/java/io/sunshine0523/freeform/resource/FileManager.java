package io.sunshine0523.freeform.resource;

import android.os.ParcelFileDescriptor;
import android.os.SELinux;
import android.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.sunshine0523.freeform.utils.Logger;

public class FileManager {
    private static final String TAG = "Mi-Freeform/FileManager";
    static final Path basePath = Paths.get("/data/user_de/0/com.android.settings/mi_freeform");
    static final Path managerApkPath = basePath.resolve("manager.apk");
    private static ParcelFileDescriptor fd = null;

    static {
        try {
            Files.createDirectories(basePath);
            SELinux.setFileContext(basePath.toString(), "u:object_r:system_file:s0");
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static ParcelFileDescriptor getManagerApk() throws IOException {
        if (fd != null) return fd.dup();
        SELinux.setFileContext(managerApkPath.toString(), "u:object_r:system_file:s0");
        fd = ParcelFileDescriptor.open(managerApkPath.toFile(), ParcelFileDescriptor.MODE_READ_ONLY);
        return fd.dup();
    }
}
