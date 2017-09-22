package com.reginald.pluginm.comm.invoker;

import android.content.Context;

import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;

/**
 * Created by lxy on 17-9-21.
 */

public interface IHostInvoker {
    IInvokeResult onInvoke(Context context, String methodName, String params, IInvokeCallback callback);
}
