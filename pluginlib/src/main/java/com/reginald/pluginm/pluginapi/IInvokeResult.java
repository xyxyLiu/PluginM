package com.reginald.pluginm.pluginapi;

/**
 * Created by lxy on 17-9-18.
 */

public interface IInvokeResult {
    int RESULT_OK = 1;

    int RESULT_NOT_FOUND = -1;
    int RESULT_REMOTE_ERROR = -2;

    int RESULT_INVOKE_INVALID = -10;
    int RESULT_INVOKE_ERROR = -11;

    int getResultCode();
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
