package com.reginald.pluginm.stub;

import android.app.ActivityManager;
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

import com.reginald.pluginm.PluginM;
import com.reginald.pluginm.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by lxy on 16-10-31.
 */
public class StubManager {
    public static final int PROCESS_TYPE_STANDALONE = 1;
    public static final int PROCESS_TYPE_DUAL = 2;

    private static final String TAG = "StubManager";
    private static final String CATEGORY_ACTIVITY_PROXY_STUB = "com.reginald.pluginm.category.STUB";
    private static volatile StubManager sInstance;

    private Context mContext;
    private int mProcessType = PROCESS_TYPE_STANDALONE;

    private final Map<String, ProcessInfo> mStubProcessInfoMap = new HashMap<String, ProcessInfo>(10);

    private final Map<String, ProcessInfo> mPluginProcessMap = new HashMap<>(10);

    public static synchronized StubManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new StubManager(hostContext);
        }

        return sInstance;
    }

    private StubManager(Context context) {
        mContext = context;
        mProcessType = PluginM.getConfigs().getProcessType();
        init();
    }

    private void init() {
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

    public ProcessInfo getProcessInfo(String processName) {
        return mStubProcessInfoMap.get(processName);
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
        ProcessInfo processInfo = mStubProcessInfoMap.get(processName);
        if (processInfo == null) {
            processInfo = new ProcessInfo(processName);
            mStubProcessInfoMap.put(processName, processInfo);
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
        if (mStubProcessInfoMap.isEmpty()) {
            throw new RuntimeException("no registered stub process found");
        }

        List<ProcessInfo> stubProcessInfos = new ArrayList<>(mStubProcessInfoMap.values());

        switch (mProcessType) {
            case PROCESS_TYPE_STANDALONE:
                filterPluginProcessMap();

                ProcessInfo processInfo = mPluginProcessMap.get(pkgName);
                if (processInfo != null) {
                    return processInfo;
                }
                for (ProcessInfo p : stubProcessInfos) {
                    if (!mPluginProcessMap.containsValue(p)) {
                        mPluginProcessMap.put(pkgName, p);
                        return p;
                    }
                }

                throw new IllegalStateException("No more stub process for plugin " + pkgName);

            case PROCESS_TYPE_DUAL:
                if (mStubProcessInfoMap.size() > 1) {
                    if (getProcessName(processName, pkgName).equals(pkgName)) {
                        return stubProcessInfos.get(0);
                    } else {
                        return stubProcessInfos.get(1);
                    }
                } else {
                    return stubProcessInfos.get(0);
                }
        }

        return null;
    }

    private void filterPluginProcessMap() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();

        Set<String> runningStubProcesses = new HashSet<>();

        for (ActivityManager.RunningAppProcessInfo info : runningAppProcesses) {
            if (info.processName.startsWith(mContext.getPackageName() + ":p")) {
                runningStubProcesses.add(info.processName);
            }
        }

        Logger.d(TAG, "filterPluginProcessMap() runningStubProcesses = " + runningStubProcesses);

        Set<Map.Entry<String, ProcessInfo>> entries = mPluginProcessMap.entrySet();
        Iterator<Map.Entry<String, ProcessInfo>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ProcessInfo> entry = iterator.next();
            if (!runningStubProcesses.contains(entry.getValue().processName)) {
                iterator.remove();
            }
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ProcessInfo) {
                ProcessInfo othProcessInfo = (ProcessInfo) obj;
                return processName.equals(othProcessInfo.processName);
            }

            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("StubManager[ mProcessType = %d, mStubProcessInfoMap = %s ]",
                mProcessType, mStubProcessInfoMap);
    }

    public static String getProcessName(String processName, String pkgName) {
        if (TextUtils.isEmpty(processName)) {
            return pkgName;
        } else {
            return processName;
        }
    }


}
