package com.reginald.pluginm;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
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
    private static PluginManager sPluginManager;

    public static void onAttachBaseContext(Application app, PluginConfigs configs) {
        if (app == null || configs == null) {
            throw new IllegalStateException("app and configs MUST be provided!");
        }
        sAppContext = app;
        sConfigs = configs;
        sPluginManager = PluginManager.getInstance(sAppContext);
        sPluginManager.onAttachBaseContext(app);
    }

    public static PluginConfigs getConfigs() {
        return new PluginConfigs(sConfigs);
    }

    public static PluginInfo install(String apkPath) {
        return sPluginManager.installPlugin(apkPath);
    }

    public static PluginInfo getInstalledPlugin(String packageName) {
        return sPluginManager.getInstalledPluginInfo(packageName);
    }

    public static List<PluginInfo> getAllInstalledPlugins() {
        return sPluginManager.getAllInstalledPlugins();
    }

    public static Intent getPluginActivityIntent(Intent pluginIntent) {
        return sPluginManager.getPluginActivityIntent(pluginIntent);
    }

    public static void startActivity(Context context, Intent intent) {
        sPluginManager.startActivity(context, intent);
    }

    public static void startActivity(Context context, Intent intent, Bundle options) {
        sPluginManager.startActivity(context, intent, options);
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode) {
        sPluginManager.startActivityForResult(activity, intent, requestCode);
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode, Bundle options) {
        sPluginManager.startActivityForResult(activity, intent, requestCode, options);
    }

    public static ComponentName startService(Context context, Intent intent) {
        return sPluginManager.startService(context, intent);
    }

    public static boolean stopService(Context context, Intent intent) {
        return sPluginManager.stopService(context, intent);
    }

    public static boolean bindService(Context context, Intent intent, ServiceConnection conn,
            int flags) {
        return sPluginManager.bindService(context, intent, conn, flags);
    }

    public static void unbindService(Context context, ServiceConnection conn) {
        sPluginManager.unbindService(context, conn);
    }

    public static ContentResolver getContentResolver(Context context) {
        return new PluginContentResolver(context, context.getContentResolver());
    }

    public static IInvokeResult invoke(String packageName, String serviceName, String methodName, String params, IInvokeCallback callback) {
        InvokeCallback invokeCallback = InvokeCallbackWrapper.build(callback);

        final InvokeResult invokeResult = PluginCommClient.getInstance(sAppContext).invoke(packageName, serviceName, methodName, params, invokeCallback);

        return InvokeResult.newIInvokerResult(invokeResult);
    }

    public static IBinder fetchService(String packageName, String serviceName) {
        return PluginCommClient.getInstance(sAppContext).fetchService(packageName, serviceName);
    }
}
