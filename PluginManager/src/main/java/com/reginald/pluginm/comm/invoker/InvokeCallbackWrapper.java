package com.reginald.pluginm.comm.invoker;

import android.os.RemoteException;

import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.utils.Logger;

/**
 * Created by lxy on 17-9-21.
 */

public abstract class InvokeCallbackWrapper extends InvokeCallback.Stub {
    private static final String TAG = "InvokeCallbackStub";

    public static InvokeCallbackWrapper build(final IInvokeCallback iInvokeCallback) {
        if (iInvokeCallback == null) {
            return null;
        }

        return new InvokeCallbackWrapper() {
            @Override
            public InvokeResult onCallback(String params) {
                IInvokeResult iInvokeResult = iInvokeCallback.onCallback(params);
                return InvokeResult.build(iInvokeResult);
            }
        };
    }

    public static IInvokeCallback build(final InvokeCallback callback) {
        return callback != null ? new IInvokeCallback() {
            @Override
            public IInvokeResult onCallback(String params) {
                final InvokeResult invokeResult;
                try {
                    invokeResult = callback.onCallback(params);
                } catch (RemoteException e) {
                    Logger.e(TAG, "build IInvokeCallback. remote died!");
                    return new IInvokeResult() {
                        @Override
                        public int getResultCode() {
                            return IInvokeResult.RESULT_REMOTE_ERROR;
                        }

                        @Override
                        public String getResult() {
                            return null;
                        }
                    };
                }

                return InvokeResult.newIInvokerResult(invokeResult);
            }
        } : null;
    }
}
