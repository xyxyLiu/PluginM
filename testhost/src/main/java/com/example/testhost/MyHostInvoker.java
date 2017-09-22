package com.example.testhost;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.reginald.pluginm.comm.invoker.IHostInvoker;
import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;

/**
 * Created by lxy on 17-9-21.
 */

public class MyHostInvoker implements IHostInvoker {

    private static final String TAG = "MyHostInvoker";

    private static final String METHOD_START_MAIN = "start_host_main";

    @Override
    public IInvokeResult onInvoke(Context context, String methodName, String params, IInvokeCallback callback) {
        Log.d(TAG, String.format("onInvoke methodName = %s, params = %s, callback = %s",
                methodName, params, callback));

        if (METHOD_START_MAIN.equals(methodName)) {
            Intent intent = new Intent(context, DemoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return new IInvokeResult() {
                @Override
                public int getResultCode() {
                    return RESULT_OK;
                }

                @Override
                public String getResult() {
                    return "{'activity_name':'" + DemoActivity.class.getName() + "'}";
                }
            };
        }

        return new IInvokeResult() {
            @Override
            public int getResultCode() {
                return IInvokeResult.RESULT_INVOKE_INVALID;
            }

            @Override
            public String getResult() {
                return null;
            }
        };
    }
}
