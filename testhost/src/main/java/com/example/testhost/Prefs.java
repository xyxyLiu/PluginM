package com.example.testhost;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lxy on 18-7-4.
 */

public class Prefs {

    public static class Config {
        private static final String FILE = "pluginm_config";
        private static final String KEY_PROCESS_TYPE = "process_type";
        private static final String KEY_USE_HOST_CLASSLOADER = "use_host_classloader";
        private static final String HOOK_HOST_CONTEXT = "hook_host_context";
        private static final String HOOK_SYSTEM_SERVICE = "hook_system_service";

        public static void setProcessType(Context context, int processType) {
            putInt(context, FILE, KEY_PROCESS_TYPE, processType);
        }

        public static int getProcessType(Context context, int defValue) {
            return getInt(context, FILE, KEY_PROCESS_TYPE, defValue);
        }

        public static void setUseHostClassloader(Context context, boolean bool) {
            putBoolean(context, FILE, KEY_USE_HOST_CLASSLOADER, bool);
        }

        public static boolean getUseHostClassloader(Context context, boolean defValue) {
            return getBoolean(context, FILE, KEY_USE_HOST_CLASSLOADER, defValue);
        }

        public static void setHookHostContext(Context context, boolean bool) {
            putBoolean(context, FILE, HOOK_HOST_CONTEXT, bool);
        }

        public static boolean getHookHostContext(Context context, boolean defValue) {
            return getBoolean(context, FILE, HOOK_HOST_CONTEXT, defValue);
        }

        public static void setHookSystemService(Context context, boolean bool) {
            putBoolean(context, FILE, HOOK_SYSTEM_SERVICE, bool);
        }

        public static boolean getHookSystemService(Context context, boolean defValue) {
            return getBoolean(context, FILE, HOOK_SYSTEM_SERVICE, defValue);
        }
    }

    private static void putBoolean(Context cxt, String prefsFile, String key, boolean defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, defValue).commit();
    }

    private static void putInt(Context cxt, String prefsFile, String key, int defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        prefs.edit().putInt(key, defValue).commit();
    }

    private static void putLong(Context cxt, String prefsFile, String key, long defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        prefs.edit().putLong(key, defValue).commit();
    }

    private static void putFloat(Context cxt, String prefsFile, String key, float defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        prefs.edit().putFloat(key, defValue).commit();
    }

    private static void putString(Context cxt, String prefsFile, String key, String defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        prefs.edit().putString(key, defValue).commit();
    }

    private static boolean getBoolean(Context cxt, String prefsFile, String key, boolean defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defValue);
    }

    private static int getInt(Context cxt, String prefsFile, String key, int defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        return prefs.getInt(key, defValue);
    }

    private static long getLong(Context cxt, String prefsFile, String key, long defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        return prefs.getLong(key, defValue);
    }

    private static float getFloat(Context cxt, String prefsFile, String key, float defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        return prefs.getFloat(key, defValue);
    }

    private static String getString(Context cxt, String prefsFile, String key, String defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        return prefs.getString(key, defValue);
    }

    private static boolean contains(Context cxt, String prefsFile, String key) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        return prefs.contains(key);
    }

    private static void remove(Context cxt, String prefsFile, String key) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        prefs.edit().remove(key).commit();
    }

    private static void clear(Context cxt, String prefsFile) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

}
