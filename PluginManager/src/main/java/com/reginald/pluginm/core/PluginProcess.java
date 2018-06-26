package com.reginald.pluginm.core;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;

import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.utils.CommonUtils;
import com.reginald.pluginm.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lxy on 17-9-27.
 */

public class PluginProcess {
    private static final String TAG = "PluginProcess";

    // plugin pkg -> appInfo
    private final Map<String, ApplicationInfo> mRunningPluginMap = new HashMap<>();

    // stub -> target
    private final Map<String, List<ActivityInfo>> mRunningActivityMap = new HashMap<>();
    private final Map<String, List<ServiceInfo>> mRunningServiceMap = new HashMap<>();
    private final Map<String, List<ProviderInfo>> mRunningProviderMap = new HashMap<>();

    private final int mPid;
    private final StubManager.ProcessInfo mStubProcess;

    public PluginProcess(int pid, StubManager.ProcessInfo stubProcess) {
        mPid = pid;
        mStubProcess = stubProcess;
    }

    public int getPid() {
        return mPid;
    }

    public String getProcessName() {
        return mStubProcess.processName;
    }

    public String getPluginProcessName() {
        return StubManager.getInstance(PluginManager.getInstance().getHostContext()).getPluginProcessName
                (getProcessName());
    }

    public List<String> getRunningPlugins() {
        return new ArrayList<>(mRunningPluginMap.keySet());
    }

    public void onApplicationAttached(ApplicationInfo targetInfo) {
        Logger.d(TAG, String.format("onApplicationAttached() processName = %s, targetInfo = %s",
                mStubProcess.processName, targetInfo));

        synchronized (mRunningPluginMap) {
            mRunningPluginMap.put(targetInfo.packageName, targetInfo);
        }
    }

    public void onActivityCreated(ActivityInfo stubInfo, ActivityInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onActivityCreated() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        synchronized (mRunningActivityMap) {
            List<ActivityInfo> activityInfos = mRunningActivityMap.get(stubInfo.name);
            if (activityInfos == null) {
                activityInfos = new ArrayList<>();
                activityInfos.add(targetInfo);
                mRunningActivityMap.put(stubInfo.name, activityInfos);
            } else {
                activityInfos.add(targetInfo);
            }
        }
    }

    public void onActivityDestory(ActivityInfo stubInfo, ActivityInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onActivityDestory() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        synchronized (mRunningActivityMap) {
            List<ActivityInfo> activityInfos = mRunningActivityMap.get(stubInfo.name);
            if (activityInfos != null) {
                boolean isRemoved = CommonUtils.removeComponent(activityInfos, targetInfo);
                if (!isRemoved) {
                    throw new IllegalStateException("duplicate running activity found for " +
                            targetInfo + " in " + activityInfos);
                }
            }
        }
    }

    public boolean canUseActivity(ActivityInfo stubInfo, ActivityInfo targetInfo) {
        synchronized (mRunningActivityMap) {
            List<ActivityInfo> usedActivities = mRunningActivityMap.get(stubInfo.name);
            if (usedActivities != null && !usedActivities.isEmpty()) {
                for (ActivityInfo usedActivity : usedActivities) {
                    if (CommonUtils.isComponentInfoMatch(usedActivity, targetInfo)) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
    }

    public void onServiceCreated(ServiceInfo stubInfo, ServiceInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onServiceCreated() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        synchronized (mRunningServiceMap) {
            List<ServiceInfo> serviceInfos = mRunningServiceMap.get(stubInfo.name);
            if (serviceInfos == null) {
                serviceInfos = new ArrayList<>();
                serviceInfos.add(targetInfo);
                mRunningServiceMap.put(stubInfo.name, serviceInfos);
                return;
            }

            if (CommonUtils.containsComponent(serviceInfos, targetInfo)) {
                throw new IllegalStateException("duplicate running service found for " +
                        targetInfo + " in " + serviceInfos);
            }

            serviceInfos.add(targetInfo);
        }
    }

    public void onServiceDestory(ServiceInfo stubInfo, ServiceInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onServiceDestory() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        synchronized (mRunningServiceMap) {
            List<ServiceInfo> serviceInfos = mRunningServiceMap.get(stubInfo.name);
            if (serviceInfos != null) {
                boolean isRemoved = CommonUtils.removeComponent(serviceInfos, targetInfo);
                if (!isRemoved) {
                    throw new IllegalStateException("duplicate running service found for " + targetInfo);
                }
            }
        }
    }

    public void onProviderCreated(ProviderInfo stubInfo, ProviderInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onProviderCreated() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        synchronized (mRunningProviderMap) {
            List<ProviderInfo> providerInfos = mRunningProviderMap.get(stubInfo.name);
            if (providerInfos == null) {
                providerInfos = new ArrayList<>();
                providerInfos.add(targetInfo);
                mRunningProviderMap.put(stubInfo.name, providerInfos);
                return;
            }

            if (CommonUtils.containsComponent(providerInfos, targetInfo)) {
                throw new IllegalStateException("duplicate provider running! " + targetInfo);
            }

            providerInfos.add(targetInfo);
        }
    }

    @Override
    public String toString() {
        return String.format("PluginProcess[ processName = %s, mRunningPluginMap = %s, " +
                        "mRunningActivityMap = %s, mRunningServiceMap = %s, mRunningProviderMap = %s]",
                mStubProcess.processName, mRunningPluginMap, mRunningActivityMap,
                mRunningServiceMap, mRunningProviderMap);
    }
}
