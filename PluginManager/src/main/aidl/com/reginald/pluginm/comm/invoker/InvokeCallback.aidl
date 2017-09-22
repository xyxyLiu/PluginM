// IInvokeCallback.aidl
package com.reginald.pluginm.comm.invoker;

import com.reginald.pluginm.comm.invoker.InvokeResult;
// Declare any non-default types here with import statements

interface InvokeCallback {
    InvokeResult onCallback(String params);
}
