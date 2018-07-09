package com.reginald.pluginm.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.reginald.pluginm.BuildConfig;
import com.reginald.pluginm.IPluginClient;
import com.reginald.pluginm.IPluginManager;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.PluginM;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.parser.IntentMatcher;
import com.reginald.pluginm.parser.PluginPackageParser;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.stub.PluginStubMainProvider;
import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.utils.ConfigUtils;
import com.reginald.pluginm.utils.Logger;
import com.reginald.pluginm.utils.PackageUtils;
import com.reginald.pluginm.utils.ProcessHelper;

import android.app.Activity;
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
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 17-8-22.
 */

public class PluginManagerService extends IPluginManager.Stub {

    private static final boolean DEBUG = BuildConfig.DEBUG_LOG;
    private static final String TAG = "PluginManagerService";
    private static volatile PluginManagerService sInstance;

    private final Object mInstallLock = new Object();

    // 已安装的插件信息：
    private final Map<String, PluginInfo> mInstalledPluginMap = new ConcurrentHashMap<>();
    private final Map<String, PluginPackageParser> mInstalledPkgParser = new ConcurrentHashMap<>();

    // 运行中的插件信息：
    private final Map<String, IPluginClient> mPluginClientMap = new HashMap<>(2);
    private final Map<String, PluginProcess> mRunningPluginProcess = new ConcurrentHashMap<>();


    private Context mContext;
    private StubManager mStubManager;

    private PluginManagerService(Context hostContext) {
        Context appContext = hostContext.getApplicationContext();
        mContext = appContext != null ? appContext : hostContext;
        mStubManager = StubManager.getInstance(mContext);
        onPluginsInit();
    }

    public static synchronized PluginManagerService getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginManagerService(hostContext);
        }

        return sInstance;
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
        if (origIntent != null) {
            origIntent.setExtrasClassLoader(classLoader);
            return origIntent;
        }

        return pluginIntent;
    }

    public static void setActivityIntent(Activity activity, Intent intent) {
        try {
            FieldUtils.writeField(Activity.class, "mIntent", activity, intent);
        } catch (Exception e) {
            Logger.e(TAG, "setActivityIntent() error!", e);
            activity.setIntent(intent);
        }
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

    private void onPluginsInit() {
        long realtime = SystemClock.elapsedRealtime();
        List<File> apkfiles = new ArrayList<>();
        try {
            File baseDir = mContext.getDir(PackageUtils.PLUGIN_ROOT, Context.MODE_PRIVATE);
            File[] dirs = baseDir.listFiles();
            for (File pluginDir : dirs) {
                if (pluginDir.isDirectory()) {
                    File apkDir = new File(pluginDir, PackageUtils.PLUGIN_APK_FOLDER_NAME);
                    if (apkDir.isDirectory()) {
                        File apkFile = new File(apkDir, PackageUtils.PLUGIN_APK_FILE_NAME);
                        if (apkFile.exists()) {
                            apkfiles.add(apkFile);
                        } else {
                            PackageUtils.deleteAll(pluginDir);
                            Logger.d(TAG, "onPluginsInit() no apk found in " + pluginDir + ". delete!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "onPluginsInit() scan a apk file error", e);
        }

        Logger.d(TAG, String.format("onPluginsInit() find %d internal apk(s) cost %s ms",
                apkfiles.size(), (SystemClock.elapsedRealtime() - realtime)));

        realtime = SystemClock.elapsedRealtime();
        if (apkfiles != null && apkfiles.size() > 0) {
            for (File pluginFile : apkfiles) {
                long subTime = SystemClock.elapsedRealtime();
                try {
                    PluginInfo pluginInfo = install(pluginFile.getAbsolutePath(), true, false);
                    Logger.d(TAG, String.format("onPluginsInit() install %s %s! cost %s ms",
                            pluginFile.getAbsoluteFile(), pluginInfo != null ? "ok" : "error",
                            (SystemClock.elapsedRealtime() - subTime)));
                } catch (Throwable e) {
                    Logger.e(TAG, "onPluginsInit() install internal apk file error for " + pluginFile.getAbsolutePath(), e);
                }
            }
        }

        Logger.d(TAG, String.format("onPluginsInit() install %d internal apk(s) cost %s ms",
                apkfiles.size(), (SystemClock.elapsedRealtime() - realtime)));

    }

    // public api

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

    public String getPluginProcessName(int pid) {
        for (PluginProcess pluginProcess : mRunningPluginProcess.values()) {
            if (pid == pluginProcess.getPid()) {
                return pluginProcess.getPluginProcessName();
            }
        }
        return null;
    }

    @Override
    public PluginInfo install(String pluginPath, boolean isInternal, boolean isLoadDex) {
        try {
            Logger.d(TAG, String.format("install() pluginPath = %s, isInternal? %b, isLoadDex? %b",
                    pluginPath, isInternal, isLoadDex));

            PluginInfo pluginInfo = null;
            File originApk = new File(pluginPath);
            if (!originApk.exists()) {
                Logger.e(TAG, "install() apk " + originApk.getAbsolutePath() + " NOT found!");
                return null;
            }

            pluginInfo = ApkParser.parsePluginInfo(mContext, pluginPath);
            if (pluginInfo == null) {
                Logger.e(TAG, "install() apk " + originApk.getAbsolutePath() + " parse error!");
                return null;
            }
            String pluginPkgName = pluginInfo.packageName;

            resolveConfigInfo(pluginInfo);
            pluginInfo.dexDir = PackageUtils.makePluginDexDir(mContext, pluginPkgName).getAbsolutePath();
            pluginInfo.nativeLibDir = PackageUtils.makePluginLibDir(mContext, pluginPkgName).getAbsolutePath();
            pluginInfo.apkPath = PackageUtils.getPluginApk(mContext, pluginPkgName).getAbsolutePath();
            pluginInfo.fileSize = originApk.length();
            pluginInfo.lastModified = originApk.lastModified();

            if (!checkInstall(pluginInfo, isInternal)) {
                Logger.e(TAG, String.format("install() invalid plugin! plugin = %s", pluginInfo));
                return null;
            }

            synchronized (mInstallLock) {
                PluginInfo installedPluginInfo = mInstalledPluginMap.get(pluginPkgName);

                // already installed
                if (installedPluginInfo != null) {
                    if (!checkUpdate(pluginInfo, installedPluginInfo)) {
                        Logger.e(TAG, String.format("install() invalid update! Try update new plugin = %s,  old plugin = %s ", pluginInfo, installedPluginInfo));
                        return null;
                    }

                    if (isPluginRunning(pluginPkgName)) {
                        // running now ... wait process reboot
                        Logger.w(TAG, String.format("install() wait reboot! Try update new plugin = %s,  old RUNNING plugin = %s ", pluginInfo, installedPluginInfo));
                        return null;
                    }
                }

                // if not from internal apk
                if (!isInternal) {
                    boolean isSuccess = PackageUtils.copyFile(originApk.getAbsolutePath(), pluginInfo.apkPath);
                    if (!isSuccess) {
                        Logger.e(TAG, String.format("install() copy apk from %s to %s error!", originApk.getAbsolutePath(), pluginInfo.apkPath));
                        return null;
                    }
                    File apkFile = new File(pluginInfo.apkPath);

                    // create classloader & dexopt
                    if (isLoadDex) {
                        Logger.d(TAG, "install() mContext.getClassLoader() = " + mContext.getClassLoader());
                        ClassLoader parentClassLoader;
                        ClassLoader hostClassLoader = mContext.getClassLoader();

                        parentClassLoader = hostClassLoader.getParent();
                        DexClassLoader dexClassLoader = new PluginDexClassLoader(
                                pluginInfo.apkPath, pluginInfo.dexDir,
                                pluginInfo.nativeLibDir, parentClassLoader, hostClassLoader);

                        Logger.d(TAG, "install() dexClassLoader = " + dexClassLoader);
                        Logger.d(TAG, "install() dexClassLoader's parent = " + dexClassLoader.getParent());

                        pluginInfo.classLoader = dexClassLoader;
                        pluginInfo.parentClassLoader = parentClassLoader;
                    }

                    // install so
                    File tempSoDir = new File(apkFile.getParentFile(), "temp");
                    Set<String> soList = PackageUtils.unZipSo(apkFile, tempSoDir);
                    if (soList != null) {
                        for (String soName : soList) {
                            PackageUtils.copySo(tempSoDir, soName, pluginInfo.nativeLibDir);
                        }
                        //删掉临时文件
                        PackageUtils.deleteAll(tempSoDir);
                    }
                }

                Logger.d(TAG, "install() pluginInfo = " + pluginInfo);

                Logger.d(TAG, "install() mInstalledPkgParser add " + pluginInfo.packageName);
                mInstalledPkgParser.put(pluginInfo.packageName, pluginInfo.pkgParser);

                Logger.d(TAG, "install() mInstalledPluginMap add " + pluginInfo.packageName);
                mInstalledPluginMap.put(pluginInfo.packageName, pluginInfo);
            }

            return pluginInfo;
        } catch (Exception e) {
            Logger.e(TAG, "install() error!", e);
            return null;
        }

    }

    @Override
    public PluginInfo uninstall(String pluginPackageName) throws RemoteException {
        Logger.d(TAG, "uninstall() pluginPackageName = " + pluginPackageName);
        synchronized (mInstallLock) {
            PluginInfo installedPluginInfo = mInstalledPluginMap.get(pluginPackageName);
            if (installedPluginInfo != null) {
                if (!isPluginRunning(pluginPackageName)) {
                    mInstalledPluginMap.remove(pluginPackageName);
                    mInstalledPkgParser.remove(pluginPackageName);
                    File pluginDir = PackageUtils.getPluginDir(mContext, pluginPackageName);
                    PackageUtils.deleteAll(pluginDir);
                    Logger.d(TAG, "uninstall() ok! uninstalledPluginInfo = " + installedPluginInfo);
                    return installedPluginInfo;
                } else {
                    Logger.w(TAG, "uninstall() plugin " + pluginPackageName + " is running now!");
                }
            }
        }

        Logger.w(TAG, "uninstall() failed! " + pluginPackageName);
        return null;
    }

    private boolean checkUpdate(PluginInfo newPlugin, PluginInfo oldPlugin) {
        // check version

        // debug模式下允许同版本覆盖
        if (DEBUG && newPlugin.versionCode == oldPlugin.versionCode) {
            Logger.w(TAG, "equal version update approved in DEBUG MODE!");
            return true;
        }

        if (newPlugin.versionCode > oldPlugin.versionCode) {
            return true;
        }
        return false;
    }

    private boolean checkInstall(PluginInfo newPlugin, boolean isInternal) {
        try {
            // load cached signatures
            Signature[] signatures = null;
            if (isInternal) {
                // read signature from cache
                signatures = PackageUtils.readSignatures(mContext, newPlugin.packageName);
                if (signatures != null) {
                    // write cached signature to package parser.
                    newPlugin.pkgParser.writeSignature(signatures);
                }
            }

            int flags = PackageManager.GET_PERMISSIONS;
            if (signatures == null) {
                flags |= PackageManager.GET_SIGNATURES;
            }
            PackageInfo pkgInfo = newPlugin.pkgParser.getPackageInfo(flags);

            // check sdk config
            ApplicationInfo stubAppInfo = mStubManager.getStubApplicationInfo();
            ApplicationInfo pluginAppInfo = pkgInfo.applicationInfo;
            int sdkVersion = Build.VERSION.SDK_INT;
            int minSdkVersion = -1;
            int pluginTargetSdkVersion = -1;
            if (stubAppInfo == null || pluginAppInfo == null) {
                throw new IllegalStateException("application is null!");
            }

            try {
                minSdkVersion = pluginAppInfo.minSdkVersion;
            } catch (Throwable t) {
                Logger.e(TAG, "error get minSdkVersion configs", t);
            }

            try {
                pluginTargetSdkVersion = stubAppInfo.targetSdkVersion;
            } catch (Throwable t) {
                Logger.e(TAG, "error get targetSdkVersion configs", t);
            }

            if (minSdkVersion > 0 && minSdkVersion > sdkVersion) {
                Logger.e(TAG, String.format("checkInstall() minSdkVersion for " +
                        "plugin %s is %d, but current sdk version is %d!",
                        newPlugin.packageName, pluginAppInfo.minSdkVersion, sdkVersion));
                return false;
            }
            if (pluginTargetSdkVersion > 0 && stubAppInfo.targetSdkVersion > pluginTargetSdkVersion &&
                    sdkVersion >= pluginTargetSdkVersion) {
                Logger.e(TAG, String.format("checkInstall() targetSdkVersion for plugin %s is %d " +
                                "which is smaller than stub targetSdkVersion %d , and current sdk version is %d!",
                        newPlugin.packageName, pluginAppInfo.targetSdkVersion,
                        stubAppInfo.targetSdkVersion, sdkVersion));
//                return false;
            }

            // load signature from packageManager
            if (signatures == null) {
                if (pkgInfo.signatures == null || pkgInfo.signatures.length <= 0) {
                    throw new IllegalStateException("CAN NOT get signatures for " + newPlugin.packageName);
                }
                signatures = pkgInfo.signatures;
                PackageUtils.saveSignatures(mContext, pkgInfo);
            }

            // check signatures:
            if (PluginM.getConfigs().isSignatureCheckEnabled()) {
                Logger.e(TAG, String.format("checkInstall() check signatures for plugin %s!",
                        newPlugin.packageName));
                Set<Signature> configSignatures = PluginM.getConfigs().getSignatures();
                if (configSignatures.isEmpty()) {
                    throw new IllegalStateException("Signature check is enabled, but no signatures provided in config!");
                }
                Signature[] checkSignatures = configSignatures.toArray(new Signature[configSignatures.size()]);
                boolean isSuc = PackageUtils.checkSignatures(signatures, checkSignatures);
                if (!isSuc) {
                    Logger.e(TAG, String.format("checkInstall() signature check not approved for plugin %s!",
                            newPlugin.packageName));
                    return false;
                }
            }

            // check permission:
            String[] pluginPerms = pkgInfo.requestedPermissions;
            Set<String> stubPermissions = mStubManager.getStubPermissions();
            if (pluginPerms != null && pluginPerms.length > 0 && !stubPermissions.isEmpty()) {
                for (String perm : pluginPerms) {
                    if (!stubPermissions.contains(perm)) {
                        Logger.w(TAG, String.format("checkInstall: no permission %s for plugin %s!",
                                perm, newPlugin.packageName));
                    }
                }
            }

        } catch (Exception e) {
            Logger.e(TAG, "checkInstall() error!", e);
            return false;
        }

        return true;
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
        ProviderInfo providerInfo = PluginManager.getInstance().resolveProviderInfo(auth);
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
            Logger.e(TAG, "resolveActivityInfo() error!", e);
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
            Logger.e(TAG, "resolveServiceInfo() error!", e);
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
            Logger.e(TAG, "resolveProviderInfo() error!", e);
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
            Logger.e(TAG, "getActivityInfo() error!", e);
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
            Logger.e(TAG, "getServiceInfo() error!", e);
        }

        return null;
    }

    @Override
    public ActivityInfo getReceiverInfo(ComponentName componentName, int flags) {
        Logger.d(TAG, "getReceiverInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getReceiverInfo(componentName, flags);
        } catch (Exception e) {
            Logger.e(TAG, "getReceiverInfo() error!", e);
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
            Logger.e(TAG, "getProviderInfo() error!", e);
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

        PluginProcess pluginProcess = mRunningPluginProcess.get(processName);
        StubManager.ProcessInfo stubProcessInfo = mStubManager.getProcessInfo(processName);

        if (stubProcessInfo == null) {
            throw new IllegalStateException("no stub processInfo found for process " + processName);
        }

        if (pluginProcess == null) {
            pluginProcess = new PluginProcess(pid, stubProcessInfo);
            mRunningPluginProcess.put(processName, pluginProcess);
        }

        if (client != null) {
            IPluginClient pluginClient = IPluginClient.Stub.asInterface(client);
            onPluginClientStarted(processName, pluginClient);
        }

    }

    @Override
    public void onApplicationAttached(ApplicationInfo targetInfo, String processName) throws RemoteException {
        Logger.d(TAG, String.format("onApplicationAttached() targetInfo = %s, stubProcessInfo = %s",
                targetInfo, processName));

        PluginProcess pluginProcess = mRunningPluginProcess.get(processName);

        if (pluginProcess == null) {
            throw new IllegalStateException("no PluginProcess found for process " + processName);
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

    @Override
    public List<PluginInfo> getAllRunningPlugins() throws RemoteException {
        return new ArrayList<>(getRunningPluginsMap().values());
    }

    @Override
    public boolean isPluginRunning(String pkgName) throws RemoteException {
        Map<String, PluginInfo> runningPlugins = getRunningPluginsMap();
        return runningPlugins.get(pkgName) != null;
    }

    private Map<String, PluginInfo> getRunningPluginsMap() {
        Map<String, PluginInfo> runningPlugins = new HashMap<>();
        for (PluginProcess pluginProcess : mRunningPluginProcess.values()) {
            List<String> plugins = pluginProcess.getRunningPlugins();
            for (String pluginPkg : plugins) {
                if (!runningPlugins.containsKey(pluginPkg)) {
                    PluginInfo pluginInfo = mInstalledPluginMap.get(pluginPkg);
                    if (pluginInfo == null) {
                        throw new IllegalStateException("running plugin " + pluginPkg + " NOT installed!");
                    }
                    runningPlugins.put(pluginPkg, pluginInfo);
                }
            }
        }
        return runningPlugins;
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
}
