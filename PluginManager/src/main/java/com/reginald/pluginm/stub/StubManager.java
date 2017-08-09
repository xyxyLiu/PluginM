package com.reginald.pluginm.stub;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;
import android.util.Log;

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
                    if (providerInfo.name != null && providerInfo.name.startsWith(PluginStubMainProvider.class.getName())) {
                        ProcessInfo processInfo = getOrCreateProcess(providerInfo);
                        processInfo.addStubProvider(providerInfo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "init() " + this);

    }

    private ProcessInfo getOrCreateProcess(ComponentInfo componentInfo) {
        String processName = getProcessName(componentInfo);
        ProcessInfo processInfo = mProcessInfoMap.get(processName);
        if (processInfo == null) {
            processInfo = new ProcessInfo(processName);
            mProcessInfoMap.put(processName, processInfo);
        }
        return processInfo;
    }

    private StubManager(Context context) {
        mContext = context;
    }

    public void addPluginActivityInfo(ActivityInfo activityInfo) {

    }

    public void removePluginActivityInfo(ActivityInfo activityInfo) {

    }

    public ActivityInfo getStubActivity(ActivityInfo activityInfo) {
        ActivityInfo stubActivityInfo = new ActivityInfo();
        stubActivityInfo.applicationInfo = new ApplicationInfo();
        stubActivityInfo.packageName = mContext.getPackageName();
        stubActivityInfo.name = String.format(PluginHostProxy.STUB_ACTIVITY, "P0", "Standard0");
        return stubActivityInfo;
    }


    private static class ProcessInfo {
        private final String mProcessName;

        private Map<String, ActivityInfo> mStubActivityMap = new HashMap<>();
        private Map<String, ServiceInfo> mStubServiceMap = new HashMap<>();
        private Map<String, ProviderInfo> mStubProviderMap = new HashMap<>();

        private Map<String, HashSet<ActivityInfo>> mStubTargetActivityMap = new HashMap<>();

        public ProcessInfo(String packageName) {
            mProcessName = packageName;
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

//        public ActivityInfo getStubActivity(ActivityInfo activityInfo) {
//            ActivityInfo stubActivityInfo = new ActivityInfo();
//            stubActivityInfo.packageName = mContext.getPackageName();
//            stubActivityInfo.name = String.format(PluginHostProxy.STUB_ACTIVITY, "P0","Standard0");
//            return stubActivityInfo;
//        }

        @Override
        public String toString() {
            return String.format("ProcessInfo[ mProcessName = %s, mStubActivityMap = %s, mStubServiceMap = %s, mStubProviderMap = %s ]",
                    mProcessName, mStubActivityMap, mStubServiceMap, mStubProviderMap);
        }
    }

    @Override
    public String toString() {
        return String.format("StubManager[ mProcessInfoMap = %s ]", mProcessInfoMap);
    }

    public static String getProcessName(ComponentInfo componentInfo) {
        if (TextUtils.isEmpty(componentInfo.processName)) {
            return componentInfo.packageName;
        } else {
            return componentInfo.processName;
        }
    }

}
