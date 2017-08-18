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
import android.text.TextUtils;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Pair;

import com.android.common.ContextCompat;
import com.reginald.pluginm.hook.IActivityManagerServiceHook;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.parser.IntentMatcher;
import com.reginald.pluginm.parser.PluginPackageParser;
import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.stub.Stubs;

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

    //TODO 考虑修改实现方式
    private ActivityInfo mPendingLoadActivity;
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

    public PluginInfo loadPlugin(ApplicationInfo applicationInfo) {
        Log.d(TAG, "loadPlugin() applicationInfo = " + applicationInfo);
        try {
            String pluginPackageName = applicationInfo.packageName;
            PluginInfo pluginInfo = null;
            Log.d(TAG, "loadPlugin() pluginPackageName = " + pluginPackageName);
            synchronized (sLoadedPluginMap) {
                pluginInfo = sLoadedPluginMap.get(pluginPackageName);
                if (pluginInfo != null) {
                    Log.d(TAG, "loadPlugin() found loaded pluginInfo " + pluginInfo);
                    return pluginInfo;
                }
            }

            pluginInfo = install(applicationInfo.packageName);

            Log.d(TAG, "loadPlugin() pluginInfo = " + pluginInfo);

            if (pluginInfo == null) {
                Log.e(TAG, "loadPlugin() install error with " + pluginInfo);
                return null;
            }

            synchronized (sLoadedPluginMap) {
                sLoadedPluginMap.put(pluginInfo.packageName, pluginInfo);
            }


            if (!initPlugin(pluginInfo, mContext)) {
                Log.e(TAG, "loadPlugin() initPlugin error!");
                return null;
            }


            Log.d(TAG, "loadPlugin() ok!");
            return pluginInfo;
        } catch (Exception e) {
            Log.e(TAG, "loadPlugin() error! exception: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private static boolean initPlugin(PluginInfo pluginInfo, Context hostContext) {
        Log.d(TAG, "initPlugin() pluginInfo = " + pluginInfo);
//        if (!initPluginHelper(pluginInfo, hostContext)) {
//            Log.e(TAG, "initPlugin() initPluginHelper error! ");
//            return false;
//        }

        if (!initPluginApplication(pluginInfo, hostContext)) {
            Log.e(TAG, "initPlugin() initPluginApplication error! ");
            return false;
        }

        return true;
    }

//    private static boolean initPluginHelper(PluginInfo pluginInfo, Context hostContext) {
//        Log.d(TAG, "initPluginHelper() pluginInfo = " + pluginInfo);
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

    private static boolean initPluginApplication(PluginInfo pluginInfo, Context hostContext) {
        Log.d(TAG, "initPluginApplication() pluginInfo = " + pluginInfo + " , hostContext = " + hostContext);
        try {
            Context hostBaseContext = hostContext.createPackageContext(hostContext.getPackageName(), Context.CONTEXT_INCLUDE_CODE);
            pluginInfo.baseContext = new PluginContext(pluginInfo, hostBaseContext);

            ApplicationInfo applicationInfo = pluginInfo.pkgParser.getApplicationInfo(0);
            Log.d(TAG, "initPluginApplication() applicationInfo.name = " + applicationInfo.name);

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
            pluginInfo.application.onCreate();

            initStaticReceivers(pluginInfo);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void initStaticReceivers(PluginInfo pluginInfo) {
        Map<ActivityInfo, List<IntentFilter>> receiverIntentFilters = pluginInfo.pkgParser.getReceiverIntentFilter();

        if (receiverIntentFilters != null) {
            for (Map.Entry<ActivityInfo, List<IntentFilter>> entry : receiverIntentFilters.entrySet()) {
                ActivityInfo receiverInfo = entry.getKey();
                List<IntentFilter> intentFilters = entry.getValue();

                try {
                    BroadcastReceiver receiver = (BroadcastReceiver) pluginInfo.classLoader.loadClass(receiverInfo.name).newInstance();
                    for (IntentFilter filter : intentFilters) {
                        pluginInfo.application.registerReceiver(receiver, filter);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                //test;
                Log.d(TAG, "initStaticReceivers() receiverInfo = " + receiverInfo);
                int i = 1;
                for (IntentFilter intentFilter : intentFilters) {
                    Log.d(TAG, "initStaticReceivers() IntentFilter No." + i++ + " :");
                    intentFilter.dump(new LogPrinter(Log.DEBUG, "initStaticReceivers() "), "");
                    Log.d(TAG, "initStaticReceivers() \n");
                }

            }
        }
    }


    public void registerActivity(ActivityInfo activityInfo) {
        mPendingLoadActivity = activityInfo;
    }

    /**
     * only called by classloader
     * 目前只有创建插件Activity时有效
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> findPluginClass(String className) throws ClassNotFoundException {
        Log.d(TAG, "findPluginClass() className = " + className + " ,mPendingLoadActivity = " + mPendingLoadActivity);
        String targetPackage = null;
        String targetClass = className;


        // replace target class
        if (className.startsWith(Stubs.Activity.class.getName()) && mPendingLoadActivity != null) {
            Log.d(TAG, "findPluginClass() mPendingLoadActivity = " + mPendingLoadActivity);
            targetPackage = mPendingLoadActivity.packageName;
            targetClass = mPendingLoadActivity.name;
        }

        Log.d(TAG, "findPluginClass() targetPackage = " + targetPackage + " ,targetClass = " + className);

        if (targetPackage != null && targetClass != null) {
            return loadPluginClass(targetPackage, targetClass);
        }

        throw new ClassNotFoundException(className);
    }

    /**
     * find plugin class by plugin packageName and className
     * @param packageName
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> loadPluginClass(String packageName, String className) throws ClassNotFoundException {
        Log.d(TAG, "loadPluginClass() className = " + className);
        PluginInfo pluginInfo;
        synchronized (sLoadedPluginMap) {
            pluginInfo = sLoadedPluginMap.get(packageName);
        }

        if (pluginInfo != null) {
            ClassLoader pluginClassLoader = pluginInfo.classLoader;
            Log.d(TAG, "loadPluginClass() pluginClassLoader = " + pluginClassLoader);
            try {
                Class<?> clazz = pluginClassLoader.loadClass(className);
                if (clazz != null) {
                    Log.d(TAG, "loadPluginClass() className = " + className + " success!");
                    return clazz;
                }
            } catch (Exception e) {
//                    e.printStackTrace();
                Log.e(TAG, "loadPluginClass() className = " + className + " fail!");
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
        ClassLoader newHostLoader = HostClassLoader.install(app);
        Log.d(TAG, "onAttachBaseContext() replace host classloader, classloader = " + newHostLoader);

        boolean isSuc = IActivityManagerServiceHook.init(app);
        Log.d(TAG, "onAttachBaseContext() replace host IActivityManager, isSuc? " + isSuc);

        Instrumentation newInstrumentation = HostInstrumentation.install(app);
        Log.d(TAG, "onAttachBaseContext() replace host instrumentation, instrumentation = " + newInstrumentation);

        isSuc = HostHCallback.install(app);
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
        ActivityInfo activityInfo = resolveActivityInfo(originIntent, 0);
        Log.d(TAG, String.format("getPluginActivityIntent() originIntent = %s, resolved activityInfo = %s",
                originIntent, activityInfo));

        if (activityInfo == null) {
            return null;
        }

        // make a proxy activity for it:
        ActivityInfo stubActivityInfo = StubManager.getInstance(mContext).selectStubActivity(activityInfo);
        Log.d(TAG, String.format("getPluginActivityIntent() activityInfo = %s -> stub = %s",
                activityInfo, stubActivityInfo));

        if (stubActivityInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubActivityInfo.packageName, stubActivityInfo.name);
            intent.putExtra(EXTRA_INTENT_TARGET_ACTIVITYINFO, activityInfo);

            // register class for it:
//        registerActivity(activityInfo);
            Log.d(TAG, String.format("getPluginActivityIntent() stubActivityInfo = %s", stubActivityInfo));
            return intent;
        } else {
            return null;
        }
    }

    public Intent getPluginServiceIntent(Intent originIntent) {
        ServiceInfo serviceInfo = resolveServiceInfo(originIntent, 0);
        Log.d(TAG, String.format("getPluginServiceIntent() originIntent = %s, resolved serviceInfo = %s",
                originIntent, serviceInfo));

        if (serviceInfo == null) {
            return null;
        }

        ServiceInfo stubServiceInfo = StubManager.getInstance(mContext).selectStubService(serviceInfo);
        Log.d(TAG, String.format("getPluginServiceIntent() serviceInfo = %s -> stub = %s",
                serviceInfo, stubServiceInfo));
        if (stubServiceInfo != null) {
            Intent intent = handleOriginalIntent(originIntent);
            intent.setClassName(stubServiceInfo.packageName, stubServiceInfo.name);
            intent.putExtra(EXTRA_INTENT_TARGET_SERVICEINFO, serviceInfo);

            Log.d(TAG, String.format("getPluginServiceIntent() stubServiceInfo = %s", stubServiceInfo));
            return intent;
        } else {
            return null;
        }
    }

    public Pair<Uri, Bundle> getPluginProviderUri(String auth) {
        ProviderInfo providerInfo = PluginManager.getInstance(mContext).resolveProviderInfo(auth);
        Log.d(TAG, "getPluginProviderUri() auth = " + auth + ",resolved providerInfo = " + providerInfo);

        if (providerInfo == null) {
            return null;
        }

        ProviderInfo stubProvider = StubManager.getInstance(mContext).selectStubProvider(providerInfo);
        Log.d(TAG, String.format("getPluginProviderUri() providerInfo = %s -> stub = %s",
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
        Log.d(TAG, "getPackageNameCompat(): ");
        int i = 0;
        int lookupIndex = -1;
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            Log.d(TAG, "#  " + stackTraceElement.toString());
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

        Log.d(TAG, "getPackageNameCompat(): return pkg = " + pkg);
        return pkg;
    }
}
