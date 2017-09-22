package com.example.testplugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.pluginapi.IPluginInvoker;

/**
 * Created by lxy on 17-9-20.
 */

public class MyPluginInvoker implements IPluginInvoker {
    private static final String TAG = "MyPluginInvoker";

    private static final String METHOD_START_MAIN = "start_main";

    @Override
    public IInvokeResult onInvoke(Context context, String methodName, String params, IInvokeCallback callback) {
        Log.d(TAG, String.format("onInvoke methodName = %s, params = %s, callback = %s",
                methodName, params, callback));

        if (callback != null) {
            callback.onCallback("start oncallback!!");
        }

        if (METHOD_START_MAIN.equals(methodName)) {
            Intent intent = new Intent(context, PluginMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return IInvokeResult.INVOKERESULT_VOID_OK;
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
