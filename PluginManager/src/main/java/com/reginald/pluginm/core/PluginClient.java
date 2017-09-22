package com.reginald.pluginm.core;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;

import com.reginald.pluginm.IPluginClient;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.comm.invoker.InvokeCallback;
import com.reginald.pluginm.comm.invoker.InvokeCallbackWrapper;
import com.reginald.pluginm.comm.invoker.InvokeResult;
import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.pluginapi.IPluginInvoker;
import com.reginald.pluginm.utils.ConfigUtils;
import com.reginald.pluginm.utils.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lxy on 17-9-20.
 */

public class PluginClient extends IPluginClient.Stub {

    private static final String TAG = "PluginClient";
    private static volatile PluginClient sInstance;

    private Context mContext;
    private Map<String, IPluginInvoker> mInvokerCacheMap = new HashMap<>();

    public static synchronized PluginClient getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginClient(hostContext);
        }

        return sInstance;
    }

    private PluginClient(Context hostContext) {
        Context appContext = hostContext.getApplicationContext();
        mContext = appContext != null ? appContext : hostContext;
    }

    @Override
    public List<PluginInfo> getAllLoadedPlugins() throws RemoteException {
        return PluginManager.getInstance(mContext).getLoadedPluginInfos();
    }

    @Override
    public InvokeResult invokePlugin(final String packageName, final String serviceName, final String methodName, String params, final InvokeCallback callback) throws RemoteException {
        IPluginInvoker pluginInvoker = fetchPluginInvoker(packageName, serviceName);

        if (pluginInvoker != null) {
            IInvokeCallback iInvokeCallback = InvokeCallbackWrapper.build(callback);

            PluginInfo pluginInfo = PluginManager.getInstance(mContext).getLoadedPluginInfo(packageName);
            if (pluginInfo != null) {
                IInvokeResult result = pluginInvoker.onInvoke(pluginInfo.baseContext, methodName, params, iInvokeCallback);
                return InvokeResult.build(result);
            }
        }

        Logger.w(TAG, String.format("invokePlugin() pkg = %s, service = %s, method = %s, params = %s, callback = %s, NOT found!",
                packageName, serviceName, methodName, params, callback));

        return new InvokeResult(IInvokeResult.RESULT_NOT_FOUND, null);
    }

    private IPluginInvoker fetchPluginInvoker(String packageName, String serviceName) {
        String key = keyForInvokerMap(packageName, serviceName);
        if (key == null) {
            Logger.w(TAG, String.format("fetchPluginInvoker() key is Null! for %s @ %s", serviceName, packageName));
            return null;
        }

        synchronized (mInvokerCacheMap) {
            IPluginInvoker pluginInvoker = mInvokerCacheMap.get(key);
            Logger.w(TAG, String.format("fetchPluginInvoker() cached pluginInvoker = %s", pluginInvoker));

            if (pluginInvoker != null) {
                return pluginInvoker;
            }

            PluginInfo pluginInfo = PluginManager.getInstance(mContext).loadPlugin(packageName);

            if (pluginInfo == null) {
                Logger.w(TAG, String.format("fetchPluginInvoker() pluginInfo is Null! for %s @ %s", serviceName, packageName));
                return null;
            }

            Logger.w(TAG, String.format("fetchPluginInvoker() pluginInfo = %s", pluginInfo));

            try {
                Map<String, String> serviceConfig = pluginInfo.pluginInvokerClassMap.get(serviceName);
                if (serviceConfig == null) {
                    Logger.w(TAG, String.format("fetchPluginInvoker() pluginInvokerClassName NOT config! for %s @ %s",
                            serviceName, packageName));
                    return null;
                }
                String className = serviceConfig.get(ConfigUtils.KEY_INVOKER_CLASS);
                Class<?> invokerClazz = pluginInfo.classLoader.loadClass(className);
                pluginInvoker = (IPluginInvoker) invokerClazz.newInstance();
                mInvokerCacheMap.put(key, pluginInvoker);
                return pluginInvoker;
            } catch (ClassNotFoundException e) {
                Logger.e(TAG, String.format("fetchPluginInvoker() pluginInvokerClass NOT found! for %s @ %s",
                        serviceName, packageName), e);
            } catch (Exception e) {
                Logger.e(TAG, String.format("fetchPluginInvoker() pluginInvoker init error! for %s @ %s",
                        serviceName, packageName), e);
            }
        }

        return null;
    }

    private static String keyForInvokerMap(String packageName, String serviceName) {
        if (!TextUtils.isEmpty(packageName)) {
            return String.format("%s@%s", !TextUtils.isEmpty(serviceName) ? serviceName : "", packageName);
        }

        return null;
    }
}
