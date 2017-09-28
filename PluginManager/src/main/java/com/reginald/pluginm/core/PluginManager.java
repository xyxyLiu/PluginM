package com.reginald.pluginm.core;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
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
import com.reginald.pluginm.comm.PluginLocalManager;
import com.reginald.pluginm.hook.IActivityManagerServiceHook;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.pluginapi.IPluginLocalManager;
import com.reginald.pluginm.pluginapi.PluginHelper;
import com.reginald.pluginm.stub.PluginStubMainProvider;
import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.utils.BinderParcelable;
import com.reginald.pluginm.utils.CommonUtils;
import com.reginald.pluginm.utils.Logger;
import com.reginald.pluginm.utils.ProcessHelper;
import com.reginald.pluginm.utils.ThreadUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public static final String EXTRA_INTENT_STUB_INFO = "extra.plugin.stub.info";
    public static final String EXTRA_INTENT_ORIGINAL_INTENT = "extra.plugin.origin.intent";

    // 本进程已经加载的插件信息：
    private final Object mPluginLoadLock = new Object();
    private final Map<String, PluginInfo> mLoadedPluginMap = new ConcurrentHashMap<>();
    private final Map<String, ClassLoader> mLoadedClassLoaderMap = new ConcurrentHashMap<>();

    // 本进程正在运行的插件组件信息：
    private final Map<Application, ApplicationInfo> mRunningApplicationMap = new WeakHashMap<>();
    private final Map<Activity, Pair<ActivityInfo, ActivityInfo>> mRunningActivityMap = new WeakHashMap<>();
    private final Map<Service, Pair<ServiceInfo, ServiceInfo>> mRunningServiceMap = new WeakHashMap<>();
    private final Map<ContentProvider, Pair<ProviderInfo, ProviderInfo>> mRunningProviderMap = new WeakHashMap<>();

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
        ProcessHelper.init(app);

        boolean isSuc = IActivityManagerServiceHook.init(app);
        Logger.d(TAG, "onAttachBaseContext() replace host IActivityManager, isSuc? " + isSuc);

        Instrumentation newInstrumentation = HostInstrumentation.install(app);
        Logger.d(TAG, "onAttachBaseContext() replace host instrumentation, instrumentation = " + newInstrumentation);

        isSuc = HostHCallback.install(app);
        Logger.d(TAG, "onAttachBaseContext() replace host mH, success? " + isSuc);

        isSuc = PluginClientService.attach(app);
        Logger.d(TAG, "onAttachBaseContext() PluginClientService attach, success? " + isSuc);
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
                                onCoreServiceDied();
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

    private void onCoreServiceDied() {
        Logger.e(TAG, "onCoreServiceDied() receive core process died. dump info = " + toDumpString());
        // 检测到常驻进程退出，插件进程自杀
        if (!mRunningApplicationMap.isEmpty()) {
            Logger.e(TAG, "onCoreServiceDied() exit!");
            System.exit(0);
        }
    }

    private String toDumpString() {
        return String.format("PluginManager [ process = %s(%d), mLoadedPluginMap = %s, mLoadedClassLoaderMap = %s, " +
                        "mRunningApplicationMap = %s. mRunningActivityMap = %s, mRunningServiceMap = %s, mRunningProviderMap = %s]",
                ProcessHelper.sProcessName, ProcessHelper.sPid, mLoadedPluginMap, mLoadedClassLoaderMap,
                mRunningApplicationMap, mRunningActivityMap, mRunningServiceMap, mRunningProviderMap);
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

            pluginInfo = mLoadedPluginMap.get(pluginPackageName);
            if (pluginInfo != null) {
                Logger.d(TAG, "loadPlugin() found loaded pluginInfo " + pluginInfo);
                return pluginInfo;
            }

            synchronized (mPluginLoadLock) {
                pluginInfo = mLoadedPluginMap.get(pluginPackageName);
                if (pluginInfo != null) {
                    Logger.d(TAG, "loadPlugin() found loaded pluginInfo " + pluginInfo);
                    return pluginInfo;
                }


                pluginInfo = getInstalledPluginInfo(packageName);
                Logger.d(TAG, "loadPlugin() getInstalledPluginInfo " + pluginInfo);
                if (pluginInfo == null) {
                    Logger.e(TAG, "loadPlugin() " + pluginPackageName + " NOT installed!");
                    return null;
                }

                ClassLoader parentClassLoader;
                ClassLoader hostClassLoader = mContext.getClassLoader();
                // create classloader
                Logger.d(TAG, "loadPlugin() mContext.getClassLoader() = " + mContext.getClassLoader());

                if (pluginInfo.isStandAlone) {
                    parentClassLoader = hostClassLoader.getParent();
                } else {
                    parentClassLoader = hostClassLoader;
                }
                DexClassLoader pluginClassLoader = new PluginDexClassLoader(
                        pluginInfo.apkPath, pluginInfo.dexDir, pluginInfo.nativeLibDir, parentClassLoader, hostClassLoader);
                Logger.d(TAG, "loadPlugin() pluginClassLoader = " + pluginClassLoader);
                Logger.d(TAG, "loadPlugin() pluginClassLoader's parent = " + pluginClassLoader.getParent());

                pluginInfo.pkgParser = ApkParser.getPackageParser(mContext, pluginInfo.apkPath);
                pluginInfo.applicationInfo = pluginInfo.pkgParser.getApplicationInfo(0);
                pluginInfo.classLoader = pluginClassLoader;
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

                mLoadedPluginMap.put(pluginPackageName, pluginInfo);
                mLoadedClassLoaderMap.put(pluginPackageName, pluginClassLoader);
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

    private boolean initPlugin(final PluginInfo pluginInfo, final Context hostContext) {
        Logger.d(TAG, "initPlugin() pluginInfo = " + pluginInfo);

        final AtomicBoolean isSuc = new AtomicBoolean(false);

        ThreadUtils.ensureRunOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (!initPluginHelper(pluginInfo, hostContext)) {
                    Logger.e(TAG, "initPlugin() initPluginHelper error! ");
                    return;
                }

                if (!loadPluginApplication(pluginInfo, hostContext)) {
                    Logger.e(TAG, "initPlugin() initPluginApplication error! ");
                    return;
                }

                isSuc.set(true);
            }
        });


        return isSuc.get();
    }

    private static boolean initPluginHelper(PluginInfo pluginInfo, Context hostContext) {
        Logger.d(TAG, "initPluginHelper() pluginInfo = " + pluginInfo);
        Class<?> pluginHelperClazz;
        try {
            pluginHelperClazz = pluginInfo.classLoader.loadClass(PluginHelper.class.getName());
            IPluginLocalManager pluginLocalManager = PluginLocalManager.getInstance(hostContext);
            if (pluginHelperClazz != null) {
                Method initMethod = pluginHelperClazz.getDeclaredMethod("init", Object.class);
                initMethod.setAccessible(true);
                initMethod.invoke(null, pluginLocalManager);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG, "initPluginHelper() error!", e);
        }
        return false;
    }

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

            callApplicationOnAttach(pluginInfo.application, applicationInfo);

            loadProviders(pluginInfo);

            pluginInfo.application.onCreate();

            loadStaticReceivers(pluginInfo);
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "loadPluginApplication() error!", e);
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
                String currentProcessName = ProcessHelper.sProcessName;
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

                String currentProcessName = ProcessHelper.sProcessName;
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
    public Class<?> loadPluginClass(String packageName, String className) throws ClassNotFoundException {
        Logger.d(TAG, "loadPluginClass() className = " + className);
        PluginInfo pluginInfo;
        pluginInfo = mLoadedPluginMap.get(packageName);

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

    public PluginInfo getLoadedPluginInfo(String packageName) {
        return mLoadedPluginMap.get(packageName);
    }

    public List<PluginInfo> getLoadedPluginInfos() {
        return new ArrayList<>(mLoadedPluginMap.values());
    }

    public Context createPluginContext(String packageName, Context baseContext) {
        PluginInfo pluginInfo = getLoadedPluginInfo(packageName);
        if (pluginInfo != null) {
            return new PluginContext(pluginInfo, baseContext);
        } else {
            return null;
        }
    }

    public PluginInfo getPluginInfoByClassLoader(ClassLoader pluginClassLoader) {
        for (Map.Entry<String, ClassLoader> entry : mLoadedClassLoaderMap.entrySet()) {
            if (pluginClassLoader == entry.getValue()) {
                return mLoadedPluginMap.get(entry.getKey());
            }
        }
        return null;
    }

    public void callApplicationOnAttach(Application app, ApplicationInfo appInfo) {
        String myProcessName = ProcessHelper.sProcessName;
        Logger.d(TAG, String.format("callApplicationOnAttach() appInfo =  %s, processName = %s", appInfo, myProcessName));

        mRunningApplicationMap.put(app, appInfo);
        onApplicationAttached(appInfo, myProcessName);
    }

    public void callActivityOnCreate(Activity activity, ActivityInfo stubInfo, ActivityInfo targetInfo) {
        if (mRunningActivityMap.containsKey(activity)) {
            throw new IllegalStateException("duplicate activity created! " + activity);
        }
        Logger.d(TAG, String.format("callActivityOnCreate() activity =  %s, stubInfo = %s, targetInfo = %s",
                activity, stubInfo, targetInfo));
        mRunningActivityMap.put(activity, new Pair<>(stubInfo, targetInfo));
        onActivityCreated(stubInfo, targetInfo);
    }

    public void callActivityOnDestory(Activity activity) {
        Pair<ActivityInfo, ActivityInfo> pair = mRunningActivityMap.get(activity);
        if (pair != null) {
            Logger.d(TAG, String.format("callActivityOnDestory() activity =  %s, pair = %s",
                    activity, pair));
            mRunningActivityMap.remove(activity);
            onActivityDestory(pair.first, pair.second);
        }
    }

    public void callServiceOnCreate(Service service, ServiceInfo stubInfo, ServiceInfo targetInfo) {
        if (mRunningServiceMap.containsKey(service)) {
            throw new IllegalStateException("duplicate service created! " + service);
        }

        Logger.d(TAG, String.format("callServiceOnCreate() service =  %s, stubInfo = %s, targetInfo = %s",
                service, stubInfo, targetInfo));

        mRunningServiceMap.put(service, new Pair<>(stubInfo, targetInfo));
        onServiceCreated(stubInfo, targetInfo);
    }

    public void callServiceOnDestory(Service service) {
        Pair<ServiceInfo, ServiceInfo> pair = mRunningServiceMap.get(service);

        if (pair != null) {
            Logger.d(TAG, String.format("callServiceOnDestory() service =  %s, pair = %s",
                    service, pair));
            mRunningServiceMap.remove(service);
            onServiceDestory(pair.first, pair.second);
        }
    }

    public void callProviderOnCreate(ContentProvider provider, ProviderInfo stubInfo, ProviderInfo targetInfo) {
        if (mRunningProviderMap.containsKey(provider)) {
            throw new IllegalStateException("duplicate provider created! " + provider);
        }

        Logger.d(TAG, String.format("callProviderOnCreate() provider =  %s, stubInfo = %s, targetInfo = %s",
                provider, stubInfo, targetInfo));

        mRunningProviderMap.put(provider, new Pair<>(stubInfo, targetInfo));
        onProviderCreated(stubInfo, targetInfo);
    }


    // IPC:

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
                Bundle bundle = mService.getPluginProviderUri(auth);
                Logger.d(TAG, "getPluginProviderUri() bundle = " + bundle);
                if (bundle != null) {
                    bundle.setClassLoader(PluginManager.class.getClassLoader());
                    Pair<Uri, Bundle> pair = new Pair<>((Uri) bundle.getParcelable("uri"),
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

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        if (mService != null) {
            try {
                return mService.queryIntentActivities(intent, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "queryIntentActivities() error!", e);
            }
        }
        return null;
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        if (mService != null) {
            try {
                return mService.queryIntentServices(intent, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "queryIntentServices() error!", e);
            }
        }
        return null;
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        if (mService != null) {
            try {
                return mService.queryBroadcastReceivers(intent, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "queryBroadcastReceivers() error!", e);
            }
        }
        return null;
    }

    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        if (mService != null) {
            try {
                return mService.queryIntentContentProviders(intent, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "queryIntentContentProviders() error!", e);
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

    public PackageInfo getPackageInfo(String packageName, int flags) {
        if (mService != null) {
            try {
                return mService.getPackageInfo(packageName, flags);
            } catch (RemoteException e) {
                Logger.e(TAG, "getPackageInfo() error!", e);
            }
        }
        return null;
    }

    public void onPluginProcessAttached(IBinder client) throws RemoteException {
        if (mService != null) {
            try {
                mService.onPluginProcessAttached(client);
            } catch (RemoteException e) {
                Logger.e(TAG, "onPluginProcessAttached() error!", e);
            }
        }
    }

    public void onApplicationAttached(ApplicationInfo targetInfo, String processName) {
        if (mService != null) {
            try {
                mService.onApplicationAttached(targetInfo, processName);
            } catch (RemoteException e) {
                Logger.e(TAG, "onActivityCreated() error!", e);
            }
        }
    }

    public void onActivityCreated(ActivityInfo stubInfo, ActivityInfo targetInfo) {
        if (mService != null) {
            try {
                mService.onActivityCreated(stubInfo, targetInfo);
            } catch (RemoteException e) {
                Logger.e(TAG, "onActivityCreated() error!", e);
            }
        }
    }

    public void onActivityDestory(ActivityInfo stubInfo, ActivityInfo targetInfo) {
        if (mService != null) {
            try {
                mService.onActivityDestory(stubInfo, targetInfo);
            } catch (RemoteException e) {
                Logger.e(TAG, "onActivityDestory() error!", e);
            }
        }
    }

    public void onServiceCreated(ServiceInfo stubInfo, ServiceInfo targetInfo) {
        if (mService != null) {
            try {
                mService.onServiceCreated(stubInfo, targetInfo);
            } catch (RemoteException e) {
                Logger.e(TAG, "onServiceCreated() error!", e);
            }
        }
    }

    public void onServiceDestory(ServiceInfo stubInfo, ServiceInfo targetInfo) {
        if (mService != null) {
            try {
                mService.onServiceDestory(stubInfo, targetInfo);
            } catch (RemoteException e) {
                Logger.e(TAG, "onServiceDestory() error!", e);
            }
        }
    }

    public void onProviderCreated(ProviderInfo stubInfo, ProviderInfo targetInfo) {
        if (mService != null) {
            try {
                mService.onProviderCreated(stubInfo, targetInfo);
            } catch (RemoteException e) {
                Logger.e(TAG, "onProviderCreated() error!", e);
            }
        }
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

        //TODO 通过调用栈判断返回包名，属投机取巧的做法，后期需要考虑其它处理方法
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Logger.d(TAG, "getPackageNameCompat(): ");
        int i = 0;
        int lookupIndex = -1;
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            Logger.d(TAG, "#  " + stackTraceElement.toString());
            String className = stackTraceElement.getClassName();
            String methodName = stackTraceElement.getMethodName();
            if (i >= lookupIndex && className.endsWith(PluginContext.class.getName()) &&
                    methodName.equals("getPackageName")) {
                lookupIndex = i + 1;
                continue;
            }

            if (i >= lookupIndex && className.equals(ContextWrapper.class.getName()) &&
                    methodName.equals("getPackageName")) {
                lookupIndex = i + 1;
                continue;
            }

            if (i == lookupIndex) {
                if (!className.startsWith("android.")) {
                    pkg = plugin;
                } else if (className.startsWith("android.content.ComponentName") ||
                        methodName.equals("<init>")) {
                    pkg = plugin;
                }

                break;
            }

            i++;
        }

        Logger.d(TAG, "getPackageNameCompat(): return pkg = " + pkg);
        return pkg;
    }
}
