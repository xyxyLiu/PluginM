// IPluginServiceStubBinder.aidl
package com.reginald.pluginm;

import android.content.Intent;
import com.reginald.pluginm.PluginInfo;
// Declare any non-default types here with import statements

interface IPluginManager {
    PluginInfo install(in String pluginPackageName, boolean isInternal, boolean loadDex);
    PluginInfo uninstall(in String pluginPackageName);

    PluginInfo getInstalledPluginInfo(in String packageName);
    List<PluginInfo> getAllInstalledPlugins();
    List<PluginInfo> getAllRunningPlugins();
    boolean isPluginRunning(String pkgName);

    Intent getPluginActivityIntent(in Intent originIntent);
    Intent getPluginServiceIntent(in Intent originIntent);
    Bundle getPluginProviderUri(in String auth);
    String selectStubProcessName(in String processName, in String pkgName);

    ActivityInfo resolveActivityInfo(in Intent intent, int flags);
    ServiceInfo resolveServiceInfo(in Intent intent, int flags);
    ProviderInfo resolveProviderInfo(in String name);

    List<ResolveInfo> queryIntentActivities(in Intent intent, int flags);
    List<ResolveInfo> queryIntentServices(in Intent intent, int flags);
    List<ResolveInfo> queryBroadcastReceivers(in Intent intent, int flags);
    List<ResolveInfo> queryIntentContentProviders(in Intent intent, int flags);

    ActivityInfo getActivityInfo(in ComponentName componentName, int flags);
    ServiceInfo getServiceInfo(in ComponentName componentName, int flags);
    ActivityInfo getReceiverInfo(in ComponentName componentName, int flags);
    ProviderInfo getProviderInfo(in ComponentName componentName, int flags);
    PackageInfo getPackageInfo(in String packageName, int flags);

    void onPluginProcessAttached(in IBinder client);
    void onApplicationAttached(in ApplicationInfo targetInfo, String processName);
    void onActivityCreated(in ActivityInfo stubInfo,in ActivityInfo targetInfo);
    void onActivityDestory(in ActivityInfo stubInfo,in ActivityInfo targetInfo);
    void onServiceCreated(in ServiceInfo stubInfo,in ServiceInfo targetInfo);
    void onServiceDestory(in ServiceInfo stubInfo,in ServiceInfo targetInfo);
    void onProviderCreated(in ProviderInfo stubInfo,in ProviderInfo targetInfo);
}