package com.reginald.pluginm.comm;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.reginald.pluginm.comm.invoker.InvokeCallback;
import com.reginald.pluginm.comm.invoker.InvokeResult;
import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.core.PluginManagerServiceProvider;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.utils.BinderParcelable;
import com.reginald.pluginm.utils.Logger;

/**
 * Created by lxy on 17-9-20.
 */

public class PluginCommClient {
    private static final String TAG = "PluginCommClient";
    private static volatile PluginCommClient sInstance;

    private Context mContext;
    private volatile IPluginComm mService;

    public static synchronized PluginCommClient getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginCommClient(hostContext);
        }

        return sInstance;
    }

    private PluginCommClient(Context hostContext) {
        Context appContext = hostContext.getApplicationContext();
        mContext = appContext != null ? appContext : hostContext;
        initCommService();
    }

    private void initCommService() {
        try {
            final ContentResolver contentResolver = mContext.getContentResolver();
            final Bundle bundle = contentResolver.call(PluginManagerServiceProvider.getUri(mContext),
                    PluginManagerServiceProvider.METHOD_GET_COMM_SERVICE, null, null);
            if (bundle != null) {
                bundle.setClassLoader(PluginManager.class.getClassLoader());
                BinderParcelable bp = bundle.getParcelable(PluginManagerServiceProvider.KEY_SERVICE);
                if (bp != null) {
                    IBinder iBinder = bp.iBinder;
                    if (iBinder != null) {
                        iBinder.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                initCommService();
                            }
                        }, 0);
                    }
                    mService = IPluginComm.Stub.asInterface(iBinder);
                    Logger.d(TAG, "initCommService() success! mService = " + mService);
                }
            }
        } catch (Throwable e) {
            Logger.e(TAG, "initCommService() error!", e);
        }
    }

    public InvokeResult invoke(String packageName, String serviceName,
            String methodName, String params, InvokeCallback callback) {
        if (mService != null) {
            try {
                return mService.invoke(packageName, serviceName, methodName, params, callback);
            } catch (RemoteException e) {
                Logger.e(TAG, "invokePlugin() error!", e);
            }
        }
        return InvokeResult.buildErrorResult(IInvokeResult.RESULT_REMOTE_ERROR);
    }

    public IBinder fetchService(String packageName, String serviceName) {
        if (mService != null) {
            try {
                return mService.fetchService(packageName, serviceName);
            } catch (RemoteException e) {
                Logger.e(TAG, "fetchService() error!", e);
            }
        }
        return null;
    }
}
