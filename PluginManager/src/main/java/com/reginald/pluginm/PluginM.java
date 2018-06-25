package com.reginald.pluginm;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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
    private static Context sContext;
    private static PluginConfigs sConfigs;
    private static PluginManager sPluginManager;

    public static void onAttachBaseContext(Application app, PluginConfigs configs) {
        if (app == null || configs == null) {
            throw new IllegalStateException("app and configs MUST be provided!");
        }
        sContext = app;
        sConfigs = configs;
        PluginManager.onAttachBaseContext(app);
    }

    public static PluginConfigs getConfigs() {
        return new PluginConfigs(sConfigs);
    }

    public static PluginInfo install(String apkPath) {
        return install(apkPath, true);
    }

    public static PluginInfo install(String apkPath, boolean loadDex) {
        return PluginManager.getInstance().installPlugin(apkPath, loadDex);
    }

    public static PluginInfo uninstall(String packageName) {
        return PluginManager.getInstance().uninstallPlugin(packageName);
    }

    public static PluginInfo getInstalledPlugin(String packageName) {
        return PluginManager.getInstance().getInstalledPluginInfo(packageName);
    }

    public static List<PluginInfo> getAllInstalledPlugins() {
        return PluginManager.getInstance().getAllInstalledPlugins();
    }

    public static List<PluginInfo> getAllRunningPlugins() {
        return PluginManager.getInstance().getAllRunningPlugins();
    }

    public static boolean isPluginRunning(String pkgName) {
        return PluginManager.getInstance().isPluginRunning(pkgName);
    }

    public static PackageManager getPluginPackageManager(Context context) {
        return PluginManager.getInstance().getPluginPackageManager();
    }

    public static void startActivity(Context context, Intent intent) {
        PluginManager.getInstance().startActivity(context, intent);
    }

    public static void startActivity(Context context, Intent intent, Bundle options) {
        PluginManager.getInstance().startActivity(context, intent, options);
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode) {
        PluginManager.getInstance().startActivityForResult(activity, intent, requestCode);
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode, Bundle options) {
        PluginManager.getInstance().startActivityForResult(activity, intent, requestCode, options);
    }

    public static ComponentName startService(Context context, Intent intent) {
        return PluginManager.getInstance().startService(context, intent);
    }

    public static boolean stopService(Context context, Intent intent) {
        return PluginManager.getInstance().stopService(context, intent);
    }

    public static boolean bindService(Context context, Intent intent, ServiceConnection conn,
            int flags) {
        return PluginManager.getInstance().bindService(context, intent, conn, flags);
    }

    public static void unbindService(Context context, ServiceConnection conn) {
        PluginManager.getInstance().unbindService(context, conn);
    }

    public static ContentResolver getContentResolver(Context context) {
        return new PluginContentResolver(context, context.getContentResolver());
    }

    public static IInvokeResult invoke(String packageName, String serviceName, String methodName, String params, IInvokeCallback callback) {
        InvokeCallback invokeCallback = InvokeCallbackWrapper.build(callback);

        final InvokeResult invokeResult = PluginCommClient.getInstance(sContext).invoke(packageName, serviceName, methodName, params, invokeCallback);

        return InvokeResult.newIInvokerResult(invokeResult);
    }

    public static IBinder fetchService(String packageName, String serviceName) {
        return PluginCommClient.getInstance(sContext).fetchService(packageName, serviceName);
    }
}
