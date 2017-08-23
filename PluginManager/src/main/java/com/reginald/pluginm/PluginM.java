package com.reginald.pluginm;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.stub.PluginContentResolver;

/**
 * Created by lxy on 17-8-23.
 */

public class PluginM {
    private static Context sAppContext;

    public static void onAttachBaseContext(Application app) {
        sAppContext = app;
        PluginManager.init(app);
    }

    public static PluginInfo install(String apkPath) {
        return PluginManager.getInstance(sAppContext).installPlugin(apkPath);
    }

    public static Intent getPluginActivityIntent(Intent pluginIntent) {
        return PluginManager.getInstance(sAppContext).getPluginActivityIntent(pluginIntent);
    }

    public static Intent getPluginServiceIntent(Intent pluginIntent) {
        return PluginManager.getInstance(sAppContext).getPluginServiceIntent(pluginIntent);
    }

    public static ContentResolver getPluginContentResolver() {
        return new PluginContentResolver(sAppContext, sAppContext.getContentResolver());
    }
}
