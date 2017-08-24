// IPluginServiceStubBinder.aidl
package com.reginald.pluginm;

import android.content.Intent;
import com.reginald.pluginm.PluginInfo;
// Declare any non-default types here with import statements

interface IPluginManager {
    PluginInfo install(in String pluginPackageName);
    PluginInfo getInstalledPluginInfo(in String packageName);
    List<PluginInfo> getAllInstalledPlugins();
    Intent getPluginActivityIntent(in Intent originIntent);
    Intent getPluginServiceIntent(in Intent originIntent);
    Bundle getPluginProviderUri(in String auth);
    String selectStubProcessName(in String processName, in String pkgName);

    ActivityInfo resolveActivityInfo(in Intent intent, int flags);
    ServiceInfo resolveServiceInfo(in Intent intent, int flags);
    ProviderInfo resolveProviderInfo(in String name);
    ActivityInfo getActivityInfo(in ComponentName componentName, int flags);
    ServiceInfo getServiceInfo(in ComponentName componentName, int flags);
    ActivityInfo getReceiverInfo(in ComponentName componentName, int flags);
    ProviderInfo getProviderInfo(in ComponentName componentName, int flags);

}