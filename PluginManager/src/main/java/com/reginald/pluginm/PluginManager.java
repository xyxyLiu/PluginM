package com.reginald.pluginm;

import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Pair;

import com.android.common.ContextCompat;
import com.reginald.pluginm.hook.IActivityManagerServiceHook;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.parser.IntentMatcher;
import com.reginald.pluginm.parser.PluginPackageParser;
import com.reginald.pluginm.stub.PluginStubMainProvider;
import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.utils.CommonUtils;
import com.reginald.pluginm.utils.Logger;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-11-1.
 */
public class PluginManager {
    private static final String TAG = "PluginManager";
    private static volatile PluginManager sInstance;

    public static final String PLUGIN_DEX_FOLDER_NAME = "plugin_dexes_2";
    public static final String PLUGIN_LIB_FOLDER_NAME = "plugin_lib_2";
    public static final String EXTRA_INTENT_TARGET_ACTIVITYINFO = "extra.plugin.target.activityinfo";
    public static final String EXTRA_INTENT_STUB_ACTIVITYINFO = "extra.plugin.stub.activityinfo";
    public static final String EXTRA_INTENT_TARGET_SERVICEINFO = "extra.plugin.target.serviceinfo";
    public static final String EXTRA_INTENT_TARGET_PROVIDERINFO = "extra.plugin.target.providerinfo";
    public static final String EXTRA_INTENT_ORIGINAL_EXTRAS = "extra.plugin.origin.extras";
    private static Map<String, PluginConfig> sPluginConfigs = new HashMap<>();
    private static Map<String, PluginInfo> sInstalledPluginMap = new HashMap<>();
    private static Map<String, PluginPackageParser> sInstalledPkgParser = new HashMap<>();
    private static Map<String, PluginInfo> sLoadedPluginMap = new HashMap<>();

    private Context mContext;

    public static synchronized PluginManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginManager(hostContext);
        }

        return sInstance;
    }

    private PluginManager(Context hostContext) {
        Context appContext = hostContext.getApplicationContext();
        mContext = appContext != null ? appContext : hostContext;
        // test
        StubManager.getInstance(hostContext).init();
    }

    public Context getHostContext() {
        return mContext;
    }

    public PluginInfo loadPlugin(String packageName) {
        Logger.d(TAG, "loadPlugin() packageName = " + packageName);
        try {
            String pluginPackageName = packageName;
            PluginInfo pluginInfo = null;
            Logger.d(TAG, "loadPlugin() pluginPackageName = " + pluginPackageName);
            synchronized (sLoadedPluginMap) {
                pluginInfo = sLoadedPluginMap.get(pluginPackageName);
                if (pluginInfo != null) {
                    Logger.d(TAG, "loadPlugin() found loaded pluginInfo " + pluginInfo);
                    return pluginInfo;
                }
            }

            pluginInfo = install(packageName);

            Logger.d(TAG, "loadPlugin() pluginInfo = " + pluginInfo);

            if (pluginInfo == null) {
                Logger.e(TAG, "loadPlugin() install error with " + pluginInfo);
                return null;
            }

            synchronized (sLoadedPluginMap) {
                sLoadedPluginMap.put(pluginInfo.packageName, pluginInfo);
            }


            if (!initPlugin(pluginInfo, mContext)) {
                Logger.e(TAG, "loadPlugin() initPlugin error!");
                return null;
            }


            Logger.d(TAG, "loadPlugin() ok!");
            return pluginInfo;
        } catch (Exception e) {
            Logger.e(TAG, "loadPlugin() error! exception: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private static boolean initPlugin(PluginInfo pluginInfo, Context hostContext) {
        Logger.d(TAG, "initPlugin() pluginInfo = " + pluginInfo);
//        if (!initPluginHelper(pluginInfo, hostContext)) {
//            Logger.e(TAG, "initPlugin() initPluginHelper error! ");
//            return false;
//        }

        if (!loadPluginApplication(pluginInfo, hostContext)) {
            Logger.e(TAG, "initPlugin() initPluginApplication error! ");
            return false;
        }

        return true;
    }

//    private static boolean initPluginHelper(PluginInfo pluginInfo, Context hostContext) {
//        Logger.d(TAG, "initPluginHelper() pluginInfo = " + pluginInfo);
//        Class<?> pluginHelperClazz;
//        try {
//            pluginHelperClazz = pluginInfo.classLoader.loadClass(PluginHelper.class.getName());
//            ILocalPluginManager iPluginManager = PluginManager.getInstance(hostContext);
//            if (pluginHelperClazz != null) {
//                MethodUtils.invokeStaticMethod(pluginHelperClazz, "onInit",
//                        new Object[]{iPluginManager, pluginInfo.packageName},
//                        new Class[]{Object.class, String.class});
//                return true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    private static boolean loadPluginApplication(PluginInfo pluginInfo, Context hostContext) {
        Logger.d(TAG, "loadPluginApplication() pluginInfo = " + pluginInfo + " , hostContext = " + hostContext);
        try {
            Context hostBaseContext = hostContext.createPackageContext(hostContext.getPackageName(), Context.CONTEXT_INCLUDE_CODE);
            pluginInfo.baseContext = new PluginContext(pluginInfo, hostBaseContext);

            ApplicationInfo applicationInfo = pluginInfo.pkgParser.getApplicationInfo(0);
            Logger.d(TAG, "loadPluginApplication() applicationInfo.name = " + applicationInfo.name);

            if (applicationInfo.className == null) {
                applicationInfo.className = Application.class.getName();//BasePluginApplication.class.getName();
            }

            Class pluginAppClass = pluginInfo.classLoader.loadClass(applicationInfo.className);
            pluginInfo.application = (Application) pluginAppClass.newInstance();
            Method attachMethod = android.app.Application.class
                    .getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(pluginInfo.application, pluginInfo.baseContext);
            ContextCompat.setOuterContext(pluginInfo.baseContext, pluginInfo.application);

            installProviders(hostContext, pluginInfo);

            pluginInfo.application.onCreate();

            loadStaticReceivers(hostContext, pluginInfo);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void installProviders(Context hostContext, PluginInfo pluginInfo) {
        PluginStubMainProvider.installProviders(hostContext, pluginInfo);
    }

    private static void loadStaticReceivers(Context hostContext, PluginInfo pluginInfo) {
        Map<ActivityInfo, List<IntentFilter>> receiverIntentFilters = pluginInfo.pkgParser.getReceiverIntentFilter();

        if (receiverIntentFilters != null) {
            for (Map.Entry<ActivityInfo, List<IntentFilter>> entry : receiverIntentFilters.entrySet()) {
                ActivityInfo receiverInfo = entry.getKey();
                List<IntentFilter> intentFilters = entry.getValue();

                String currentProcessName = CommonUtils.getProcessName(hostContext, Process.myPid());
                StubManager.ProcessInfo processInfo = StubManager.getInstance(hostContext).selectStubProcess(receiverInfo);

                if (!TextUtils.isEmpty(currentProcessName) && currentProcessName.equals(processInfo.processName)) {
                    try {
                        Logger.d(TAG, "loadStaticReceivers() receiverInfo = " + receiverInfo);
                        BroadcastReceiver receiver = (BroadcastReceiver) pluginInfo.classLoader.loadClass(receiverInfo.name).newInstance();
                        int i = 1;
                        for (IntentFilter filter : intentFilters) {
                            pluginInfo.application.registerReceiver(receiver, filter);
                            Logger.d(TAG, "loadStaticReceivers() IntentFilter No." + i++ + " :");
                            filter.dump(new LogPrinter(Log.DEBUG, "loadStaticReceivers() "), "");
                            Logger.d(TAG, "loadStaticReceivers() \n");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * find plugin class by plugin packageName and className
     * @param packageName
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> loadPluginClass(String packageName, String className) throws ClassNotFoundException {
        Logger.d(TAG, "loadPluginClass() className = " + className);
        PluginInfo pluginInfo;
        synchronized (sLoadedPluginMap) {
            pluginInfo = sLoadedPluginMap.get(packageName);
        }

        if (pluginInfo != null) {
            ClassLoader pluginClassLoader = pluginInfo.classLoader;
            Logger.d(TAG, "loadPluginClass() pluginClassLoader = " + pluginClassLoader);
            try {
                Class<?> clazz = pluginClassLoader.loadClass(className);
                if (clazz != null) {
                    Logger.d(TAG, "loadPluginClass() className = " + className + " success!");
                    return clazz;
                }
            } catch (Exception e) {
//                    e.printStackTrace();
                Logger.e(TAG, "loadPluginClass() className = " + className + " fail!");
            }
        }
        throw new ClassNotFoundException(className);
    }

    public static PluginInfo getPluginInfo(String packageName) {
        synchronized (sLoadedPluginMap) {
            return sLoadedPluginMap.get(packageName);
        }
    }

    public static List<PluginInfo> getPluginInfos() {
        synchronized (sLoadedPluginMap) {
            return new ArrayList<>(sLoadedPluginMap.values());
        }
    }

    public Context createPluginContext(String packageName, Context baseContext) {
        PluginInfo pluginInfo = getPluginInfo(packageName);
        if (pluginInfo != null) {
            return new PluginContext(pluginInfo, baseContext);
        } else {
            return null;
        }
    }


    private static void onAttachBaseContext(Application app) {
        boolean isSuc = IActivityManagerServiceHook.init(app);
        Logger.d(TAG, "onAttachBaseContext() replace host IActivityManager, isSuc? " + isSuc);

        Instrumentation newInstrumentation = HostInstrumentation.install(app);
        Logger.d(TAG, "onAttachBaseContext() replace host instrumentation, instrumentation = " + newInstrumentation);

        isSuc = HostHCallback.install(app);
        Logger.d(TAG, "onAttachBaseContext() replace host mH, success? " + isSuc);
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


    public Intent getPluginActivityIntent(Intent originIntent) {
        ActivityInfo activityInfo = resolveActivityInfo(originIntent, 0);
        Logger.d(TAG, String.format("getPluginActivityIntent() originIntent = %s, resolved activityInfo = %s",
                originIntent, activityInfo));

        if (activityInfo == null) {
            return null;
        }

        // make a proxy activity for it:
        ActivityInfo stubActivityInfo = StubManager.getInstance(mContext).selectStubActivity(activityInfo);
        Logger.d(TAG, String.format("getPluginActivityIntent() activityInfo = %s -> stub = %s",
                activityInfo, stubActivityInfo));

        if (stubActivityInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubActivityInfo.packageName, stubActivityInfo.name);
            intent.putExtra(EXTRA_INTENT_TARGET_ACTIVITYINFO, activityInfo);

            // register class for it:
//        registerActivity(activityInfo);
            Logger.d(TAG, String.format("getPluginActivityIntent() stubActivityInfo = %s", stubActivityInfo));
            return intent;
        } else {
            return null;
        }
    }

    public Intent getPluginServiceIntent(Intent originIntent) {
        ServiceInfo serviceInfo = resolveServiceInfo(originIntent, 0);
        Logger.d(TAG, String.format("getPluginServiceIntent() originIntent = %s, resolved serviceInfo = %s",
                originIntent, serviceInfo));

        if (serviceInfo == null) {
            return null;
        }

        ServiceInfo stubServiceInfo = StubManager.getInstance(mContext).selectStubService(serviceInfo);
        Logger.d(TAG, String.format("getPluginServiceIntent() serviceInfo = %s -> stub = %s",
                serviceInfo, stubServiceInfo));
        if (stubServiceInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubServiceInfo.packageName, stubServiceInfo.name);
            intent.putExtra(EXTRA_INTENT_TARGET_SERVICEINFO, serviceInfo);

            Logger.d(TAG, String.format("getPluginServiceIntent() stubServiceInfo = %s", stubServiceInfo));
            return intent;
        } else {
            return null;
        }
    }

    public Pair<Uri, Bundle> getPluginProviderUri(String auth) {
        ProviderInfo providerInfo = PluginManager.getInstance(mContext).resolveProviderInfo(auth);
        Logger.d(TAG, "getPluginProviderUri() auth = " + auth + ",resolved providerInfo = " + providerInfo);

        if (providerInfo == null) {
            return null;
        }

        ProviderInfo stubProvider = StubManager.getInstance(mContext).selectStubProvider(providerInfo);
        Logger.d(TAG, String.format("getPluginProviderUri() providerInfo = %s -> stub = %s",
                providerInfo, stubProvider));

        if (stubProvider != null) {
            Bundle providerBundle = new Bundle();
            providerBundle.putParcelable(PluginManager.EXTRA_INTENT_TARGET_PROVIDERINFO, providerInfo);
            return new Pair<>(Uri.parse("content://" + stubProvider.authority), providerBundle);
        } else {
            return null;
        }
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
            Logger.d(TAG, "install() pluginPackageName = " + pluginPackageName);

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
                Logger.d(TAG, "install() pluginPackageName = " + pluginPackageName + " error! no config found!");
                return null;
            }


            File apkFile = PackageUtils.copyAssetsApk(mContext, pluginConfig.apkPath);
            File pluginDexPath = mContext.getDir(PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = mContext.getDir(PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginNativeLibPath = new File(pluginLibPath, pluginPackageName);

            if (!apkFile.exists()) {
                Logger.e(TAG, "install() apkFile = " + apkFile.getAbsolutePath() + " NOT exist!");
                return null;
            }

            ClassLoader parentClassLoader;

            // create classloader
            Logger.d(TAG, "install() mContext.getClassLoader() = " + mContext.getClassLoader());

            if (isStandAlone) {
                parentClassLoader = mContext.getClassLoader().getParent();
            } else {
                parentClassLoader = mContext.getClassLoader();
            }
            DexClassLoader dexClassLoader = new PluginDexClassLoader(
                    apkFile.getAbsolutePath(), pluginDexPath.getAbsolutePath(), pluginNativeLibPath.getAbsolutePath(), parentClassLoader);
            Logger.d(TAG, "install() dexClassLoader = " + dexClassLoader);
            Logger.d(TAG, "install() dexClassLoader's parent = " + dexClassLoader.getParent());

            pluginInfo = ApkParser.parsePluginInfo(mContext, apkFile.getAbsolutePath());
            pluginInfo.classLoader = dexClassLoader;
            pluginInfo.parentClassLoader = parentClassLoader;
            pluginInfo.dexDir = pluginDexPath.getAbsolutePath();
            pluginInfo.apkPath = apkFile.getAbsolutePath();
            pluginInfo.fileSize = apkFile.length();
            pluginInfo.lastModified = apkFile.lastModified();

            PluginPackageManager pluginPackageManager = new PluginPackageManager(mContext, mContext.getPackageManager());
            Logger.d(TAG, "install() pluginPackageManager = " + pluginPackageManager);
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
                Logger.e(TAG, "install() error! resources is null!");
                return null;
            }


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

    private PluginPackageParser getPackageParserForComponent(ComponentName componentName) {
        PluginInfo pluginInfo = getInstalledPluginInfo(componentName.getPackageName());
        if (pluginInfo == null) {
            return null;
        }

        return pluginInfo.pkgParser;
    }

    public PluginInfo getInstalledPluginInfo(String packageName) {
        synchronized (sInstalledPluginMap) {
            return sInstalledPluginMap.get(packageName);
        }
    }

    public static String getPackageNameCompat(String plugin, String host) {
        String pkg = plugin;

        // 通过调用栈判断返回包名，属投机取巧的做法，后期需要考虑其它处理方法
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Logger.d(TAG, "getPackageNameCompat(): ");
        int i = 0;
        int lookupIndex = -1;
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            Logger.d(TAG, "#  " + stackTraceElement.toString());
            String className = stackTraceElement.getClassName();
            String methodName = stackTraceElement.getMethodName();
            if (i >= lookupIndex && className.equals("android.content.ContextWrapper") &&
                    methodName.equals("getPackageName")) {
                lookupIndex = i + 1;
            }

            if (lookupIndex == i) {
                if (className.startsWith("android.widget") ||
                        className.startsWith("android.view") ||
                        className.startsWith("android.app")) {
                    pkg = host;
                }
            }

            if (className.startsWith("android.app.Notification")) {
                pkg = host;
            }

            i++;
        }

        Logger.d(TAG, "getPackageNameCompat(): return pkg = " + pkg);
        return pkg;
    }
}
