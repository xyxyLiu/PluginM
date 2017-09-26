// IPlugin.aidl
package com.reginald.pluginm;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.comm.invoker.InvokeResult;
import com.reginald.pluginm.comm.invoker.InvokeCallback;

interface IPluginClient {
    List<PluginInfo> getAllLoadedPlugins();

    InvokeResult invokePlugin(String packageName, String serviceName, String methodName, String params, InvokeCallback callback);

    IBinder fetchPluginService(String packageName, String serviceName);
}
