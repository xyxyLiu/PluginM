package com.reginald.pluginm;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.stub.ActivityStub;

import java.lang.reflect.Field;

/**
 * Created by lxy on 16-6-22.
 */
public class HostClassLoader extends ClassLoader {
    private static final String TAG = "HostClassLoader";
    private ClassLoader mOldClassLoader;
    private Context mContext;

    public static ClassLoader install(Application app) {
        try {
            Field mBaseField = FieldUtils.getField(ContextWrapper.class, "mBase");
            Object mBaseObj = mBaseField.get(app);
            Field mPackageInfoField = FieldUtils.getField(mBaseObj.getClass(), "mPackageInfo");
            Object mPackageInfoObj = mPackageInfoField.get(mBaseObj);
            Field mClassLoaderField = FieldUtils.getField(mPackageInfoObj.getClass(), "mClassLoader");
            ClassLoader loader = (ClassLoader) mClassLoaderField.get(mPackageInfoObj);
            ClassLoader newLoader = new HostClassLoader(app, loader);
            FieldUtils.writeField(mClassLoaderField, mPackageInfoObj, newLoader);
            return newLoader;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HostClassLoader(Context hostContext, ClassLoader oldClassLoader) {
        Log.d(TAG, "mOldClassLoader = " + oldClassLoader);
        Log.d(TAG, "HostClassLoader = " + this);
        mContext = hostContext;
        mOldClassLoader = oldClassLoader;
    }



    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {

        Log.d(TAG, "loadClass() className = " + className + " , resolve = " + resolve + " ,ActivityStub.class.getName() = " + ActivityStub.class.getName());

        if (className.startsWith(ActivityStub.class.getName())) {
            try {
                Class<?> clazz = PluginManager.getInstance(mContext).findPluginClass(className);
                Log.d(TAG, "loadClass() use plugin loaders " + className + " ok! clazz = " + clazz);
                return clazz;
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "loadClass() use plugin loaders " + className + " fail!");
            }
        }

        try {
            Class<?> clazz = mOldClassLoader.loadClass(className);
            Log.d(TAG, "loadClass() use old one " + className + " ok! clazz = " + clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
            Log.d(TAG, "loadClass() use old one " + className + " fail!");
            throw e;
        }
    }

}
