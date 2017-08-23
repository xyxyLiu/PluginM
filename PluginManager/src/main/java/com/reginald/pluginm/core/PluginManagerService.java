package com.reginald.pluginm.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.reginald.pluginm.IPluginManager;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.parser.IntentMatcher;
import com.reginald.pluginm.parser.PluginPackageParser;
import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.utils.Logger;
import com.reginald.pluginm.utils.PackageUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 17-8-22.
 */

public class PluginManagerService extends IPluginManager.Stub {


    private static final String TAG = "PluginManagerService";
    private static volatile PluginManagerService sInstance;

    public static final String PLUGIN_DEX_FOLDER_NAME = "pluginm_dexes";
    public static final String PLUGIN_LIB_FOLDER_NAME = "pluginm_lib";
    private static Map<String, PluginInfo> sInstalledPluginMap = new HashMap<>();
    private static Map<String, PluginPackageParser> sInstalledPkgParser = new HashMap<>();

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
        mStubManager.init();
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
            File apkFile = PackageUtils.copyAssetsApk(mContext, pluginPath);
            if (!apkFile.exists()) {
                Logger.e(TAG, "install() apkFile = " + apkFile.getAbsolutePath() + " NOT exist!");
                return null;
            }

            pluginInfo = ApkParser.parsePluginInfo(mContext, apkFile.getAbsolutePath());
            pluginInfo.apkPath = apkFile.getAbsolutePath();
            pluginInfo.fileSize = apkFile.length();
            pluginInfo.lastModified = apkFile.lastModified();

            synchronized (sInstalledPluginMap) {
                PluginInfo installedPluginInfo = sInstalledPluginMap.get(pluginInfo.packageName);
                if (installedPluginInfo != null) {
                    Logger.d(TAG, "install() found installed pluginInfo " + installedPluginInfo);
                    return installedPluginInfo;
                }
            }

            // create classloader & dexopt
            Logger.d(TAG, "install() mContext.getClassLoader() = " + mContext.getClassLoader());
            ClassLoader parentClassLoader;
            if (isStandAlone) {
                parentClassLoader = mContext.getClassLoader().getParent();
            } else {
                parentClassLoader = mContext.getClassLoader();
            }
            File pluginDexPath = mContext.getDir(PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = mContext.getDir(PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginNativeLibPath = new File(pluginLibPath, pluginInfo.packageName);
            DexClassLoader dexClassLoader = new PluginDexClassLoader(
                    apkFile.getAbsolutePath(), pluginDexPath.getAbsolutePath(), pluginNativeLibPath.getAbsolutePath(), parentClassLoader);
            Logger.d(TAG, "install() dexClassLoader = " + dexClassLoader);
            Logger.d(TAG, "install() dexClassLoader's parent = " + dexClassLoader.getParent());
            pluginInfo.isStandAlone = isStandAlone;
            pluginInfo.classLoader = dexClassLoader;
            pluginInfo.parentClassLoader = parentClassLoader;
            pluginInfo.dexDir = pluginDexPath.getAbsolutePath();

            // install so
            File apkParent = apkFile.getParentFile();
            File tempSoDir = new File(apkParent, "temp");
            Set<String> soList = PackageUtils.unZipSo(apkFile, tempSoDir);
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

            synchronized (sInstalledPkgParser) {
                Logger.d(TAG, "install() sInstalledPkgParser add " + pluginInfo.packageName);
                sInstalledPkgParser.put(pluginInfo.packageName, pluginInfo.pkgParser);
            }

            synchronized (sInstalledPluginMap) {
                Logger.d(TAG, "install() sInstalledPluginMap add " + pluginInfo.packageName);
                sInstalledPluginMap.put(pluginInfo.packageName, pluginInfo);
            }

            return pluginInfo;
        } catch (Exception e) {
            Logger.e(TAG, "install() error! exception: " + e);
            e.printStackTrace();
            return null;
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

        // make a proxy activity for it:
        ActivityInfo stubActivityInfo = mStubManager.selectStubActivity(activityInfo);
        Logger.d(TAG, String.format("getPluginActivityIntent() activityInfo = %s -> stub = %s",
                activityInfo, stubActivityInfo));

        if (stubActivityInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubActivityInfo.packageName, stubActivityInfo.name);
            intent.putExtra(PluginManager.EXTRA_INTENT_TARGET_ACTIVITYINFO, activityInfo);

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

        ServiceInfo stubServiceInfo = mStubManager.selectStubService(serviceInfo);
        Logger.d(TAG, String.format("getPluginServiceIntent() serviceInfo = %s -> stub = %s",
                serviceInfo, stubServiceInfo));
        if (stubServiceInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubServiceInfo.packageName, stubServiceInfo.name);
            intent.putExtra(PluginManager.EXTRA_INTENT_TARGET_SERVICEINFO, serviceInfo);

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

        ProviderInfo stubProvider = mStubManager.selectStubProvider(providerInfo);
        Logger.d(TAG, String.format("getPluginProviderUri() providerInfo = %s -> stub = %s",
                providerInfo, stubProvider));

        if (stubProvider != null) {
            Bundle resultBundle = new Bundle();
            Bundle providerBundle = new Bundle();
            providerBundle.putParcelable(PluginManager.EXTRA_INTENT_TARGET_PROVIDERINFO, providerInfo);
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
            List<ResolveInfo> resolveInfos = IntentMatcher.resolveActivityIntent(mContext, sInstalledPkgParser, intent, intent.resolveTypeIfNeeded(mContext.getContentResolver()), 0);
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
            List<ResolveInfo> resolveInfos = IntentMatcher.resolveServiceIntent(mContext, sInstalledPkgParser, intent, intent.resolveTypeIfNeeded(mContext.getContentResolver()), 0);
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
        Logger.d(TAG, "resolveProviderInfo() name = " + name + " ,sInstalledPkgParser = " + sInstalledPkgParser);
        try {
            for (PluginPackageParser pluginPackageParser : sInstalledPkgParser.values()) {
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
    public PluginInfo getInstalledPluginInfo(String packageName) {
        synchronized (sInstalledPluginMap) {
            return sInstalledPluginMap.get(packageName);
        }
    }

    private PluginPackageParser getPackageParserForComponent(ComponentName componentName) {
        PluginInfo pluginInfo = getInstalledPluginInfo(componentName.getPackageName());
        if (pluginInfo == null) {
            return null;
        }

        return pluginInfo.pkgParser;
    }

    public static Intent handleOriginalIntent(Intent origIntent) {
        Bundle extras = origIntent.getExtras();
        origIntent.replaceExtras((Bundle) null);
        Intent newIntent = new Intent(origIntent);
        newIntent.putExtra(PluginManager.EXTRA_INTENT_ORIGINAL_EXTRAS, extras);
        return newIntent;
    }

    public static Intent recoverOriginalIntent(Intent pluginIntent, ComponentName componentName, ClassLoader classLoader) {
        Intent origIntent = new Intent(pluginIntent);
        origIntent.setComponent(componentName);
        Bundle origExtras = pluginIntent.getBundleExtra(PluginManager.EXTRA_INTENT_ORIGINAL_EXTRAS);
        origIntent.replaceExtras(origExtras);
        origIntent.setExtrasClassLoader(classLoader);
        return origIntent;
    }
}
