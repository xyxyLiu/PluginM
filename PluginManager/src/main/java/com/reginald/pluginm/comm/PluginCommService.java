package com.reginald.pluginm.comm;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.reginald.pluginm.IPluginClient;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.comm.invoker.HostInvokerManager;
import com.reginald.pluginm.comm.invoker.InvokeCallback;
import com.reginald.pluginm.comm.invoker.InvokeResult;
import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.core.PluginManagerService;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.utils.ConfigUtils;
import com.reginald.pluginm.utils.Logger;

import java.util.Map;

/**
 * Created by lxy on 17-8-22.
 */

public class PluginCommService extends IPluginComm.Stub {
    private static final String TAG = "PluginCommService";
    private static volatile PluginCommService sInstance;

    private Context mContext;
    private PluginManager mPluginManager;
    private HostInvokerManager mHostInvokerManager;

    public static synchronized PluginCommService getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginCommService(hostContext);
        }

        return sInstance;
    }

    private PluginCommService(Context hostContext) {
        Context appContext = hostContext.getApplicationContext();
        mContext = appContext != null ? appContext : hostContext;
        mPluginManager = PluginManager.getInstance(mContext);
        mHostInvokerManager = HostInvokerManager.getInstance(mContext);
    }

    @Override
    public InvokeResult invoke(String packageName, String serviceName, String methodName, String params, InvokeCallback callback) throws RemoteException {
        if (TextUtils.isEmpty(packageName)) {
            return invokeHost(serviceName, methodName, params, callback);
        } else {
            return invokePlugin(packageName, serviceName, methodName, params, callback);
        }
    }

    @Override
    public IBinder fetchService(String packageName, String serviceName) throws RemoteException {
        if (TextUtils.isEmpty(packageName)) {
            return mHostInvokerManager.fetchHostServiceBinder(serviceName);
        } else {
            return fetchPluginServiceBinder(packageName, serviceName);
        }
    }


    private InvokeResult invokeHost(String serviceName, String methodName, String params, InvokeCallback callback) throws RemoteException {
        return mHostInvokerManager.invokeHost(serviceName, methodName, params, callback);
    }

    private InvokeResult invokePlugin(String packageName, String serviceName, String methodName, String params, InvokeCallback callback) throws RemoteException {
        IPluginClient pluginClient = fetchPluginClient(packageName, serviceName);
        if (pluginClient != null) {
            return pluginClient.invokePlugin(packageName, serviceName, methodName, params, callback);
        }

        return InvokeResult.buildErrorResult(IInvokeResult.RESULT_NOT_FOUND);
    }


    // TODO cache binders
    private IBinder fetchPluginServiceBinder(String packageName, String serviceName) throws RemoteException {
        IPluginClient pluginClient = fetchPluginClient(packageName, serviceName);
        if (pluginClient != null) {
            return pluginClient.fetchPluginService(packageName, serviceName);
        }

        return null;
    }

    private IPluginClient fetchPluginClient(String packageName, String serviceName) {
        PluginInfo pluginInfo = mPluginManager.getInstalledPluginInfo(packageName);
        if (pluginInfo == null) {
            Logger.e(TAG, String.format("fetchPluginClient() plugin  %s not installed!", packageName));
            return null;
        }

        Map<String, String> serviceConfig = pluginInfo.pluginInvokerClassMap.get(serviceName);

        if (serviceConfig == null) {
            Logger.e(TAG, String.format("fetchPluginClient() service config for %s@%s not found!", serviceName, packageName));
            return null;
        }

        String targetProcessName = serviceConfig.get(ConfigUtils.KEY_INVOKER_PROCESS);
        StubManager.ProcessInfo processInfo = StubManager.getInstance(mContext).selectStubProcess(targetProcessName, packageName);
        Logger.d(TAG, String.format("fetchPluginClient() packageName = %s, serviceName = %s -> processInfo = %s",
                packageName, serviceName, processInfo));

        if (processInfo != null) {
            return PluginManagerService.getInstance(mContext).fetchPluginClient(processInfo.processName, true);
        }

        return null;
    }


}
