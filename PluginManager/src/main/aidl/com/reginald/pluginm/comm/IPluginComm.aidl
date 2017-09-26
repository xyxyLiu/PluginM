// IPluginComm.aidl
package com.reginald.pluginm.comm;

import com.reginald.pluginm.comm.invoker.InvokeResult;
import com.reginald.pluginm.comm.invoker.InvokeCallback;

interface IPluginComm {
    InvokeResult invoke(String packageName, String serviceName, String methodName, String params, InvokeCallback callback);
    IBinder fetchService(String packageName, String serviceName);
}
