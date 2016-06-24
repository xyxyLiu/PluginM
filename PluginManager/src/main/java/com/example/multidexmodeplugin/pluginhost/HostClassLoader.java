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
        Log.d(TAG, "mOldClassLoader = " + oldClassLoader);
        Log.d(TAG, "HostClassLoader = " + this);
        mOldClassLoader = oldClassLoader;
        mDexClassLoaderPluginManager = dexClassLoaderPluginManager;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {

        Log.d(TAG, "loadClass() className = " + className);

        try {
            return mOldClassLoader.loadClass(className);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mDexClassLoaderPluginManager.findPluginClass(className);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {

        Log.d(TAG, "loadClass() className = " + className + " , resolve = " + resolve);

        try {
            return mOldClassLoader.loadClass(className);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
