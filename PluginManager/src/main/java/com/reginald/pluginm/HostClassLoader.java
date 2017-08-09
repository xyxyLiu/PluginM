package com.reginald.pluginm;

import android.content.Context;
import android.util.Log;

import com.reginald.pluginm.stub.ActivityStub;

/**
 * Created by lxy on 16-6-22.
 */
public class HostClassLoader extends ClassLoader {
    private static final String TAG = "HostClassLoader";
    private ClassLoader mOldClassLoader;
    private Context mContext;

    public HostClassLoader(Context hostContext, ClassLoader oldClassLoader) {
//        super(oldClassLoader);
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
