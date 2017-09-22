package com.reginald.pluginm.comm;

import android.content.Context;
import android.content.pm.PackageInfo;

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
    public IInvokeResult invokeHost(String serviceName, String methodName, String params, IInvokeCallback callback) {
        InvokeCallback invokeCallback = InvokeCallbackWrapper.build(callback);

        final InvokeResult invokeResult = PluginCommClient.getInstance(mContext).invokeHost(serviceName, methodName, params, invokeCallback);

        return InvokeResult.newIInvokerResult(invokeResult);
    }

    @Override
    public IInvokeResult invokePlugin(final String packageName, final String serviceName, final String methodName, String params, final IInvokeCallback callback) {
        InvokeCallback invokeCallback = InvokeCallbackWrapper.build(callback);

        final InvokeResult invokeResult = PluginCommClient.getInstance(mContext).invokePlugin(packageName, serviceName, methodName, params, invokeCallback);

        return InvokeResult.newIInvokerResult(invokeResult);
    }

    private PluginInfo getPluginInfo(Context pluginContext) {
        if (pluginContext == null) {
            return null;
        }
        return PluginManager.getInstance(mContext).getPluginInfoByClassLoader(pluginContext.getClassLoader());
    }
}
