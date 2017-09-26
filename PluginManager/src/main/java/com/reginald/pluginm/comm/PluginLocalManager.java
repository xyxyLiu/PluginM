package com.reginald.pluginm.comm;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.IBinder;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.comm.invoker.InvokeCallback;
import com.reginald.pluginm.comm.invoker.InvokeCallbackWrapper;
import com.reginald.pluginm.comm.invoker.InvokeResult;
import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.pluginapi.IPluginLocalManager;

/**
 * Created by lxy on 17-9-20.
 */

public class PluginLocalManager implements IPluginLocalManager {

    private static final String TAG = "PluginLocalManager";
    private static volatile PluginLocalManager sInstance;

    private Context mContext;

    public static synchronized PluginLocalManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginLocalManager(hostContext);
        }

        return sInstance;
    }

    private PluginLocalManager(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public Context getHostContext() {
        return mContext;
    }

    @Override
    public String getPluginPackageName(Context context) {
        PluginInfo pluginInfo = getPluginInfo(context);
        if (pluginInfo != null) {
            return pluginInfo.packageName;
        }
        return null;
    }

    @Override
    public PackageInfo getPluginPackageInfo(String packageName, int flags) {
        return PluginManager.getInstance(mContext).getPackageInfo(packageName, flags);
    }

    @Override
    public IInvokeResult invoke(String packageName, String serviceName, String methodName, String params, IInvokeCallback callback) {
        InvokeCallback invokeCallback = InvokeCallbackWrapper.build(callback);

        final InvokeResult invokeResult = PluginCommClient.getInstance(mContext).invoke(packageName, serviceName, methodName, params, invokeCallback);

        return InvokeResult.newIInvokerResult(invokeResult);
    }

    @Override
    public IBinder fetchService(String packageName, String serviceName) {
        return PluginCommClient.getInstance(mContext).fetchService(packageName, serviceName);
    }

    private PluginInfo getPluginInfo(Context pluginContext) {
        if (pluginContext == null) {
            return null;
        }
        return PluginManager.getInstance(mContext).getPluginInfoByClassLoader(pluginContext.getClassLoader());
    }
}
