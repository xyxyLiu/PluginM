package com.reginald.pluginm.pluginapi;

/**
 * Created by lxy on 17-9-18.
 */

public interface IInvokeCallback {
    /**
     * 函数回调
     * @param params 回调参数
     * @return 回调结果 {@link com.reginald.pluginm.pluginapi.IInvokeResult}
     */
    IInvokeResult onCallback(String params);
}
