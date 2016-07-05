package com.example.testplugin;

import android.app.Application;
import android.util.Log;

/**
 * Created by lxy on 16-6-28.
 */
public class PluginApplication extends Application {

    private static final String TAG = "PluginApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }
}
