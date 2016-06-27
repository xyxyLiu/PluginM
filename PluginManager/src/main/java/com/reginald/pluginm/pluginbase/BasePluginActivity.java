package com.reginald.pluginm.pluginbase;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.reginald.pluginm.DexClassLoaderPluginManager;
import com.reginald.pluginm.PluginInfo;

/**
 * Created by lxy on 16-6-6.
 */
public class BasePluginActivity extends Activity{
    private static final String TAG = "BasePluginActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        Log.d(TAG, "attachBaseContext() classloader = " + getClass().getClassLoader());
        PluginInfo pluginInfo = DexClassLoaderPluginManager.getPluginInfo(getClass().getClassLoader());
        if (pluginInfo == null) {
            Log.e(TAG, "attachBaseContext() error! No pluginInfo found!");
            super.attachBaseContext(newBase);
            return;
        }

        Context pluginContext = DexClassLoaderPluginManager.createPluginContext(pluginInfo, newBase);
        Log.d(TAG, "attachBaseContext() plugin activity context = " + pluginContext);
        super.attachBaseContext(pluginContext);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
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

    @Override
    public ComponentName startService(Intent intent) {
        Intent pluginIntent = DexClassLoaderPluginManager.getInstance(getApplicationContext()).getPluginServiceIntent(
                intent.getComponent().getPackageName(), intent.getComponent().getClassName());
        if(pluginIntent != null) {
            return super.startService(pluginIntent);
        } else {
            return super.startService(intent);
        }
    }




}
