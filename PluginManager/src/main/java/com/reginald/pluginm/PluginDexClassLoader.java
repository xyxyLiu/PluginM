package com.reginald.pluginm;

import android.util.Log;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-24.
 */
public class PluginDexClassLoader extends DexClassLoader {

    private static final String TAG = "PluginDexClassLoader";

    ClassLoader testparent;

    public PluginDexClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
        Log.d(TAG, "PluginDexClassLoader() " + this);
        Log.d(TAG, "PluginDexClassLoader() parent = " + parent);
//        try {
//            Log.d(TAG, "PluginDexClassLoader() parent load DexClassLoaderPluginManager = " + parent.loadClass("com.example.multidexmodeplugin.DexClassLoaderPluginManager"));
//        } catch (Exception e) {
//            Log.d(TAG, "PluginDexClassLoader() parent load DexClassLoaderPluginManager error: " + e);
//        }
        testparent = parent;
    }

    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Log.d(TAG, "loadClass() classname = " + className + " , resolve = " + resolve);
//        try {
//            Log.d(TAG, "loadClass() try parent load = " + testparent.loadClass(className));
//        } catch (Exception e ){
//            e.printStackTrace();
//            Log.d(TAG, "loadClass() try parent load error! ");
//        }

//        Class<?> clazz = findLoadedClass(className);
//        Log.d(TAG, "loadClass() findLoadedClass = " +clazz);
//        if (clazz == null) {
//            ClassNotFoundException suppressed = null;
//            try {
//                clazz = testparent.loadClass(className);
//            } catch (ClassNotFoundException e) {
//                suppressed = e;
//            }
//
//
//            Log.d(TAG, "loadClass() parent didn't found " +clazz);
//
//            if (clazz == null) {
//                try {
//                    Log.d(TAG, "loadClass() findclass " +clazz);
//                    clazz = findClass(className);
//                } catch (ClassNotFoundException e) {
////                    e.addSuppressed(suppressed);
//                    throw e;
//                }
//            }
//        }
//
//        return clazz;

        try {
            Class<?> clazz = super.loadClass(className, resolve);
            Log.d(TAG, "loadClass() classname = " + className + " ok!");
            return clazz;
        } catch (Exception e) {
            Log.e(TAG, "loadClass() classname = " + className + " fail!");
            throw e;
        }
    }

    public String toString() {
        return "@" + hashCode() + "  " + super.toString();
    }


}
