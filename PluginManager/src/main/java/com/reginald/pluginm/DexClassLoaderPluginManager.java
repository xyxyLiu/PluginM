package com.reginald.pluginm;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.util.Log;
import android.util.LogPrinter;

import com.android.common.ActivityThreadCompat;
import com.android.common.ContextCompat;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.parser.IntentMatcher;
import com.reginald.pluginm.parser.PluginPackageParser;
import com.reginald.pluginm.pluginbase.PluginContext;
import com.reginald.pluginm.pluginhost.HostClassLoader;
import com.reginald.pluginm.pluginhost.HostHCallback;
import com.reginald.pluginm.pluginhost.PluginHostProxy;
import com.reginald.pluginm.reflect.FieldUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-1.
 */
public class DexClassLoaderPluginManager {
    private static final String TAG = "DexClassLoderPM";
    private static final String PLUGIN_DEX_FOLDER_NAME = "plugin_dexes_2";
    private static final String PLUGIN_LIB_FOLDER_NAME = "plugin_lib_2";

    private static Map<String, PluginConfig> sPluginConfigs = new HashMap<>();
    private static Map<String, PluginInfo> sInstalledPluginMap = new HashMap<>();
    private static Map<String, PluginPackageParser> sInstalledPkgParser = new HashMap<>();

    public static final String EXTRA_INTENT_TARGET_COMPONENT = "extra.plugin.target.component";

    private ComponentInfo mPendingLoadActivity;

    private static volatile DexClassLoaderPluginManager sInstance;

    private Context mContext;

    private ClassLoader mOriginalClassLoader;
    ;
    private ClassLoader mHostClassLoader;

    public static synchronized DexClassLoaderPluginManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new DexClassLoaderPluginManager(hostContext);
        }

        return sInstance;
    }

    private DexClassLoaderPluginManager(Context hostContext) {
        Log.d(TAG, "DexClassLoaderPluginManager() classLoader = " + this.getClass().getClassLoader());
        Log.d(TAG, "DexClassLoaderPluginManager() parent classLoader = " + this.getClass().getClassLoader().getParent());
        mContext = hostContext;
    }

    private void onAttachBaseContext(Application app) {
        try {
            Field mBaseField = FieldUtils.getField(ContextWrapper.class, "mBase");
            Object mBaseObj = mBaseField.get(app);
            Field mPackageInfoField = FieldUtils.getField(mBaseObj.getClass(), "mPackageInfo");
            Object mPackageInfoObj = mPackageInfoField.get(mBaseObj);
            Field mClassLoaderField = FieldUtils.getField(mPackageInfoObj.getClass(), "mClassLoader");
            ClassLoader loader = (ClassLoader) mClassLoaderField.get(mPackageInfoObj);
            ClassLoader newLoader = new HostClassLoader(this, loader);
            mOriginalClassLoader = loader;
            mHostClassLoader = newLoader;
            FieldUtils.writeField(mClassLoaderField, mPackageInfoObj, newLoader);

            Log.d(TAG, "onAttachBaseContext() replace classloader success! classload = " + newLoader);

            HostHCallback.onInstall(app);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onAttachBaseContext() replace classloader error!");
        }
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
    public void init(Application app, List<PluginConfig> pluginConfigs) {
        initAllPluginConfigs(pluginConfigs);
        onAttachBaseContext(app);
    }


    public boolean install(String pluginPackageName) {
        return install(pluginPackageName, true);
    }


    public boolean install(String pluginPackageName, boolean isStandAlone) {
        try {

            synchronized (sInstalledPluginMap) {
                if (sInstalledPluginMap.get(pluginPackageName) != null) {
                    return true;
                }
            }

            PluginConfig pluginConfig = null;
            synchronized (sPluginConfigs) {
                pluginConfig = sPluginConfigs.get(pluginPackageName);
            }
            if (pluginConfig == null) {
                Log.d(TAG, "install() pluginPackageName = " + pluginPackageName + " error! no config found!");
                return false;
            }


            File apkFile = PackageUtils.copyAssetsApk(mContext, pluginConfig.apkPath);
            File pluginDexPath = mContext.getDir(PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = mContext.getDir(PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginNativeLibPath = new File(pluginLibPath, pluginPackageName);
            ClassLoader parentClassLoader;


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

            PluginInfo pluginInfo = ApkParser.parsePluginInfo(mContext, apkFile.getAbsolutePath());
            pluginInfo.classLoader = dexClassLoader;
            pluginInfo.parentClassLoader = parentClassLoader;
            pluginInfo.apkPath = apkFile.getAbsolutePath();

            synchronized (sInstalledPkgParser) {
                sInstalledPkgParser.put(pluginInfo.packageName, pluginInfo.pkgParser);
            }

            // replace resources
            Resources resources = ResourcesManager.getPluginResources(mContext, apkFile.getAbsolutePath());
            if (resources != null) {
                pluginInfo.resources = resources;
            } else {
                Log.e(TAG, "install() error! resources is null!");
                return false;
            }




            Log.d(TAG, "install() pluginInfo = " + pluginInfo);

            if (pluginInfo == null) {
                Log.e(TAG, "install() error! pluginInfo is null!");
                return false;
            }

            synchronized (sInstalledPluginMap) {
                sInstalledPluginMap.put(pluginInfo.packageName, pluginInfo);
            }

            initPluginApplication(pluginInfo, mContext);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "install() error! exception: " + e);
            e.printStackTrace();
            return false;
        }

    }


    public static PluginInfo getPluginInfo(String packageName) {
        synchronized (sInstalledPluginMap) {
            return sInstalledPluginMap.get(packageName);
        }
    }

    public static PluginInfo getPluginInfo(ClassLoader pluginClassLoader) {
        Log.d(TAG, "getPluginInfo() classloader = " + pluginClassLoader);
        synchronized (sInstalledPluginMap) {
            for (PluginInfo pluginInfo : sInstalledPluginMap.values()) {
                Log.d(TAG, "getPluginInfo() installed classloader = " + pluginInfo.classLoader);
                if (pluginInfo.classLoader == pluginClassLoader) {
                    return pluginInfo;
                }
            }
        }

        return null;
    }

    public Intent getPluginActivityIntent(Intent originIntent) {
        Log.d(TAG, "getPluginActivityIntent() originIntent = " + originIntent);

        // test resolve plugin activity:
        List<ResolveInfo> resolveInfos = null;
        try {
            resolveInfos = IntentMatcher.resolveActivityIntent(mContext, sInstalledPkgParser, originIntent, originIntent.resolveTypeIfNeeded(mContext.getContentResolver()), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, String.format("getPluginActivityIntent() resolveInfos = %s", resolveInfos));

        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return null;
        }

        // choose the first one:
        ResolveInfo finalInfo = resolveInfos.get(0);
        ActivityInfo activityInfo = finalInfo.activityInfo;

        Log.d(TAG, String.format("getPluginActivityIntent() activityInfo = %s", activityInfo));

        // make a proxy activity for it:
        Intent intent = new Intent(originIntent);
        intent.setClassName(mContext, PluginHostProxy.STUB_ACTIVITY);
        intent.putExtra(EXTRA_INTENT_TARGET_COMPONENT, new ComponentName(activityInfo.packageName, activityInfo.name));

        // register class for it:
//        registerActivity(activityInfo);

        return intent;
    }


    public void registerActivity(ActivityInfo activityInfo) {
        mPendingLoadActivity = activityInfo;
    }

    public Intent getPluginServiceIntent(Intent originIntent) {
        Log.d(TAG, "getPluginServiceIntent() originIntent = " + originIntent);

        // test resolve plugin activity:
        List<ResolveInfo> resolveInfos = null;
        try {
            resolveInfos = IntentMatcher.resolveServiceIntent(mContext, sInstalledPkgParser, originIntent, originIntent.resolveTypeIfNeeded(mContext.getContentResolver()), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, String.format("getPluginServiceIntent() resolveInfos = %s", resolveInfos));

        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return null;
        }

        // choose the first one:
        ResolveInfo finalInfo = resolveInfos.get(0);
        ServiceInfo serviceInfo = finalInfo.serviceInfo;

        Log.d(TAG, String.format("getPluginServiceIntent() serviceInfo = %s", serviceInfo));

        // make a proxy activity for it:
        Intent intent = new Intent(originIntent);
        intent.setClassName(mContext, PluginHostProxy.STUB_SERVICE);
        intent.putExtra(EXTRA_INTENT_TARGET_COMPONENT, new ComponentName(serviceInfo.packageName, serviceInfo.name));

        return intent;
    }


    /**
     * only called by classloader
     * 目前只有创建插件Activity时有效
     *
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> findPluginClass(String className) throws ClassNotFoundException {
        Log.d(TAG, "findPluginClass() className = " + className);
        String targetPackage = null;
        String targetClass = className;


        // replace target class
        if (className.startsWith(PluginHostProxy.STUB_ACTIVITY) && mPendingLoadActivity != null) {
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
    public Class<?> loadPluginClass(String packageName, String className) throws ClassNotFoundException {
        Log.d(TAG, "loadPluginClass() className = " + className);
        PluginInfo pluginInfo;
        synchronized (sInstalledPluginMap) {
            pluginInfo = sInstalledPluginMap.get(packageName);
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


    public static Context createPluginContext(PluginInfo pluginInfo, Context baseContext) {
        return new PluginContext(pluginInfo, baseContext);
    }

    public void initPluginApplication(PluginInfo pluginInfo, Context hostContext) {
        Log.d(TAG, "initPluginApplication() pluginInfo = " + pluginInfo + " , hostContext = " + hostContext);
        try {
            Context hostBaseContext = hostContext.createPackageContext(hostContext.getPackageName(), Context.CONTEXT_INCLUDE_CODE);
            pluginInfo.baseContext = new PluginContext(pluginInfo, hostBaseContext);


            initContentProviders(pluginInfo);


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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initContentProviders(PluginInfo pluginInfo) {

        try {
            List<ProviderInfo> srcProviderInfos = pluginInfo.pkgParser.getProviders();
            List<ProviderInfo> providerInfos = new ArrayList<>();
            for (ProviderInfo srcProviderInfo : srcProviderInfos) {
                ProviderInfo providerInfo = new ProviderInfo(srcProviderInfo);
                providerInfo.packageName = pluginInfo.baseContext.getPackageName();
                providerInfo.applicationInfo = new ApplicationInfo(srcProviderInfo.applicationInfo);
                providerInfo.applicationInfo.packageName = pluginInfo.baseContext.getPackageName();
                providerInfos.add(providerInfo);
            }
            ActivityThreadCompat.installContentProviders(pluginInfo.baseContext, providerInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initStaticReceivers(PluginInfo pluginInfo) {
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

    // packages:

    public static ActivityInfo getActivityInfo(ComponentName componentName) {
        Log.d(TAG, "getActivityInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getActivityInfo(componentName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ServiceInfo getServiceInfo(ComponentName componentName) {
        Log.d(TAG, "getServiceInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getServiceInfo(componentName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ActivityInfo getReceiverInfo(ComponentName componentName) {
        Log.d(TAG, "getActivityInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getReceiverInfo(componentName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public static ProviderInfo getProviderInfo(ComponentName componentName) {
        Log.d(TAG, "getProviderInfo() componentName = " + componentName);

        PluginPackageParser pluginPackageParser = getPackageParserForComponent(componentName);
        if (pluginPackageParser == null) {
            return null;
        }

        try {
            return pluginPackageParser.getProviderInfo(componentName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    private static PluginPackageParser getPackageParserForComponent(ComponentName componentName) {
        PluginInfo pluginInfo = getPluginInfo(componentName.getPackageName());
        if (pluginInfo == null) {
            return null;
        }

        return pluginInfo.pkgParser;
    }


}
