// IPluginComm.aidl
package com.reginald.pluginm.comm;

import com.reginald.pluginm.comm.invoker.InvokeResult;
import com.reginald.pluginm.comm.invoker.InvokeCallback;

interface IPluginComm {
    InvokeResult invokeHost(String serviceName, String methodName, String params, InvokeCallback callback);
    InvokeResult invokePlugin(String packageName, String serviceName, String methodName, String params, InvokeCallback callback);
}
