package com.reginald.pluginm;

import com.reginald.pluginm.utils.Logger;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-24.
 */
public class PluginDexClassLoader extends DexClassLoader {

    private static final String TAG = "PluginDexClassLoader";

    ClassLoader testparent;

    public PluginDexClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
        Logger.d(TAG, "PluginDexClassLoader() " + this);
        Logger.d(TAG, "PluginDexClassLoader() parent = " + parent);
//        try {
//            Logger.d(TAG, "PluginDexClassLoader() parent load DexClassLoaderPluginManager = " + parent.loadClass("com.example.multidexmodeplugin.DexClassLoaderPluginManager"));
//        } catch (Exception e) {
//            Logger.d(TAG, "PluginDexClassLoader() parent load DexClassLoaderPluginManager error: " + e);
//        }
        testparent = parent;
    }

    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Logger.d(TAG, "loadClass() classname = " + className + " , resolve = " + resolve);
//        try {
//            Logger.d(TAG, "loadClass() try parent load = " + testparent.loadClass(className));
//        } catch (Exception e ){
//            e.printStackTrace();
//            Logger.d(TAG, "loadClass() try parent load error! ");
//        }

//        Class<?> clazz = findLoadedClass(className);
//        Logger.d(TAG, "loadClass() findLoadedClass = " +clazz);
//        if (clazz == null) {
//            ClassNotFoundException suppressed = null;
//            try {
//                clazz = testparent.loadClass(className);
//            } catch (ClassNotFoundException e) {
//                suppressed = e;
//            }
//
//
//            Logger.d(TAG, "loadClass() parent didn't found " +clazz);
//
//            if (clazz == null) {
//                try {
//                    Logger.d(TAG, "loadClass() findclass " +clazz);
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
            Logger.d(TAG, "loadClass() classname = " + className + " ok!");
            return clazz;
        } catch (Exception e) {
            Logger.e(TAG, "loadClass() classname = " + className + " fail!");
            throw e;
        }
    }

    public String toString() {
        return "@" + hashCode() + "  " + super.toString();
    }


}
