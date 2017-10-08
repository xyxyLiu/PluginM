package com.reginald.pluginm.pluginapi;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.text.TextUtils;

/**
 * Created by lxy on 16-10-27.
 */
public class PluginHelper {
    private static final String TAG = "PluginHelper";
    private static IPluginLocalManager sPluginLocalManager;
    private static String sHostPackageName;

    private static void init(Object iPluginLocalManager) {
        sPluginLocalManager = (IPluginLocalManager)iPluginLocalManager;
    }

    private PluginHelper() {
    }

    /**
     * 获取插件包名
     * @param context
     * @return 插件包名
     */
    public static String getPluginPackageName(Context context) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.getPluginPackageName(context);
        }

        //default
        return context.getPackageName();
    }

    /**
     * 获取宿主包名
     * @param context
     * @return 宿主包名
     */
    public static String getHostPackageName(Context context) {
        if (!TextUtils.isEmpty(sHostPackageName)) {
            return sHostPackageName;
        }

        sHostPackageName = getHostContext(context).getPackageName();

        return sHostPackageName;
    }

    /**
     * 获取宿主Context
     * @param context
     * @return 宿主Context
     */
    public static Context getHostContext(Context context) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.getHostContext();
        }

        return context;
    }

    /**
     * 获取插件package信息
     * @param context
     * @param packageName 插件包名
     * @param flags
     * @return
     */
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

    /**
     * IInvoker框架函数调用
     * @param packageName 插件或宿主包名
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @param params 方法参数
     * @param callback 方法回调 {@link IInvokeCallback}
     * @return 方法调用结果 {@link IInvokeResult}
     */
    public static IInvokeResult invoke(String packageName, String serviceName, String methodName, String params, IInvokeCallback callback) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.invoke(packageName, serviceName, methodName, params, callback);
        }

        return null;
    }

    /**
     * IInvoker框架Binder服务获取
     * @param packageName 插件或宿主包名
     * @param serviceName 服务名称
     * @return Binder服务
     */
    public static IBinder fetchService(String packageName, String serviceName) {
        if (sPluginLocalManager != null) {
            return sPluginLocalManager.fetchService(packageName, serviceName);
        }

        return null;
    }
}
