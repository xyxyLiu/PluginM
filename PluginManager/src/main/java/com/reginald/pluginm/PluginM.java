package com.reginald.pluginm;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.reginald.pluginm.comm.PluginCommClient;
import com.reginald.pluginm.comm.invoker.InvokeCallback;
import com.reginald.pluginm.comm.invoker.InvokeCallbackWrapper;
import com.reginald.pluginm.comm.invoker.InvokeResult;
import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.stub.PluginContentResolver;

import java.util.List;

/**
 * Created by lxy on 17-8-23.
 */

public class PluginM {
    private static Context sAppContext;
    private static PluginConfigs sConfigs;

    public static void onAttachBaseContext(Application app, PluginConfigs configs) {
        if (app == null || configs == null) {
            throw new IllegalStateException("app and configs MUST be provided!");
        }
        sAppContext = app;
        sConfigs = configs;
        PluginManager.init(app);
    }

    public static PluginConfigs getConfigs() {
        return sConfigs;
    }

    public static PluginInfo install(String apkPath) {
        return PluginManager.getInstance(sAppContext).installPlugin(apkPath);
    }

    public static List<PluginInfo> getAllInstalledPlugins() {
        return PluginManager.getInstance(sAppContext).getAllInstalledPlugins();
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

    public IInvokeResult invoke(String packageName, String serviceName, String methodName, String params, IInvokeCallback callback) {
        InvokeCallback invokeCallback = InvokeCallbackWrapper.build(callback);

        final InvokeResult invokeResult = PluginCommClient.getInstance(sAppContext).invoke(packageName, serviceName, methodName, params, invokeCallback);

        return InvokeResult.newIInvokerResult(invokeResult);
    }

    public IBinder fetchService(String packageName, String serviceName) {
        return PluginCommClient.getInstance(sAppContext).fetchService(packageName, serviceName);
    }
}
