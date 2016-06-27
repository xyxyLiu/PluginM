package com.reginald.pluginm;

import android.util.Log;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-24.
 */
public class PluginDexClassLoader extends DexClassLoader {

    private static final String TAG = "PluginDexClassLoader";

    public PluginDexClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
        Log.d(TAG, "PluginDexClassLoader() " + this);
        Log.d(TAG, "PluginDexClassLoader() parent = " + parent);
    }

    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Log.d(TAG, "loadClass() classname = " + className + " , resolve = " + resolve);
        return super.loadClass(className, resolve);
    }

    public String toString() {
        return "@" + hashCode() + "  " + super.toString();
    }


}
