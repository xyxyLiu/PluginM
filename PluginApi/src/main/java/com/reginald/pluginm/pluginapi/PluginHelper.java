package com.reginald.pluginm.pluginapi;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;

/**
 * Created by lxy on 16-10-27.
 */
public class PluginHelper {
    private static final String TAG = "PluginHelper";
    private static IPluginLocalManager sPluginLocalManager;

    private static void init(Object iPluginLocalManager) {
        sPluginLocalManager = (IPluginLocalManager)iPluginLocalManager;
    }

    private PluginHelper() {
    }

    public static String getPluginPackageName(Context context) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.getPluginPackageName(context);
        }

        //default
        return context.getPackageName();
    }

    public static Context getHostContext(Context context) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.getHostContext();
        }

        return context;
    }

    public static PackageInfo getPluginPackageInfo(Context context, String packageName, int flags) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.getPluginPackageInfo(packageName, flags);
        }

        try {
            return context.getPackageManager().getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }

        return null;
    }

    public static IInvokeResult invokeHost(String serviceName, String methodName, String params, IInvokeCallback callback) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.invoke(null, serviceName, methodName, params, callback);
        }

        return null;
    }

    public static IInvokeResult invokePlugin(String packageName, String serviceName, String methodName, String params, IInvokeCallback callback) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.invoke(packageName, serviceName, methodName, params, callback);
        }

        return null;
    }

    public static IBinder fetchService(String packageName, String serviceName) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.fetchService(packageName, serviceName);
        }

        return null;
    }
}
