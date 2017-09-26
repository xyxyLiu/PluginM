package com.example.testhost;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.reginald.pluginm.demo.pluginsharelib.ITestServiceBinder;
import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.pluginapi.IInvoker;

/**
 * Created by lxy on 17-9-21.
 */

public class MyHostInvoker implements IInvoker {

    private static final String TAG = "MyHostInvoker";

    private static final String METHOD_START_MAIN = "start_host_main";

    @Override
    public IBinder onServiceCreate(final Context context) {
        Log.d(TAG, "onServiceCreate()");
        return new ITestServiceBinder.Stub() {
            @Override
            public void test(String url) throws RemoteException {
                Log.d(TAG, "BINDER test() url = " + url);
            }
        };
    }

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
