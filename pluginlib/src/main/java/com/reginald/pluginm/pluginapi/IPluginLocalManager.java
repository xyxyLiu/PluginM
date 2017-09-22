package com.reginald.pluginm.pluginapi;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;

/**
 * Created by lxy on 16-10-27.
 */
public interface IPluginLocalManager {
    Context getHostContext();
    String getPluginPackageName(Context context);
    PackageInfo getPluginPackageInfo(String packageName, int flags);

    IInvokeResult invokeHost(String serviceName, String methodName, String params, IInvokeCallback callback);
    IInvokeResult invokePlugin(String packageName, String serviceName, String methodName, String params, IInvokeCallback callback);
}
