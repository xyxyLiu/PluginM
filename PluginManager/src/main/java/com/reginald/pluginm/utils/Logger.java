package com.reginald.pluginm.utils;

import android.util.Log;

import com.reginald.pluginm.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by lxy on 17-8-21.
 */

public class Logger {
    public static final String TAG = "PluginM";
    private static boolean LOG_ENABLED = BuildConfig.DEBUG_LOG;

    public static void d(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.d(TAG, getLogMsg(tag, msg));
        }
    }

    public static void i(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.i(TAG, getLogMsg(tag, msg));
        }
    }

    public static void w(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.w(TAG, getLogMsg(tag, msg));
        }
    }

    public static void w(String tag, String msg, Throwable e) {
        if (LOG_ENABLED) {
            Log.w(TAG, getLogMsg(tag, msg + " Exception: " + getExceptionMsg(e)));
        }
    }

    public static void e(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.e(TAG, getLogMsg(tag, msg));
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (LOG_ENABLED) {
            Log.e(TAG, getLogMsg(tag, msg + " Exception: " + getExceptionMsg(e)));
        }
    }

    private static String getLogMsg(String subTag, String msg) {
        return "[" + subTag + "] " + msg;
    }

    private static String getExceptionMsg(Throwable e) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
