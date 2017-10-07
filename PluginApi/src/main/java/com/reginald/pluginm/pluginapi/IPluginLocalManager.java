package com.reginald.pluginm.pluginapi;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.IBinder;

/**
 * Created by lxy on 16-10-27.
 */
public interface IPluginLocalManager {
    Context getHostContext();
    String getPluginPackageName(Context context);
    PackageInfo getPluginPackageInfo(String packageName, int flags);

    IInvokeResult invoke(String packageName, String serviceName, String methodName, String params, IInvokeCallback callback);
    IBinder fetchService(String packageName, String serviceName);

}
