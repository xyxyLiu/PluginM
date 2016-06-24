package com.example.multidexmodeplugin.pluginbase;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.multidexmodeplugin.AssetsManager;
import com.example.multidexmodeplugin.DexClassLoaderPluginManager;
import com.example.multidexmodeplugin.ResourcesManager;

/**
 * Created by lxy on 16-6-6.
 */
public class BasePluginActivity extends Activity{
    private static final String TAG = "BasePluginActivity";

    private Resources mOldResources;
    private Resources mPluginResources;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        replaceResources();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void replaceResources() {
        mOldResources = getResources();
        String apkPath = AssetsManager.getApkPath(this, "testplugin-debug.apk");
        Log.d(TAG, "replaceResources() apkPath = " + apkPath);
        mPluginResources = ResourcesManager.getPluginResources(this, apkPath);
//        try {
//            Log.d(TAG, "replaceResources() mPluginResources.getLayout(0x7f040019) = " + mPluginResources.getLayout(0x7f040019));
//        } catch (Resources.NotFoundException e) {
//            Log.d(TAG, "replaceResources() mOldResources error!");
//            e.printStackTrace();
//        }

        Log.d(TAG, "replaceResources() mOldResources = " + mOldResources);
        Log.d(TAG, "replaceResources() mPluginResources = " + mPluginResources);
    }

    @Override
    public Resources getResources() {
        if (mPluginResources != null) {
            return mPluginResources;
        }
        return super.getResources();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle bundle) {
        Intent pluginIntent = DexClassLoaderPluginManager.getInstance(getApplicationContext()).getPluginActivityIntent(
                intent.getComponent().getPackageName(), intent.getComponent().getClassName());
        if(pluginIntent != null) {
            super.startActivityForResult(pluginIntent, requestCode, bundle);
        } else {
            super.startActivityForResult(intent, requestCode, bundle);
        }

    }


}
