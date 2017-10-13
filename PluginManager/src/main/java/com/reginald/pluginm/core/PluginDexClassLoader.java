package com.reginald.pluginm.core;

import com.reginald.pluginm.BuildConfig;
import com.reginald.pluginm.PluginM;
import com.reginald.pluginm.utils.Logger;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-24.
 */
public class PluginDexClassLoader extends DexClassLoader {

    private static final String TAG = "PluginDexClassLoader";
    private static final boolean LOADER_DEBUG = BuildConfig.DEBUG_LOG && false;

    private final ClassLoader mHost;

    public PluginDexClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent, ClassLoader extra) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
        Logger.d(TAG, "PluginDexClassLoader() " + this + ", parent = " + parent);
        mHost = extra;
    }

    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        if (LOADER_DEBUG) {
            Logger.d(TAG, "loadClass() classname = " + className + " , resolve = " + resolve);
        }

        ClassNotFoundException exception = null;

        try {
            Class<?> clazz = super.loadClass(className, resolve);
            if (LOADER_DEBUG) {
                Logger.d(TAG, "loadClass() plugin loader: classname = " + className + " ok!");
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            if (LOADER_DEBUG) {
                Logger.e(TAG, "loadClass() plugin loader:  classname = " + className + " fail!");
            }
            exception = e;
        }

        if (canUseHostLoader(className)) {
            try {
                Class<?> clazz = mHost.loadClass(className);
                if (LOADER_DEBUG) {
                    Logger.d(TAG, "loadClass() host loader: classname = " + className + " ok!");
                }
                return clazz;
            } catch (ClassNotFoundException e) {
                if (LOADER_DEBUG) {
                    Logger.e(TAG, "loadClass() host loader: classname = " + className + " host load fail!");
                }
                exception = e;
            }
        }

        if (exception != null) {
            throw exception;
        }

        throw new ClassNotFoundException(String.format("plugin class %s NOT found", className));
    }

    private boolean canUseHostLoader(String className) {
        return PluginM.getConfigs().isUseHostLoader();
    }

    public String toString() {
        return "@" + hashCode() + "  " + super.toString();
    }


}
