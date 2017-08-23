package com.reginald.pluginm.stub;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;

import com.reginald.pluginm.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by lxy on 16-10-31.
 */
public class StubManager {

    private static StubManager sInstance;
    private static final String TAG = "StubManager";
    private static final String CATEGORY_ACTIVITY_PROXY_STUB = "com.reginald.pluginm.category.STUB";
    private Context mContext;

    private final Map<String, ProcessInfo> mProcessInfoMap = new HashMap<String, ProcessInfo>(10);


    public static synchronized StubManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new StubManager(hostContext);
        }

        return sInstance;
    }

    public void init() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(CATEGORY_ACTIVITY_PROXY_STUB);
        intent.setPackage(mContext.getPackageName());


        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
        for (ResolveInfo activity : activities) {
            ProcessInfo processInfo = getOrCreateProcess(activity.activityInfo);
            processInfo.addStubActivity(activity.activityInfo);
        }

        List<ResolveInfo> services = pm.queryIntentServices(intent, 0);
        for (ResolveInfo service : services) {
            ProcessInfo processInfo = getOrCreateProcess(service.serviceInfo);
            processInfo.addStubService(service.serviceInfo);
        }

        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_PROVIDERS);
            if (packageInfo.providers != null && packageInfo.providers.length > 0) {
                for (ProviderInfo providerInfo : packageInfo.providers) {
                    if (providerInfo.name != null && providerInfo.name.startsWith(Stubs.Provider.class.getName())) {
                        ProcessInfo processInfo = getOrCreateProcess(providerInfo);
                        processInfo.addStubProvider(providerInfo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Logger.d(TAG, "init() " + this);

    }

    private StubManager(Context context) {
        mContext = context;
    }

    /**
     * TODO 需要处理重复使用问题
     * @param activityInfo
     * @return
     */
    public ActivityInfo selectStubActivity(ActivityInfo activityInfo) {
        ProcessInfo processInfo = selectStubProcess(activityInfo);

        for (ActivityInfo stubActivityInfo : processInfo.getStubActivities()) {
            if (stubActivityInfo.launchMode == activityInfo.launchMode) {
                return stubActivityInfo;
            }
        }

        return null;
    }

    public void addPluginActivityInfo(ActivityInfo activityInfo) {

    }

    public void removePluginActivityInfo(ActivityInfo activityInfo) {

    }

    public ServiceInfo selectStubService(ServiceInfo serviceInfo) {
        ProcessInfo processInfo = selectStubProcess(serviceInfo);

        for (ServiceInfo stubServiceInfo : processInfo.getStubServices()) {
            return stubServiceInfo;
        }

        return null;
    }

    public ProviderInfo selectStubProvider(ProviderInfo providerInfo) {
        ProcessInfo processInfo = selectStubProcess(providerInfo);

        for (ProviderInfo stubProviderInfo : processInfo.getStubProviders()) {
            return stubProviderInfo;
        }

        return null;
    }

    private ProcessInfo getOrCreateProcess(ComponentInfo componentInfo) {
        String processName = getProcessName(componentInfo.processName, componentInfo.packageName);
        ProcessInfo processInfo = mProcessInfoMap.get(processName);
        if (processInfo == null) {
            processInfo = new ProcessInfo(processName);
            mProcessInfoMap.put(processName, processInfo);
        }
        return processInfo;
    }

    public ProcessInfo selectStubProcess(ComponentInfo componentInfo) {
        return selectStubProcess(componentInfo.processName, componentInfo.packageName);
    }

    /**
     * TODO 考虑插件进程模式的设计
     */
    public ProcessInfo selectStubProcess(String processName, String pkgName) {
        if (mProcessInfoMap.isEmpty()) {
            throw new RuntimeException("no registered stub process found");
        }

        List<ProcessInfo> processInfos = new ArrayList<>(mProcessInfoMap.values());

        if (mProcessInfoMap.size() > 1) {
            if (getProcessName(processName, pkgName).equals(pkgName)) {
                return processInfos.get(0);
            } else {
                return processInfos.get(1);
            }
        } else {
            return processInfos.get(0);
        }
    }

    public static class ProcessInfo {
        public final String processName;

        private Map<String, ActivityInfo> mStubActivityMap = new HashMap<>();
        private Map<String, ServiceInfo> mStubServiceMap = new HashMap<>();
        private Map<String, ProviderInfo> mStubProviderMap = new HashMap<>();

        private Map<String, HashSet<ActivityInfo>> mStubTargetActivityMap = new HashMap<>();

        public ProcessInfo(String packageName) {
            processName = packageName;
        }

        public void addStubActivity(ActivityInfo activityInfo) {
            mStubActivityMap.put(activityInfo.name, activityInfo);
        }

        public void addStubService(ServiceInfo serviceInfo) {
            mStubServiceMap.put(serviceInfo.name, serviceInfo);
        }

        public void addStubProvider(ProviderInfo providerInfo) {
            mStubProviderMap.put(providerInfo.name, providerInfo);
        }

        public List<ActivityInfo> getStubActivities() {
            return new ArrayList<>(mStubActivityMap.values());
        }

        public List<ServiceInfo> getStubServices() {
            return new ArrayList<>(mStubServiceMap.values());
        }

        public List<ProviderInfo> getStubProviders() {
            return new ArrayList<>(mStubProviderMap.values());
        }

        @Override
        public String toString() {
            return String.format("ProcessInfo[ mProcessName = %s, mStubActivityMap = %s, mStubServiceMap = %s, mStubProviderMap = %s ]",
                    processName, mStubActivityMap, mStubServiceMap, mStubProviderMap);
        }
    }

    @Override
    public String toString() {
        return String.format("StubManager[ mProcessInfoMap = %s ]", mProcessInfoMap);
    }

    public static String getProcessName(String processName, String pkgName) {
        if (TextUtils.isEmpty(processName)) {
            return pkgName;
        } else {
            return processName;
        }
    }


}
