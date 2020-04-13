package com.reginald.pluginm.core;

import android.text.TextUtils;

import com.reginald.pluginm.BuildConfig;
import com.reginald.pluginm.PluginM;
import com.reginald.pluginm.pluginapi.PluginHelper;
import com.reginald.pluginm.utils.Logger;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-24.
 */
public class PluginDexClassLoader extends DexClassLoader {

    private static final String TAG = "PluginDexClassLoader";
    private static final boolean LOADER_DEBUG = BuildConfig.DEBUG_LOG && false;
    private static final String[] HOST_LOAD_LIST = new String[]{
            PluginHelper.class.getPackage().getName()
    };

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

        Class<?> hostClazz = tryLoadFromHostList(className, resolve);
        if (hostClazz != null) {
            if (LOADER_DEBUG) {
                Logger.d(TAG, "loadClass() from host list: classname = %s ok!",
                        HOST_LOAD_LIST, className);
            }
            return hostClazz;
        }

        try {
            Class<?> clazz = super.loadClass(className, resolve);
            if (LOADER_DEBUG) {
                Logger.d(TAG, "loadClass() plugin loader: classname = " + className + " ok!");
            }
            return clazz;
        } catch (ClassNotFoundException pluginException) {
            if (LOADER_DEBUG) {
                Logger.e(TAG, "loadClass() plugin loader:  classname = " + className + " fail!");
            }

            if (canUseHostLoader(className)) {
                try {
                    Class<?> clazz = mHost.loadClass(className);
                    if (LOADER_DEBUG) {
                        Logger.d(TAG, "loadClass() host loader: classname = " + className + " ok!");
                    }
                    return clazz;
                } catch (ClassNotFoundException hostException) {
                    if (LOADER_DEBUG) {
                        Logger.e(TAG, "loadClass() host loader: classname = " + className + " host load fail!");
                    }
                    throw new ClassNotFoundException(String.format("plugin class {%s} is NOT found both in PLUGIN "
                            + "CLASSLOADER {%s} and HOST CLASSLOADER {%s}", className, this, mHost));
                }
            } else {
                throw new ClassNotFoundException(String.format("plugin class {%s} is NOT found in PLUGIN CLASSLOADER "
                        + "{%s}", className, this));
            }
        }
    }

    private Class<?> tryLoadFromHostList(String className, boolean resolve) {
        for (String prefix : HOST_LOAD_LIST) {
            if (className.startsWith(prefix)) {
                try {
                    Class<?> clazz = mHost.loadClass(className);
                    if (LOADER_DEBUG) {
                        Logger.d(TAG, "tryLoadFromHostList() host loader: classname = " + className + " ok!");
                    }
                    return clazz;
                } catch (ClassNotFoundException hostException) {
                    if (LOADER_DEBUG) {
                        Logger.e(TAG, "tryLoadFromHostList() host loader: classname = " + className + " host load fail!");
                    }
                }
            }
        }

        return null;
    }

    private boolean canUseHostLoader(String className) {
        boolean configUseHost = PluginM.getConfigs().isUseHostLoader();
        return configUseHost || isPluginApiClass(className);
    }

    private boolean isPluginApiClass(String className) {
        if (!TextUtils.isEmpty(className)) {
            return className.startsWith(PluginHelper.class.getPackage().getName());
        }
        return false;
    }

    public String toString() {
        return "@" + hashCode() + "  " + super.toString();
    }


}
