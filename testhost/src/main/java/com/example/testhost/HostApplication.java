package com.example.testhost;

import android.app.Application;
import android.content.Context;

import com.reginald.pluginm.PluginM;
import com.reginald.pluginm.core.PluginManager;

/**
 * Created by lxy on 16-6-22.
 */
public class HostApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PluginM.onAttachBaseContext(this);
    }
}
