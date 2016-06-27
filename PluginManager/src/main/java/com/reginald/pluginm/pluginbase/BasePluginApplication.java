package com.reginald.pluginm.pluginbase;

import android.app.Application;
import android.util.Log;

/**
 * Created by lxy on 16-6-28.
 */
public class BasePluginApplication extends Application {
    private static final String TAG = "BasePluginApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }

}
