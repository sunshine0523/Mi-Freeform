package com.sunshine.freeform;

import java.util.Date;

/**
 * @author sunshine
 * @date 2021/3/14
 */
public class MyLog {

    private static StringBuilder logBuilder = new StringBuilder();

    private static long MAX_SIZE = 100000;

    /**
     * 日志过多时应该适当清理
     */
    private static void needFree() {
        if (logBuilder.length() > MAX_SIZE) {
            logBuilder.delete(0, logBuilder.length());
            logBuilder.append("-----some log was delete because log is to big-----\n");
        }
    }

    public static void d(String log) {
        needFree();
        logBuilder.append(new Date(System.currentTimeMillis())).append("\ndebug:").append(log).append("\n");
    }

    public static void e(String log) {
        needFree();
        logBuilder.append(new Date(System.currentTimeMillis())).append("\nerror:").append(log).append("\n");
    }

    public static String getLog() {
        return logBuilder.toString();
    }
}
