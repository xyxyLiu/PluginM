package com.reginald.pluginm;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.parser.IntentMatcher;
import com.reginald.pluginm.parser.PluginPackageParser;
import com.reginald.pluginm.stub.PluginHostProxy;
import com.reginald.pluginm.stub.StubManager;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-1.
 */
public class PluginManagerNative {
    public static final String PLUGIN_DEX_FOLDER_NAME = "plugin_dexes_2";
    public static final String PLUGIN_LIB_FOLDER_NAME = "plugin_lib_2";
    public static final String EXTRA_INTENT_TARGET_ACTIVITYINFO = "extra.plugin.target.activityinfo";
    public static final String EXTRA_INTENT_STUB_ACTIVITYINFO = "extra.plugin.stub.activityinfo";
    public static final String EXTRA_INTENT_TARGET_SERVICEINFO = "extra.plugin.target.serviceinfo";
    public static final String EXTRA_INTENT_TARGET_PROVIDERINFO = "extra.plugin.target.providerinfo";
    public static final String EXTRA_INTENT_ORIGINAL_EXTRAS = "extra.plugin.origin.extras";
    private static final String TAG = "PluginManagerNative";
    private static Map<String, PluginConfig> sPluginConfigs = new HashMap<>();
    private static Map<String, PluginInfo> sInstalledPluginMap = new HashMap<>();
    private static Map<String, PluginPackageParser> sInstalledPkgParser = new HashMap<>();
    private static volatile PluginManagerNative sInstance;

    private Context mContext;

    private PluginManagerNative(Context hostContext) {
        Log.d(TAG, "DexClassLoaderPluginManager() classLoader = " + this.getClass().getClassLoader());
        Log.d(TAG, "DexClassLoaderPluginManager() parent classLoader = " + this.getClass().getClassLoader().getParent());
        mContext = hostContext;
        // test
        StubManager.getInstance(hostContext).init();
    }

    public static synchronized PluginManagerNative getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginManagerNative(hostContext);
        }

        return sInstance;
    }

    private static void onAttachBaseContext(Application app) {
        ClassLoader newHostLoader = HostClassLoader.onInstall(app);
        Log.d(TAG, "onAttachBaseContext() replace host classloader, classloader = " + newHostLoader);

        Instrumentation newInstrumentation = HostInstrumentation.onInstall(app);
        Log.d(TAG, "onAttachBaseContext() replace host instrumentation, instrumentation = " + newInstrumentation);

        boolean isSuc = HostHCallback.onInstall(app);
        Log.d(TAG, "onAttachBaseContext() replace host mH, success? " + isSuc);
    }

    //test
    private static void initAllPluginConfigs(List<PluginConfig> pluginConfigs) {

        for (PluginConfig pluginConfig : pluginConfigs) {
            sPluginConfigs.put(pluginConfig.packageName, pluginConfig);
        }
    }

    /**
     * must be called in {@link Application#attachBaseContext(Context)}
     * @param app
     * @param pluginConfigs
     */
    public static void init(Application app, List<PluginConfig> pluginConfigs) {
        initAllPluginConfigs(pluginConfigs);
        onAttachBaseContext(app);
    }


    // public api

    public PluginInfo install(String pluginPackageName) {
        return install(pluginPackageName, true);
    }

    public ProviderInfo resolveProviderInfo(String name) {
        Log.d(TAG, "resolveProviderInfo() name = " + name + " ,sInstalledPkgParser = " + sInstalledPkgParser);
        try {
            for (PluginPackageParser pluginPackageParser : sInstalledPkgParser.values()) {
                List<ProviderInfo> providerInfos = pluginPackageParser.getProviders();
                for (ProviderInfo providerInfo : providerInfos) {
                    Log.d(TAG, "resolveProviderInfo() check providerInfo " + providerInfo);
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

    public Intent getPluginActivityIntent(Intent originIntent) {
        Log.d(TAG, "getPluginActivityIntent() originIntent = " + originIntent);

        ActivityInfo activityInfo = resolveActivityInfo(originIntent, 0);

        if (activityInfo == null) {
            return null;
        }

        Log.d(TAG, String.format("getPluginActivityIntent() activityInfo = %s", activityInfo));

        // make a proxy activity for it:
        ActivityInfo stubActivityInfo = StubManager.getInstance(mContext).getStubActivity(activityInfo);
        if (stubActivityInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubActivityInfo.packageName, stubActivityInfo.name);
            intent.putExtra(EXTRA_INTENT_TARGET_ACTIVITYINFO, activityInfo);
            intent.putExtra(EXTRA_INTENT_STUB_ACTIVITYINFO, stubActivityInfo);

            // register class for it:
//        registerActivity(activityInfo);
            Log.d(TAG, String.format("getPluginActivityIntent() stubActivityInfo = %s", stubActivityInfo));
            return intent;
        } else {
            return null;
        }
    }

    public Intent getPluginServiceIntent(Intent originIntent) {
        Log.d(TAG, "getPluginServiceIntent() originIntent = " + originIntent);

        ServiceInfo serviceInfo = resolveServiceInfo(originIntent, 0);

        if (serviceInfo == null) {
            return null;
        }

        Log.d(TAG, String.format("getPluginServiceIntent() serviceInfo = %s", serviceInfo));

        // make a proxy activity for it:
        Intent intent = handleOriginalIntent(originIntent);
        intent.setClassName(mContext, PluginHostProxy.STUB_SERVICE);
        intent.putExtra(EXTRA_INTENT_TARGET_SERVICEINFO, serviceInfo);

        return intent;
    }

    public static Intent handleOriginalIntent(Intent origIntent) {
        Bundle extras = origIntent.getExtras();
        origIntent.replaceExtras((Bundle) null);
        Intent newIntent = new Intent(origIntent);
        newIntent.putExtra(EXTRA_INTENT_ORIGINAL_EXTRAS, extras);
        return newIntent;
    }

    public static Intent recoverOriginalIntent(Intent pluginIntent, ComponentName componentName, ClassLoader classLoader) {
        Intent origIntent = new Intent(pluginIntent);
        origIntent.setComponent(componentName);
        Bundle origExtras = pluginIntent.getBundleExtra(EXTRA_INTENT_ORIGINAL_EXTRAS);
        origIntent.replaceExtras(origExtras);
        origIntent.setExtrasClassLoader(classLoader);
        return origIntent;
    }


    private PluginInfo install(String pluginPackageName, boolean isStandAlone) {
        try {
            PluginInfo pluginInfo = null;
            synchronized (sInstalledPluginMap) {
                pluginInfo = sInstalledPluginMap.get(pluginPackageName);
                if (pluginInfo != null) {
                    return pluginInfo;
                }
            }

            PluginConfig pluginConfig = null;
            synchronized (sPluginConfigs) {
                pluginConfig = sPluginConfigs.get(pluginPackageName);
            }
            if (pluginConfig == null) {
                Log.d(TAG, "install() pluginPackageName = " + pluginPackageName + " error! no config found!");
                return null;
            }


            File apkFile = PackageUtils.copyAssetsApk(mContext, pluginConfig.apkPath);
            File pluginDexPath = mContext.getDir(PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = mContext.getDir(PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginNativeLibPath = new File(pluginLibPath, pluginPackageName);

            if (!apkFile.exists()) {
                Log.e(TAG, "install() apkFile = " + apkFile.getAbsolutePath() + " NOT exist!");
                return null;
            }

            ClassLoader parentClassLoader;

            // create classloader
            Log.d(TAG, "install() mContext.getClassLoader() = " + mContext.getClassLoader());

            if (isStandAlone) {
                parentClassLoader = mContext.getClassLoader().getParent();
            } else {
                parentClassLoader = mContext.getClassLoader();
            }
            DexClassLoader dexClassLoader = new PluginDexClassLoader(
                    apkFile.getAbsolutePath(), pluginDexPath.getAbsolutePath(), pluginNativeLibPath.getAbsolutePath(), parentClassLoader);
            Log.d(TAG, "install() dexClassLoader = " + dexClassLoader);
            Log.d(TAG, "install() dexClassLoader's parent = " + dexClassLoader.getParent());

            pluginInfo = ApkParser.parsePluginInfo(mContext, apkFile.getAbsolutePath());
            pluginInfo.classLoader = dexClassLoader;
            pluginInfo.parentClassLoader = parentClassLoader;
            pluginInfo.apkPath = apkFile.getAbsolutePath();
            pluginInfo.fileSize = apkFile.length();


            PluginPackageManager pluginPackageManager = new PluginPackageManager(mContext, mContext.getPackageManager());
            Log.d(TAG, "install() pluginPackageManager = " + pluginPackageManager);
            pluginInfo.packageManager = pluginPackageManager;

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

            // replace resources
            Resources resources = ResourcesManager.getPluginResources(mContext, apkFile.getAbsolutePath());
            if (resources != null) {
                pluginInfo.resources = resources;
            } else {
                Log.e(TAG, "install() error! resources is null!");
                return null;
            }


            Log.d(TAG, "install() pluginInfo = " + pluginInfo);

            if (pluginInfo == null) {
                Log.e(TAG, "install() error! pluginInfo is null!");
                return null;
            }

            synchronized (sInstalledPkgParser) {
                Log.d(TAG, "install() sInstalledPkgParser add " + pluginInfo.packageName);
                sInstalledPkgParser.put(pluginInfo.packageName, pluginInfo.pkgParser);
            }

            synchronized (sInstalledPluginMap) {
                Log.d(TAG, "install() sInstalledPluginMap add " + pluginInfo.packageName);
                sInstalledPluginMap.put(pluginInfo.packageName, pluginInfo);
            }

            return pluginInfo;
        } catch (Exception e) {
            Log.e(TAG, "install() error! exception: " + e);
            e.printStackTrace();
            return null;
        }

    }

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

    public ActivityInfo getActivityInfo(ComponentName componentName, int flags) {
        Log.d(TAG, "getActivityInfo() componentName = " + componentName);

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

    public ServiceInfo getServiceInfo(ComponentName componentName, int flags) {
        Log.d(TAG, "getServiceInfo() componentName = " + componentName);

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

    public ActivityInfo getReceiverInfo(ComponentName componentName, int flags) {
        Log.d(TAG, "getActivityInfo() componentName = " + componentName);

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

    public ProviderInfo getProviderInfo(ComponentName componentName, int flags) {
        Log.d(TAG, "getProviderInfo() componentName = " + componentName);

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

    private PluginPackageParser getPackageParserForComponent(ComponentName componentName) {
        PluginInfo pluginInfo = getPluginInfo(componentName.getPackageName());
        if (pluginInfo == null) {
            return null;
        }

        return pluginInfo.pkgParser;
    }

    public PluginInfo getPluginInfo(String packageName) {
        synchronized (sInstalledPluginMap) {
            return sInstalledPluginMap.get(packageName);
        }
    }
}
