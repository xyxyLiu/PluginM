package com.example.multidexmodeplugin.pluginhost;

import android.content.Context;
import android.util.Log;

import com.example.multidexmodeplugin.DexClassLoaderPluginManager;

/**
 * Created by lxy on 16-6-22.
 */
public class HostClassLoader extends ClassLoader {
    private static final String TAG = "HostClassLoader";
    private ClassLoader mOldClassLoader;
    private DexClassLoaderPluginManager mDexClassLoaderPluginManager;

    public HostClassLoader(DexClassLoaderPluginManager dexClassLoaderPluginManager, ClassLoader oldClassLoader) {
        mOldClassLoader = oldClassLoader;
        mDexClassLoaderPluginManager = dexClassLoaderPluginManager;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {

        Log.d(TAG, "loadClass() " + className);

        try {
            return mOldClassLoader.loadClass(className);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mDexClassLoaderPluginManager.findPluginClass(className);
    }
}
