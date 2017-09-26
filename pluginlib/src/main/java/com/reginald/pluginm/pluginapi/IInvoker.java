package com.reginald.pluginm.pluginapi;

import android.content.Context;
import android.os.IBinder;

import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;

/**
 * Created by lxy on 17-9-18.
 */

public interface IInvoker {
    IBinder onServiceCreate(Context context);
    IInvokeResult onInvoke(Context context, String methodName, String params, IInvokeCallback callback);
}
