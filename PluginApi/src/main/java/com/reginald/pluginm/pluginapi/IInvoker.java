package com.reginald.pluginm.pluginapi;

import android.content.Context;
import android.os.IBinder;

/**
 * Created by lxy on 17-9-18.
 */

public interface IInvoker {
    /**
     * 提供此IInvoker对外提供的Binder服务
     * @param context
     * @return Binder服务
     */
    IBinder onServiceCreate(Context context);

    /**
     * 处理此IInvoker上的函数调用
     * @param context
     * @param methodName 函数名称
     * @param params 函数参数(建议使用json等结构化数据格式)
     * @param callback 回调 {@link IInvokeCallback}
     * @return 结果 {@link IInvokeResult} 不要返回null。
     */
    IInvokeResult onInvoke(Context context, String methodName, String params, IInvokeCallback callback);
}
