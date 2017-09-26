package com.example.testplugin;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.reginald.pluginm.demo.pluginsharelib.ITestPluginBinder;
import com.reginald.pluginm.demo.pluginsharelib.PluginItem;
import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.pluginapi.IInvoker;

/**
 * Created by lxy on 17-9-20.
 */

public class MyPluginInvoker implements IInvoker {
    private static final String TAG = "MyPluginInvoker";

    private static final String METHOD_START_MAIN = "start_main";

    private IBinder myBinder = new ITestPluginBinder.Stub() {
        @Override
        public String basicTypes(PluginItem pluginItem) throws RemoteException {
            Log.d(TAG, "BINDER basicTypes() pluginItem = " + pluginItem);
            return "I'm a binder from plugintest!";
        }
    };

    @Override
    public IBinder onServiceCreate(Context context) {
        return myBinder;
    }

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
