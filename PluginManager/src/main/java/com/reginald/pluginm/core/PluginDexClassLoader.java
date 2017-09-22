package com.reginald.pluginm.core;

import com.reginald.pluginm.pluginapi.PluginHelper;
import com.reginald.pluginm.utils.Logger;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-24.
 */
public class PluginDexClassLoader extends DexClassLoader {

    private static final String TAG = "PluginDexClassLoader";

    ClassLoader mHost;

    public PluginDexClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent, ClassLoader extra) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
        Logger.d(TAG, "PluginDexClassLoader() " + this);
        Logger.d(TAG, "PluginDexClassLoader() parent = " + parent);
//        try {
//            Logger.d(TAG, "PluginDexClassLoader() parent load DexClassLoaderPluginManager = " + parent.loadClass("com.example.multidexmodeplugin.DexClassLoaderPluginManager"));
//        } catch (Exception e) {
//            Logger.d(TAG, "PluginDexClassLoader() parent load DexClassLoaderPluginManager error: " + e);
//        }
        mHost = extra;
    }

    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Logger.d(TAG, "loadClass() classname = " + className + " , resolve = " + resolve);

        ClassNotFoundException exception = null;

        if (canUseHostLoader1(className)) {
            try {
                return mHost.loadClass(className);
            } catch (ClassNotFoundException e) {
                Logger.e(TAG, "loadClass() classname = " + className + " host load fail!");
                exception = e;
            }
        }

        try {
            Class<?> clazz = super.loadClass(className, resolve);
            Logger.d(TAG, "loadClass() classname = " + className + " ok!");
            return clazz;
        } catch (ClassNotFoundException e) {
            Logger.e(TAG, "loadClass() classname = " + className + " fail!");
            exception = e;
        }

        if (canUseHostLoader2(className)) {
            try {
                return mHost.loadClass(className);
            } catch (ClassNotFoundException e) {
                Logger.e(TAG, "loadClass() classname = " + className + " host load fail!");
                exception = e;
            }
        }

        if (exception != null) {
            throw exception;
        }

        throw new ClassNotFoundException(String.format("plugin class %s NOT found", className));
    }

    private boolean canUseHostLoader1(String className) {
        return className.startsWith(PluginHelper.class.getPackage().getName());
    }

    private boolean canUseHostLoader2(String className) {
        return false;
    }

    public String toString() {
        return "@" + hashCode() + "  " + super.toString();
    }


}
