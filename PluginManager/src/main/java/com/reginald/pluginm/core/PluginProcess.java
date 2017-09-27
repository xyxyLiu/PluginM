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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by lxy on 17-9-27.
 */

public class PluginProcess {
    private static final String TAG = "PluginProcess";

    private final Map<String, ApplicationInfo> mRunningPluginMap = new HashMap<>();

    // stub -> target
    private final Map<String, List<ActivityInfo>> mRunningActivityMap = new HashMap<>();
    private final Map<String, List<ServiceInfo>> mRunningServiceMap = new HashMap<>();
    private final Map<String, List<ProviderInfo>> mRunningProviderMap = new HashMap<>();

    private StubManager.ProcessInfo mStubProcess;

    public PluginProcess(StubManager.ProcessInfo stubProcess) {
        mStubProcess = stubProcess;
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
                Iterator<ActivityInfo> iterator = activityInfos.iterator();
                while (iterator.hasNext()) {
                    ActivityInfo info = iterator.next();
                    if (CommonUtils.isComponentInfoMatch(targetInfo, info)) {
                        iterator.remove();
                    }
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
    }

    public void onServiceDestory(ServiceInfo stubInfo, ServiceInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onServiceDestory() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
    }

    public void onProviderCreated(ProviderInfo stubInfo, ProviderInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onProviderCreated() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
    }

}
