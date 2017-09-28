package com.reginald.pluginm.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.reginald.pluginm.IPluginClient;
import com.reginald.pluginm.IPluginManager;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.parser.IntentMatcher;
import com.reginald.pluginm.parser.PluginPackageParser;
import com.reginald.pluginm.stub.PluginStubMainProvider;
import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.utils.CommonUtils;
import com.reginald.pluginm.utils.ConfigUtils;
import com.reginald.pluginm.utils.Logger;
import com.reginald.pluginm.utils.PackageUtils;
import com.reginald.pluginm.utils.ProcessHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 17-8-22.
 */

public class PluginManagerService extends IPluginManager.Stub {


    private static final String TAG = "PluginManagerService";
    private static volatile PluginManagerService sInstance;

    public static final String PLUGIN_ROOT = "pluginm";
    public static final String PLUGIN_APK_FOLDER_NAME = "apk";
    public static final String PLUGIN_DEX_FOLDER_NAME = "dexes";
    public static final String PLUGIN_LIB_FOLDER_NAME = "lib";

    private final Object mInstallLock = new Object();

    // 已安装插件信息：
    private final Map<String, PluginInfo> mInstalledPluginMap = new ConcurrentHashMap<>();
    private final Map<String, PluginPackageParser> mInstalledPkgParser = new ConcurrentHashMap<>();

    // 插件客户端信息：
    private final Map<String, IPluginClient> mPluginClientMap = new HashMap<>(2);
    private final Map<String, PluginProcess> mRunningPluginProcess = new ConcurrentHashMap<>();

    private Context mContext;
    private StubManager mStubManager;

    public static synchronized PluginManagerService getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginManagerService(hostContext);
        }

        return sInstance;
    }

    private PluginManagerService(Context hostContext) {
        Context appContext = hostContext.getApplicationContext();
        mContext = appContext != null ? appContext : hostContext;
        mStubManager = StubManager.getInstance(mContext);
    }

    public IPluginClient fetchPluginClient(final String processName, boolean isStartProcess) {
        StubManager.ProcessInfo processInfo = StubManager.getInstance(mContext).getProcessInfo(processName);

        Logger.d(TAG, "fetchPluginClient() processName = " + processName + " , isStartProcess = " + isStartProcess);

        IPluginClient pluginClient = null;

        synchronized (mPluginClientMap) {
            pluginClient = mPluginClientMap.get(processName);
            if (pluginClient != null) {
                Logger.d(TAG, "fetchPluginClient() success! cached pluginClient = " + pluginClient);
                return pluginClient;
            }
        }

        if (!isStartProcess) {
            return null;
        }

        List<ProviderInfo> stubProviders = processInfo.getStubProviders();
        ProviderInfo stubProvider = stubProviders != null && !stubProviders.isEmpty() ? stubProviders.get(0) : null;

        if (stubProvider == null) {
            Logger.e(TAG, "fetchPluginClient() no stub provider found to trigger start process for " + processName);
            return null;
        }

        Uri uri = Uri.parse("content://" + stubProvider.authority);

        try {
            // trigger start process
            Bundle bundle = mContext.getContentResolver().call(
                    uri, PluginStubMainProvider.METHOD_START_PROCESS, null, null);

            synchronized (mPluginClientMap) {
                pluginClient = mPluginClientMap.get(processName);
                if (pluginClient != null) {
                    Logger.d(TAG, "fetchPluginClient() success after trigger start process! pluginClient = " + pluginClient);
                    return pluginClient;
                } else {
                    Logger.e(TAG, "fetchPluginClient() trigger process error!");
                    return null;
                }
            }

        } catch (Throwable e) {
            Logger.e(TAG, "fetchPluginClient() error!", e);
        }


        return null;
    }

    private void onPluginClientStarted(final String processName, IPluginClient pluginClient) {
        Logger.d(TAG, "onPluginClientStarted() processName = " + processName + " ,pluginClient = " + pluginClient);
        synchronized (mPluginClientMap) {
            try {
                pluginClient.asBinder().linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        onPluginClientDied(processName);
                    }
                }, 0);
                mPluginClientMap.put(processName, pluginClient);
            } catch (RemoteException e) {
                Logger.e(TAG, "onPluginClientStarted() linkToDeath error!", e);
            }
        }
    }

    private void onPluginClientDied(String processName) {
        Logger.d(TAG, "onPluginClientDied() process = " + processName);
        synchronized (mPluginClientMap) {
            IPluginClient pluginClient = mPluginClientMap.remove(processName);
            Logger.d(TAG, "onPluginClientDied() remove " + (pluginClient != null ? "success!" : "error!"));
        }

        PluginProcess removed = mRunningPluginProcess.remove(processName);
        Logger.d(TAG, "onPluginClientDied() PluginProcess removed? " + removed);
    }

    public PluginProcess getPluginProcess(String processName) {
        return mRunningPluginProcess.get(processName);
    }

    // public api

    @Override
    public PluginInfo install(String pluginPath) {
        return install(pluginPath, true);
    }

    private PluginInfo install(String pluginPath, boolean isStandAlone) {
        try {
            Logger.d(TAG, "install() pluginPath = " + pluginPath);
            PluginInfo pluginInfo = null;
            File originApk = new File(pluginPath);
            if (!originApk.exists()) {
                Logger.e(TAG, "install() apk " + originApk.getAbsolutePath() + " NOT found!");
                return null;
            }

            PackageManager pm = mContext.getPackageManager();
            PackageInfo apkInfo = pm.getPackageArchiveInfo(originApk.getAbsolutePath(), 0);
            if (apkInfo == null) {
                Logger.e(TAG, "install() apk " + originApk.getAbsolutePath() + " parse error!");
                return null;
            }

            String pluginPkgName = apkInfo.packageName;
            PluginInfo installedPluginInfo = mInstalledPluginMap.get(pluginPkgName);
            if (installedPluginInfo != null) {
                Logger.d(TAG, "install() found installed pluginInfo " + installedPluginInfo);
                return installedPluginInfo;
            }

            synchronized (mInstallLock) {
                installedPluginInfo = mInstalledPluginMap.get(pluginPkgName);
                if (installedPluginInfo != null) {
                    Logger.d(TAG, "install() found installed pluginInfo " + installedPluginInfo);
                    return installedPluginInfo;
                }

                String rootDir = mContext.getDir(PLUGIN_ROOT, Context.MODE_PRIVATE).getAbsolutePath();
                File pluginDir = PackageUtils.getOrMakeDir(rootDir, pluginPkgName);

                if (!pluginDir.exists()) {
                    Logger.e(TAG, "install() pluginDir for " + pluginPkgName + " init error!");
                    return null;
                }

                File pluginApkDir = PackageUtils.getOrMakeDir(pluginDir.getAbsolutePath(), PLUGIN_APK_FOLDER_NAME);
                if (!pluginApkDir.exists()) {
                    Logger.e(TAG, "install() pluginDir " + pluginApkDir.getAbsolutePath() + " NOT found!");
                    return null;
                }

                File pluginApk = new File(pluginApkDir, "base.apk");

                boolean isSuccess = PackageUtils.copyFile(originApk.getAbsolutePath(), pluginApk.getAbsolutePath());
                if (!isSuccess) {
                    Logger.e(TAG, "install() pluginApk = " + pluginApk.getAbsolutePath() + " copy error!");
                    return null;
                }


                pluginInfo = ApkParser.parsePluginInfo(mContext, pluginApk.getAbsolutePath());
                resolveConfigInfo(pluginInfo);
                pluginInfo.apkPath = pluginApk.getAbsolutePath();
                pluginInfo.fileSize = pluginApk.length();
                pluginInfo.lastModified = pluginApk.lastModified();

                // create classloader & dexopt
                Logger.d(TAG, "install() mContext.getClassLoader() = " + mContext.getClassLoader());
                ClassLoader parentClassLoader;
                ClassLoader hostClassLoader = mContext.getClassLoader();

                if (pluginInfo.isStandAlone) {
                    parentClassLoader = hostClassLoader.getParent();
                } else {
                    parentClassLoader = hostClassLoader;
                }

                File pluginDexPath = PackageUtils.getOrMakeDir(pluginDir.getAbsolutePath(), PLUGIN_DEX_FOLDER_NAME);
                File pluginNativeLibPath = PackageUtils.getOrMakeDir(pluginDir.getAbsolutePath(), PLUGIN_LIB_FOLDER_NAME);
                DexClassLoader dexClassLoader = new PluginDexClassLoader(
                        pluginApk.getAbsolutePath(), pluginDexPath.getAbsolutePath(),
                        pluginNativeLibPath.getAbsolutePath(), parentClassLoader, hostClassLoader);

                Logger.d(TAG, "install() dexClassLoader = " + dexClassLoader);
                Logger.d(TAG, "install() dexClassLoader's parent = " + dexClassLoader.getParent());
                pluginInfo.isStandAlone = isStandAlone;
                pluginInfo.classLoader = dexClassLoader;
                pluginInfo.parentClassLoader = parentClassLoader;
                pluginInfo.dexDir = pluginDexPath.getAbsolutePath();

                // install so
                File apkParent = pluginApk.getParentFile();
                File tempSoDir = new File(apkParent, "temp");
                Set<String> soList = PackageUtils.unZipSo(pluginApk, tempSoDir);
                if (soList != null) {
                    for (String soName : soList) {
                        PackageUtils.copySo(tempSoDir, soName, pluginNativeLibPath.getAbsolutePath());
                    }
                    //删掉临时文件
                    PackageUtils.deleteAll(tempSoDir);
                }
                pluginInfo.nativeLibDir = pluginNativeLibPath.getAbsolutePath();

                Logger.d(TAG, "install() pluginInfo = " + pluginInfo);

                if (pluginInfo == null) {
                    Logger.e(TAG, "install() error! pluginInfo is null!");
                    return null;
                }

                Logger.d(TAG, "install() mInstalledPkgParser add " + pluginInfo.packageName);
                mInstalledPkgParser.put(pluginInfo.packageName, pluginInfo.pkgParser);

                Logger.d(TAG, "install() mInstalledPluginMap add " + pluginInfo.packageName);
                mInstalledPluginMap.put(pluginInfo.packageName, pluginInfo);
            }

            return pluginInfo;
        } catch (Exception e) {
            Logger.e(TAG, "install() error! exception: " + e);
            e.printStackTrace();
            return null;
        }

    }

    private void resolveConfigInfo(PluginInfo pluginInfo) {
        try {
            PluginPackageParser packageParser = pluginInfo.pkgParser;
            ApplicationInfo applicationInfo = packageParser.getApplicationInfo(PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;
            Logger.d(TAG, "resolveConfigInfo() application metaData = " + metaData);
            Map<String, Map<String, String>> invokerConfigMap = ConfigUtils.parseInvokerConfig(metaData);
            pluginInfo.pluginInvokerClassMap.putAll(invokerConfigMap);
        } catch (Exception e) {
            Logger.e(TAG, "resolveConfigInfo() error!", e);
        }
    }

    @Override
    public Intent getPluginActivityIntent(Intent originIntent) {
        ActivityInfo activityInfo = resolveActivityInfo(originIntent, 0);
        Logger.d(TAG, String.format("getPluginActivityIntent() originIntent = %s, resolved activityInfo = %s",
                originIntent, activityInfo));

        if (activityInfo == null) {
            return null;
        }

        // make a stub activity for it:
        ActivityInfo stubActivityInfo = mStubManager.selectStubActivity(activityInfo);
        Logger.d(TAG, String.format("getPluginActivityIntent() activityInfo = %s -> stub = %s",
                activityInfo, stubActivityInfo));

        if (stubActivityInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubActivityInfo.packageName, stubActivityInfo.name);
            intent.putExtra(PluginManager.EXTRA_INTENT_TARGET_ACTIVITYINFO, activityInfo);
            intent.putExtra(PluginManager.EXTRA_INTENT_STUB_INFO, stubActivityInfo);

            Logger.d(TAG, String.format("getPluginActivityIntent() stubActivityInfo = %s", stubActivityInfo));
            return intent;
        } else {
            return null;
        }
    }

    @Override
    public Intent getPluginServiceIntent(Intent originIntent) {
        ServiceInfo serviceInfo = resolveServiceInfo(originIntent, 0);
        Logger.d(TAG, String.format("getPluginServiceIntent() originIntent = %s, resolved serviceInfo = %s",
                originIntent, serviceInfo));

        if (serviceInfo == null) {
            return null;
        }

        // make a stub service for it:
        ServiceInfo stubServiceInfo = mStubManager.selectStubService(serviceInfo);
        Logger.d(TAG, String.format("getPluginServiceIntent() serviceInfo = %s -> stub = %s",
                serviceInfo, stubServiceInfo));
        if (stubServiceInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubServiceInfo.packageName, stubServiceInfo.name);
            intent.putExtra(PluginManager.EXTRA_INTENT_TARGET_SERVICEINFO, serviceInfo);
            intent.putExtra(PluginManager.EXTRA_INTENT_STUB_INFO, stubServiceInfo);

            Logger.d(TAG, String.format("getPluginServiceIntent() stubServiceInfo = %s", stubServiceInfo));
            return intent;
        } else {
            return null;
        }
    }

    @Override
    public Bundle getPluginProviderUri(String auth) {
        ProviderInfo providerInfo = PluginManager.getInstance(mContext).resolveProviderInfo(auth);
        Logger.d(TAG, "getPluginProviderUri() auth = " + auth + ",resolved providerInfo = " + providerInfo);

        if (providerInfo == null) {
            return null;
        }

        // make a stub provider for it:
        ProviderInfo stubProvider = mStubManager.selectStubProvider(providerInfo);
        Logger.d(TAG, String.format("getPluginProviderUri() providerInfo = %s -> stub = %s",
                providerInfo, stubProvider));

        if (stubProvider != null) {
            Bundle resultBundle = new Bundle();
            Bundle providerBundle = new Bundle();
            providerBundle.putParcelable(PluginManager.EXTRA_INTENT_TARGET_PROVIDERINFO, providerInfo);
            providerBundle.putParcelable(PluginManager.EXTRA_INTENT_STUB_INFO, stubProvider);
            resultBundle.putBundle("bundle", providerBundle);
            resultBundle.putParcelable("uri", Uri.parse("content://" + stubProvider.authority));
            return resultBundle;
        } else {
            return null;
        }
    }

    @Override
    public String selectStubProcessName(String processName, String pkgName) {
        StubManager.ProcessInfo processInfo = mStubManager.selectStubProcess(processName, pkgName);
        return processInfo.processName;
    }

    @Override
    public ActivityInfo resolveActivityInfo(Intent intent, int flags) {
        if (intent.getComponent() != null) {
            return getActivityInfo(intent.getComponent(), flags);
        }
        try {
            List<ResolveInfo> resolveInfos = IntentMatcher.resolveActivityIntent(mContext, mInstalledPkgParser, intent, intent.resolveTypeIfNeeded(mContext.getContentResolver()), flags);
            if (resolveInfos == null || resolveInfos.isEmpty()) {
                return null;
            }
            // choose the first one:
            ResolveInfo finalInfo = resolveInfos.get(0);
            return finalInfo.activityInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ServiceInfo resolveServiceInfo(Intent intent, int flags) {
        if (intent.getComponent() != null) {
            return getServiceInfo(intent.getComponent(), flags);
        }
        try {
            List<ResolveInfo> resolveInfos = IntentMatcher.resolveServiceIntent(mContext, mInstalledPkgParser, intent, intent.resolveTypeIfNeeded(mContext.getContentResolver()), flags);
            if (resolveInfos == null || resolveInfos.isEmpty()) {
                return null;
            }
            // choose the first one:
            ResolveInfo finalInfo = resolveInfos.get(0);
            return finalInfo.serviceInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ProviderInfo resolveProviderInfo(String name) {
        Logger.d(TAG, "resolveProviderInfo() name = " + name + " ,mInstalledPkgParser = " + mInstalledPkgParser);
        try {
            for (PluginPackageParser pluginPackageParser : mInstalledPkgParser.values()) {
                List<ProviderInfo> providerInfos = pluginPackageParser.getProviders();
                for (ProviderInfo providerInfo : providerInfos) {
                    Logger.d(TAG, "resolveProviderInfo() check providerInfo " + providerInfo);
                    if (TextUtils.equals(providerInfo.authority, name)) {
                        return providerInfo;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        try {
            return IntentMatcher.resolveActivityIntent(mContext, mInstalledPkgParser, intent, intent.resolveTypeIfNeeded(mContext.getContentResolver()), flags);
        } catch (Exception e) {
            Logger.e(TAG, "queryIntentActivities() intent = " + intent, e);
        }
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        try {
            return IntentMatcher.resolveServiceIntent(mContext, mInstalledPkgParser, intent, intent.resolveTypeIfNeeded(mContext.getContentResolver()), flags);
        } catch (Exception e) {
            Logger.e(TAG, "queryIntentServices() intent = " + intent, e);
        }
        return null;
    }

    @Override
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        try {
            return IntentMatcher.resolveReceiverIntent(mContext, mInstalledPkgParser, intent, intent.resolveTypeIfNeeded(mContext.getContentResolver()), flags);
        } catch (Exception e) {
            Logger.e(TAG, "queryBroadcastReceivers() intent = " + intent, e);
        }
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        try {
            return IntentMatcher.resolveProviderIntent(mContext, mInstalledPkgParser, intent, intent.resolveTypeIfNeeded(mContext.getContentResolver()), flags);
        } catch (Exception e) {
            Logger.e(TAG, "queryIntentContentProviders() intent = " + intent, e);
        }
        return null;
    }

    @Override
    public ActivityInfo getActivityInfo(ComponentName componentName, int flags) {
        Logger.d(TAG, "getActivityInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getActivityInfo(componentName, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ServiceInfo getServiceInfo(ComponentName componentName, int flags) {
        Logger.d(TAG, "getServiceInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getServiceInfo(componentName, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ActivityInfo getReceiverInfo(ComponentName componentName, int flags) {
        Logger.d(TAG, "getActivityInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getReceiverInfo(componentName, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    @Override
    public ProviderInfo getProviderInfo(ComponentName componentName, int flags) {
        Logger.d(TAG, "getProviderInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getProviderInfo(componentName, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags) {
        Logger.d(TAG, "getPackageInfo() packageName = " + packageName + ", flags = " + flags);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(packageName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getPackageInfo(flags);
        } catch (Exception e) {
            Logger.e(TAG, "getPackageInfo()", e);
        }

        return null;
    }

    @Override
    public void onPluginProcessAttached(IBinder client) throws RemoteException {
        int pid = Binder.getCallingPid();
        String processName = ProcessHelper.getProcessName(mContext, pid);

        Logger.d(TAG, String.format("onPluginProcessAttached() pid = %s, processName = %s, client = %s",
                pid, processName, client));

        if (TextUtils.isEmpty(processName)) {
            throw new IllegalStateException("unknown process " + pid);
        }

        if (client != null) {
            IPluginClient pluginClient = IPluginClient.Stub.asInterface(client);
            onPluginClientStarted(processName, pluginClient);
        }

    }

    @Override
    public void onApplicationAttached(ApplicationInfo targetInfo, String processName) throws RemoteException {

        PluginProcess pluginProcess = mRunningPluginProcess.get(processName);
        StubManager.ProcessInfo stubProcessInfo = mStubManager.getProcessInfo(processName);

        Logger.d(TAG, String.format("onApplicationAttached() targetInfo = %s, processName = %s, stubProcessInfo = %s",
                targetInfo, processName, stubProcessInfo));

        if (stubProcessInfo == null) {
            throw new IllegalStateException("no stub processInfo found for process " + processName);
        }

        if (pluginProcess == null) {
            pluginProcess = new PluginProcess(stubProcessInfo);
            mRunningPluginProcess.put(processName, pluginProcess);
        }

        pluginProcess.onApplicationAttached(targetInfo);
    }

    @Override
    public void onActivityCreated(ActivityInfo stubInfo, ActivityInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onActivityCreated() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        String processName = stubInfo.processName;
        PluginProcess pluginProcess = mRunningPluginProcess.get(processName);

        if (pluginProcess == null) {
            throw new IllegalStateException("no PluginProcess found for process " + processName);
        }

        pluginProcess.onActivityCreated(stubInfo, targetInfo);
    }

    @Override
    public void onActivityDestory(ActivityInfo stubInfo, ActivityInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onActivityDestory() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        String processName = stubInfo.processName;
        PluginProcess pluginProcess = mRunningPluginProcess.get(processName);

        if (pluginProcess == null) {
            throw new IllegalStateException("no PluginProcess found for process " + processName);
        }

        pluginProcess.onActivityDestory(stubInfo, targetInfo);
    }

    @Override
    public void onServiceCreated(ServiceInfo stubInfo, ServiceInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onServiceCreated() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        String processName = stubInfo.processName;
        PluginProcess pluginProcess = mRunningPluginProcess.get(processName);

        if (pluginProcess == null) {
            throw new IllegalStateException("no PluginProcess found for process " + processName);
        }

        pluginProcess.onServiceCreated(stubInfo, targetInfo);
    }

    @Override
    public void onServiceDestory(ServiceInfo stubInfo, ServiceInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onServiceDestory() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        String processName = stubInfo.processName;
        PluginProcess pluginProcess = mRunningPluginProcess.get(processName);

        if (pluginProcess == null) {
            throw new IllegalStateException("no PluginProcess found for process " + processName);
        }

        pluginProcess.onServiceDestory(stubInfo, targetInfo);
    }

    @Override
    public void onProviderCreated(ProviderInfo stubInfo, ProviderInfo targetInfo) throws RemoteException {
        Logger.d(TAG, String.format("onProviderCreated() stubInfo = %s, targetInfo = %s", stubInfo, targetInfo));
        String processName = stubInfo.processName;
        PluginProcess pluginProcess = mRunningPluginProcess.get(processName);

        if (pluginProcess == null) {
            throw new IllegalStateException("no PluginProcess found for process " + processName);
        }

        pluginProcess.onProviderCreated(stubInfo, targetInfo);
    }

    @Override
    public PluginInfo getInstalledPluginInfo(String packageName) {
        return mInstalledPluginMap.get(packageName);
    }

    @Override
    public List<PluginInfo> getAllInstalledPlugins() {
        return new ArrayList<>(mInstalledPluginMap.values());
    }

    private PluginPackageParser getPackageParserForComponent(ComponentName componentName) {
        return getPackageParserForComponent(componentName.getPackageName());
    }

    private PluginPackageParser getPackageParserForComponent(String packageName) {
        PluginInfo pluginInfo = getInstalledPluginInfo(packageName);
        if (pluginInfo == null) {
            return null;
        }

        return pluginInfo.pkgParser;
    }

    public static Intent handleOriginalIntent(Intent origIntent) {
        Intent newIntent = new Intent(origIntent);
        newIntent.replaceExtras((Bundle) null);
        newIntent.setAction(null);
        newIntent.putExtra(PluginManager.EXTRA_INTENT_ORIGINAL_INTENT, origIntent);
        return newIntent;
    }

    public static Intent recoverOriginalIntent(Intent pluginIntent, ClassLoader classLoader) {
        Intent origIntent = pluginIntent.getParcelableExtra(PluginManager.EXTRA_INTENT_ORIGINAL_INTENT);
        origIntent.setExtrasClassLoader(classLoader);
        return origIntent;
    }
}
