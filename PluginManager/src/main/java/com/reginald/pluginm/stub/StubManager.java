package com.reginald.pluginm.stub;

import android.app.ActivityManager;
import android.content.ComponentName;
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
import com.reginald.pluginm.core.PluginManagerService;
import com.reginald.pluginm.core.PluginProcess;
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
    /**
     * 独立进程模式： 每个插件分配一个进程。
     */
    public static final int PROCESS_TYPE_INDEPENDENT = 1;

    /**
     * 单一进程模式: 所有插件都分配在一个固定的进程。
     */
    public static final int PROCESS_TYPE_SINGLE = 2;

    /**
     * 双进程模式: 所有插件都分配在两个固定的进程，进程名与插件名称相同的在一个特定进程，否则在另一个特定进程。
     */
    public static final int PROCESS_TYPE_DUAL = 3;


    private static final String TAG = "StubManager";
    private static final String CATEGORY_ACTIVITY_PROXY_STUB = "com.reginald.pluginm.category.STUB";
    private static volatile StubManager sInstance;
    private final Set<String> mStubRequestedPermission = new HashSet<>();
    private final Map<String, ProcessInfo> mStubProcessInfoMap = new HashMap<String, ProcessInfo>(10);
    // PROCESS_TYPE_INDEPENDENT 模式下的进程分配map: pkgname -> ProcessInfo
    private final Map<String, ProcessInfo> mPluginSingleProcessMap = new HashMap<>(10);
    private Context mContext;
    private int mProcessType = PROCESS_TYPE_INDEPENDENT;

    private StubManager(Context context) {
        mContext = context;
        mProcessType = PluginM.getConfigs().getProcessType();
        init();
    }

    public static synchronized StubManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new StubManager(hostContext);
        }

        return sInstance;
    }

    public static String getProcessName(String processName, String pkgName) {
        if (TextUtils.isEmpty(processName)) {
            return pkgName;
        } else {
            return processName;
        }
    }

    public static boolean isStubIntent(Context hostContext, Intent intent) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null && hostContext.getPackageName().equals(componentName.getPackageName()) &&
                !TextUtils.isEmpty(componentName.getClassName()) &&
                componentName.getClassName().startsWith(Stubs.class.getName())) {
            return true;
        }

        return false;
    }

    private void init() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(CATEGORY_ACTIVITY_PROXY_STUB);
        intent.setPackage(mContext.getPackageName());
        PackageManager pm = mContext.getPackageManager();

        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (packageInfo != null) {
                String[] permissions = packageInfo.requestedPermissions;
                if (permissions != null && permissions.length > 0) {
                    for (String permission : permissions) {
                        mStubRequestedPermission.add(permission);
                    }
                }
            }
            Logger.d(TAG, "init() requested permissions size = " + mStubRequestedPermission.size());
        } catch (Exception e) {
            Logger.e(TAG, "CAN NOT GET stub pakageInfo!", e);
        }

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
            Logger.e(TAG, "init() error!", e);
        }

        Logger.d(TAG, "init() " + this);

    }

    public Set<String> getStubPermissions() {
        return mStubRequestedPermission;
    }

    public ProcessInfo getProcessInfo(String processName) {
        return mStubProcessInfoMap.get(processName);
    }

    /**
     * TODO 需要处理Theme
     * @param activityInfo
     * @return
     */
    public ActivityInfo selectStubActivity(ActivityInfo activityInfo) {
        ProcessInfo processInfo = selectStubProcess(activityInfo);

        List<ActivityInfo> stubActivities = processInfo.getStubActivities();

        for (ActivityInfo stubActivityInfo : stubActivities) {
            if (stubActivityInfo.launchMode == activityInfo.launchMode) {
                switch (stubActivityInfo.launchMode) {
                    case ActivityInfo.LAUNCH_MULTIPLE:
                        return stubActivityInfo;
                    case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                    case ActivityInfo.LAUNCH_SINGLE_TASK:
                    case ActivityInfo.LAUNCH_SINGLE_TOP:
                        PluginProcess pluginProcess = PluginManagerService.getInstance(mContext).
                                getPluginProcess(processInfo.processName);
                        if (pluginProcess == null ||
                                pluginProcess.canUseActivity(stubActivityInfo, activityInfo)) {
                            return stubActivityInfo;
                        }
                        break;
                }
            }
        }

        throw new IllegalStateException("no valid stubActivity found for " + activityInfo);
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
        ProcessInfo processInfo = selectStubProcess(componentInfo.processName, componentInfo.packageName);
        Logger.d(TAG, "selectStubProcess() componentInfo = " + componentInfo +
                " -> stub process = " + processInfo.processName);
        return processInfo;
    }

    /**
     * TODO 考虑插件进程模式的设计
     */
    public ProcessInfo selectStubProcess(String processName, String pkgName) {
        if (mStubProcessInfoMap.isEmpty()) {
            throw new RuntimeException("no registered stub process found for plugin process " + processName);
        }

        List<ProcessInfo> stubProcessInfos = new ArrayList<>(mStubProcessInfoMap.values());

        switch (mProcessType) {
            case PROCESS_TYPE_INDEPENDENT:
                filterPluginProcessMap();

                ProcessInfo processInfo = mPluginSingleProcessMap.get(pkgName);
                if (processInfo != null) {
                    return processInfo;
                }
                for (ProcessInfo p : stubProcessInfos) {
                    if (!mPluginSingleProcessMap.containsValue(p)) {
                        mPluginSingleProcessMap.put(pkgName, p);
                        return p;
                    }
                }

                throw new IllegalStateException("No more stub process for plugin " + pkgName);

            case PROCESS_TYPE_SINGLE:
                return stubProcessInfos.get(0);

            case PROCESS_TYPE_DUAL:
                if (stubProcessInfos.size() > 1) {
                    String pluginProcessName = getProcessName(processName, pkgName);
                    Logger.d(TAG, "selectStubProcess() pluginProcessName = " + pluginProcessName);
                    if (pkgName.equals(pluginProcessName)) {
                        return stubProcessInfos.get(0);
                    } else {
                        return stubProcessInfos.get(1);
                    }
                } else {
                    Logger.w(TAG, "selectStubProcess() PROCESS_TYPE_DUAL need at least 2 stub process, " +
                            "but only one stub process provided! ");
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

        Set<Map.Entry<String, ProcessInfo>> entries = mPluginSingleProcessMap.entrySet();
        Iterator<Map.Entry<String, ProcessInfo>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ProcessInfo> entry = iterator.next();
            if (!runningStubProcesses.contains(entry.getValue().processName)) {
                iterator.remove();
            }
        }
    }

    @Override
    public String toString() {
        return String.format("StubManager[ mProcessType = %d, mStubProcessInfoMap = %s, mPermissions = %s]",
                mProcessType, mStubProcessInfoMap, mStubRequestedPermission);
    }

    public static class ProcessInfo {
        public final String processName;

        private final List<ActivityInfo> mStubActivityList = new ArrayList<>();
        private final List<ServiceInfo> mStubServiceList = new ArrayList<>();
        private final List<ProviderInfo> mStubProviderList = new ArrayList<>();

        public ProcessInfo(String processName) {
            this.processName = processName;
        }

        public void addStubActivity(ActivityInfo activityInfo) {
            mStubActivityList.add(activityInfo);
        }

        public void addStubService(ServiceInfo serviceInfo) {
            mStubServiceList.add(serviceInfo);
        }

        public void addStubProvider(ProviderInfo providerInfo) {
            mStubProviderList.add(providerInfo);
        }

        public List<ActivityInfo> getStubActivities() {
            return mStubActivityList;
        }

        public List<ServiceInfo> getStubServices() {
            return mStubServiceList;
        }

        public List<ProviderInfo> getStubProviders() {
            return mStubProviderList;
        }


        @Override
        public String toString() {
            return String.format("ProcessInfo[ mProcessName = %s, mStubActivityMap = %s, mStubServiceMap = %s, mStubProviderMap = %s ]",
                    processName, mStubActivityList, mStubServiceList, mStubProviderList);
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


}
