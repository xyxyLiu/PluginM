package com.example.testhost;

import android.app.Application;
import android.content.Context;

import com.reginald.pluginm.PluginManagerNative;

/**
 * Created by lxy on 16-6-22.
 */
public class HostApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PluginManagerNative.getInstance(this).init(this, PluginConfigsUtil.getPluginConfigs());
    }
}
