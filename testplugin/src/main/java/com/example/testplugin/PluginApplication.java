package com.example.testplugin;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Created by lxy on 16-6-28.
 */
public class PluginApplication extends Application {

    private static final String TAG = "PluginApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "attachBaseContext() base = " + base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }
}
