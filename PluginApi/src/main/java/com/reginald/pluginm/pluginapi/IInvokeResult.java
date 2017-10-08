package com.reginald.pluginm.pluginapi;

/**
 * Created by lxy on 17-9-18.
 */

public interface IInvokeResult {
    /**
     * 函数调用结果成功
     */
    int RESULT_OK = 1;
    /**
     * 函数调用结果失败，未找到目标IInvoker
     */
    int RESULT_NOT_FOUND = -1;
    /**
     * 函数调用结果失败，远端服务进程死亡
     */
    int RESULT_REMOTE_ERROR = -2;
    /**
     * 函数调用结果失败，非法或不支持的参数
     */
    int RESULT_INVOKE_INVALID = -10;
    /**
     * 函数调用结果失败，内部错误
     */
    int RESULT_INVOKE_ERROR = -11;

    /**
     * 获取调用返回码
     * @return 返回码
     */
    int getResultCode();

    /**
     * 获取调用结果，当{@link IInvokeResult#getResultCode()} 为 {@link IInvokeResult#RESULT_OK} 时才有意义。
     * @return 调用结果
     */
    String getResult();

    IInvokeResult INVOKERESULT_VOID_OK = new IInvokeResult() {
        @Override
        public int getResultCode() {
            return RESULT_OK;
        }

        @Override
        public String getResult() {
            return null;
        }
    };
}
