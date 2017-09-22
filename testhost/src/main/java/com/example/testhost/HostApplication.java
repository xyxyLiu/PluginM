package com.example.testhost;

import android.app.Application;
import android.content.Context;

import com.reginald.pluginm.PluginConfigs;
import com.reginald.pluginm.PluginM;

/**
 * Created by lxy on 16-6-22.
 */
public class HostApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PluginM.onAttachBaseContext(this, new PluginConfigs().setProcessType(PluginConfigs.PROCESS_TYPE_DUAL));
    }
}
