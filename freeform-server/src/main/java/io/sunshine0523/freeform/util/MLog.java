package io.sunshine0523.freeform.util;

import android.util.Log;

public class MLog {
    public static void i(String tag, String message) {
        Log.i(tag, message);
        DataHelper.INSTANCE.appendLog("[i] " + tag + "\t" + message);
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
        DataHelper.INSTANCE.appendLog("[w] " + tag + "\t" + message);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
        DataHelper.INSTANCE.appendLog("[e] " + tag + "\t" + message);
    }
}
