package com.example.testplugin;

import android.util.Log;

import com.reginald.pluginm.pluginbase.BasePluginApplication;

/**
 * Created by lxy on 16-6-28.
 */
public class PluginApplication extends BasePluginApplication{

    private static final String TAG = "PluginApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }
}
