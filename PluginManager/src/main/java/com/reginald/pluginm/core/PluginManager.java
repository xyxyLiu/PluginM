package com.reginald.pluginm.core;

import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Pair;

import com.android.common.ContextCompat;
import com.reginald.pluginm.IPluginManager;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.hook.IActivityManagerServiceHook;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.stub.PluginStubMainProvider;
import com.reginald.pluginm.utils.BinderParcelable;
import com.reginald.pluginm.utils.CommonUtils;
import com.reginald.pluginm.utils.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-11-1.
 */
public class PluginManager {
    private static final String TAG = "PluginManager";
    private static volatile PluginManager sInstance;

    public static final String EXTRA_INTENT_TARGET_ACTIVITYINFO = "extra.plugin.target.activityinfo";
    public static final String EXTRA_INTENT_TARGET_SERVICEINFO = "extra.plugin.target.serviceinfo";
    public static final String EXTRA_INTENT_TARGET_PROVIDERINFO = "extra.plugin.target.providerinfo";
    public static final String EXTRA_INTENT_ORIGINAL_INTENT = "extra.plugin.origin.intent";
    private static Map<String, PluginInfo> sLoadedPluginMap = new HashMap<>();

    private Context mContext;
    private volatile IPluginManager mService;

    public static synchronized PluginManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginManager(hostContext);
        }

        return sInstance;
    }

    /**
     * must be called in {@link Application#attachBaseContext(Context)}
     * @param app
     */
    public static void init(Application app) {
        onAttachBaseContext(app);
    }

    private static void onAttachBaseContext(Application app) {
        boolean isSuc = IActivityManagerServiceHook.init(app);
        Logger.d(TAG, "onAttachBaseContext() replace host IActivityManager, isSuc? " + isSuc);

        Instrumentation newInstrumentation = HostInstrumentation.install(app);
        Logger.d(TAG, "onAttachBaseContext() replace host instrumentation, instrumentation = " + newInstrumentation);

        isSuc = HostHCallback.install(app);
        Logger.d(TAG, "onAttachBaseContext() replace host mH, success? " + isSuc);
    }

    private PluginManager(Context hostContext) {
        Context appContext = hostContext.getApplicationContext();
        mContext = appContext != null ? appContext : hostContext;
        initCoreService();
    }

    private void initCoreService() {
        try {
            final ContentResolver contentResolver = mContext.getContentResolver();
            final Bundle bundle = contentResolver.call(PluginManagerServiceProvider.URI,
                    PluginManagerServiceProvider.METHOD_GET_CORE_SERVICE, null, null);
            if (bundle != null) {
                bundle.setClassLoader(PluginManager.class.getClassLoader());
                BinderParcelable bp = bundle.getParcelable(PluginManagerServiceProvider.KEY_SERVICE);
                if (bp != null) {
                    IBinder iBinder = bp.iBinder;
                    if (iBinder != null) {
                        iBinder.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                initCoreService();
                            }
                        }, 0);
                    }
                    mService = IPluginManager.Stub.asInterface(iBinder);
                    Logger.d(TAG, "initCoreService() success! mService = " + mService);
                }
            }
        } catch (Throwable e) {
            Logger.e(TAG, "initCoreService() error!", e);
        }
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

            pluginInfo = getInstalledPluginInfo(packageName);
            Logger.d(TAG, "loadPlugin() getInstalledPluginInfo " + pluginInfo);
            if (pluginInfo == null) {
                Logger.e(TAG, "loadPlugin() " + pluginPackageName + " NOT installed!");
                return null;
            }

            ClassLoader parentClassLoader;

            // create classloader
            Logger.d(TAG, "loadPlugin() mContext.getClassLoader() = " + mContext.getClassLoader());

            if (pluginInfo.isStandAlone) {
                parentClassLoader = mContext.getClassLoader().getParent();
            } else {
                parentClassLoader = mContext.getClassLoader();
            }
            DexClassLoader dexClassLoader = new PluginDexClassLoader(
                    pluginInfo.apkPath, pluginInfo.dexDir, pluginInfo.nativeLibDir, parentClassLoader);
            Logger.d(TAG, "loadPlugin() dexClassLoader = " + dexClassLoader);
            Logger.d(TAG, "loadPlugin() dexClassLoader's parent = " + dexClassLoader.getParent());

            pluginInfo.pkgParser = ApkParser.getPackageParser(mContext, pluginInfo.apkPath);
            pluginInfo.applicationInfo = pluginInfo.pkgParser.getApplicationInfo(0);
            pluginInfo.classLoader = dexClassLoader;
            pluginInfo.parentClassLoader = parentClassLoader;

            PluginPackageManager pluginPackageManager = new PluginPackageManager(mContext, mContext.getPackageManager());
            Logger.d(TAG, "loadPlugin() pluginPackageManager = " + pluginPackageManager);
            pluginInfo.packageManager = pluginPackageManager;

            // replace resources
            Resources resources = ResourcesManager.getPluginResources(mContext, pluginInfo.apkPath);
            if (resources != null) {
                pluginInfo.resources = resources;
            } else {
                Logger.e(TAG, "loadPlugin() error! resources is null!");
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

    private boolean initPlugin(PluginInfo pluginInfo, Context hostContext) {
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

    private boolean loadPluginApplication(PluginInfo pluginInfo, Context hostContext) {
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

            loadProviders(pluginInfo);

            pluginInfo.application.onCreate();

            loadStaticReceivers(pluginInfo);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadProviders(PluginInfo pluginInfo) {
        List<ProviderInfo> providerInfos = null;
        try {
            providerInfos = pluginInfo.pkgParser.getProviders();
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<ProviderInfo> targetProviderInfos = new ArrayList<>();
        if (providerInfos != null) {
            for (ProviderInfo providerInfo : providerInfos) {
                String currentProcessName = CommonUtils.getProcessName(mContext, Process.myPid());
                String stubProcessName = selectStubProcessName(providerInfo.processName, providerInfo.packageName);
                if (!TextUtils.isEmpty(currentProcessName) && currentProcessName.equals(stubProcessName)) {
                    targetProviderInfos.add(providerInfo);
                }
            }
        }

        Logger.d(TAG, "loadProviders() targetProviderInfos = " + targetProviderInfos);
        PluginStubMainProvider.loadProviders(pluginInfo, targetProviderInfos);
    }

    private void loadStaticReceivers(PluginInfo pluginInfo) {
        Map<ActivityInfo, List<IntentFilter>> receiverIntentFilters = pluginInfo.pkgParser.getReceiverIntentFilter();

        if (receiverIntentFilters != null) {
            for (Map.Entry<ActivityInfo, List<IntentFilter>> entry : receiverIntentFilters.entrySet()) {
                ActivityInfo receiverInfo = entry.getKey();
                List<IntentFilter> intentFilters = entry.getValue();

                String currentProcessName = CommonUtils.getProcessName(mContext, Process.myPid());
                String stubProcessName = selectStubProcessName(receiverInfo.processName, receiverInfo.packageName);

                if (!TextUtils.isEmpty(currentProcessName) && currentProcessName.equals(stubProcessName)) {
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

    // public api
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

    public PluginInfo getPluginInfo(String packageName) {
        synchronized (sLoadedPluginMap) {
            return sLoadedPluginMap.get(packageName);
        }
    }

    public List<PluginInfo> getPluginInfos() {
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

    public PluginInfo installPlugin(String apkPath) {
        if (mService != null) {
            try {
                return mService.install(apkPath);
            } catch (RemoteException e) {
                Logger.e(TAG, "installPlugin() error!", e);
            }
        }
        return null;
    }

    public Intent getPluginActivityIntent(Intent originIntent) {
        if (mService != null) {
            try {
                return mService.getPluginActivityIntent(originIntent);
            } catch (RemoteException e) {
                Logger.e(TAG, "getPluginActivityIntent() error!", e);
            }
        }
        return null;
    }

    public Intent getPluginServiceIntent(Intent originIntent) {
        if (mService != null) {
            try {
                return mService.getPluginServiceIntent(originIntent);
            } catch (RemoteException e) {
                Logger.e(TAG, "getPluginServiceIntent() error!", e);
            }
        }
        return null;
    }

    public Pair<Uri, Bundle> getPluginProviderUri(String auth) {
        if (mService != null) {
            try {
                Bundle bundle =  mService.getPluginProviderUri(auth);
                Logger.d(TAG, "getPluginProviderUri() bundle = " + bundle);
                if (bundle != null) {
                    bundle.setClassLoader(PluginManager.class.getClassLoader());
                    Pair<Uri, Bundle> pair = new Pair<>((Uri)bundle.getParcelable("uri"),
                            (Bundle) bundle.getBundle("bundle"));
                    return pair;
                }
            } catch (RemoteException e) {
                Logger.e(TAG, "getPluginProviderUri() error!", e);
            }
        }
        return null;
    }

    public String selectStubProcessName(String processName, String pkgName) {
        if (mService != null) {
            try {
                return mService.selectStubProcessName(processName, pkgName);
            } catch (RemoteException e) {
                Logger.e(TAG, "selectStubProcessName() error!", e);
            }
        }
        return null;
    }

    public ActivityInfo resolveActivityInfo(Intent intent, int flags) {
        if (mService != null) {
            try {
                return mService.resolveActivityInfo(intent, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "resolveActivityInfo() error!", e);
            }
        }
        return null;
    }

    public ServiceInfo resolveServiceInfo(Intent intent, int flags) {
        if (mService != null) {
            try {
                return mService.resolveServiceInfo(intent, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "resolveServiceInfo() error!", e);
            }
        }
        return null;
    }

    public ProviderInfo resolveProviderInfo(String name) {
        if (mService != null) {
            try {
                return mService.resolveProviderInfo(name);
            } catch (RemoteException e) {
                Logger.e(TAG, "resolveProviderInfo() error!", e);
            }
        }
        return null;
    }

    public ActivityInfo getActivityInfo(ComponentName componentName, int flags) {
        if (mService != null) {
            try {
                return mService.getActivityInfo(componentName, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "getActivityInfo() error!", e);
            }
        }
        return null;
    }

    public ServiceInfo getServiceInfo(ComponentName componentName, int flags) {
        if (mService != null) {
            try {
                return mService.getServiceInfo(componentName, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "getServiceInfo() error!", e);
            }
        }
        return null;
    }

    public ActivityInfo getReceiverInfo(ComponentName componentName, int flags) {
        if (mService != null) {
            try {
                return mService.getReceiverInfo(componentName, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "getReceiverInfo() error!", e);
            }
        }
        return null;
    }

    public ProviderInfo getProviderInfo(ComponentName componentName, int flags) {
        if (mService != null) {
            try {
                return mService.getProviderInfo(componentName, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "getProviderInfo() error!", e);
            }
        }
        return null;
    }

    public PluginInfo getInstalledPluginInfo(String packageName) {
        if (mService != null) {
            try {
                return mService.getInstalledPluginInfo(packageName);
            } catch (RemoteException e) {
                Logger.e(TAG, "getInstalledPluginInfo() error!", e);
            }
        }
        return null;
    }

    public List<PluginInfo> getAllInstalledPlugins() {
        if (mService != null) {
            try {
                return mService.getAllInstalledPlugins();
            } catch (RemoteException e) {
                Logger.e(TAG, "getInstalledPluginInfo() error!", e);
            }
        }
        return null;
    }

    public static String getPackageNameCompat(String plugin, String host) {
        String pkg = host;

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

            if (i == lookupIndex) {
                if (!className.startsWith("android.")) {
                    pkg = plugin;
                    break;
                }

                if (className.startsWith("android.content.ComponentName") ||
                        methodName.equals("<init>")) {
                    pkg = plugin;
                    break;
                }
            }

            i++;
        }

        Logger.d(TAG, "getPackageNameCompat(): return pkg = " + pkg);
        return pkg;
    }
}
