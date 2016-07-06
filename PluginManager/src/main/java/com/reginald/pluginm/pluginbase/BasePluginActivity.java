package com.reginald.pluginm.pluginbase;

import android.app.Activity;
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
        PluginInfo pluginInfo = DexClassLoaderPluginManager.getPluginInfo(getClass().getClassLoader());
        if (pluginInfo == null) {
            super.attachBaseContext(newBase);
            return;
        }

        Context pluginContext = DexClassLoaderPluginManager.createPluginContext(pluginInfo, newBase);
        super.attachBaseContext(pluginContext);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        test();
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle bundle) {
        Intent pluginIntent = DexClassLoaderPluginManager.getInstance(getApplicationContext()).getPluginActivityIntent(intent);
        if(pluginIntent != null) {
            super.startActivityForResult(pluginIntent, requestCode, bundle);
        } else {
            super.startActivityForResult(intent, requestCode, bundle);
        }
    }

    private void test() {
        Log.d(TAG, "getFilesDir() = " + getFilesDir());
        Log.d(TAG, "getCacheDir() = " + getCacheDir());
        Log.d(TAG, "getApplication() = " + getApplication());
        Log.d(TAG, "getApplicationContext() = " + getApplicationContext());
    }


}
