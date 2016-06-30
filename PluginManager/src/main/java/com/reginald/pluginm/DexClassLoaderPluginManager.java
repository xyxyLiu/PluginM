package com.reginald.pluginm;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.util.Log;

import com.android.common.ContextCompat;
import com.reginald.pluginm.packages.ApkParser;
import com.reginald.pluginm.pluginbase.BasePluginApplication;
import com.reginald.pluginm.pluginbase.PluginContext;
import com.reginald.pluginm.pluginhost.HostClassLoader;
import com.reginald.pluginm.pluginhost.HostHCallback;
import com.reginald.pluginm.pluginhost.PluginHostProxy;
import com.reginald.pluginm.reflect.FieldUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


            File apkFile = AssetsManager.copyAssetsApk(mContext, pluginConfig.apkPath);
            File pluginDexPath = mContext.getDir(PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = mContext.getDir(PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            ClassLoader parentClassLoader;

            Log.d(TAG, "install() mContext.getClassLoader() = " + mContext.getClassLoader());

            if (isStandAlone) {
                parentClassLoader = mContext.getClassLoader().getParent();
            } else {
                parentClassLoader = mContext.getClassLoader();
            }
            DexClassLoader dexClassLoader = new PluginDexClassLoader(
                    apkFile.getAbsolutePath(), pluginDexPath.getAbsolutePath(), pluginLibPath.getAbsolutePath(), parentClassLoader);
            Log.d(TAG, "install() dexClassLoader = " + dexClassLoader);
            Log.d(TAG, "install() dexClassLoader's parent = " + dexClassLoader.getParent());

            PluginInfo pluginInfo = ApkParser.parsePackage(mContext, apkFile.getAbsolutePath());
            pluginInfo.classLoader = dexClassLoader;
            pluginInfo.parentClassLoader = parentClassLoader;
            pluginInfo.apkPath = apkFile.getAbsolutePath();


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

    public Intent getPluginActivityIntent(Intent originIntent, String pluginPkg, String clazz) {
        Log.d(TAG, "getPluginActivityIntent() classloader = " + this.getClass().getClassLoader());
        Log.d(TAG, String.format("getPluginActivityIntent() pluginPkg = %s, clazz = %s", pluginPkg, clazz));

        // test resolve plugin activity:
        ActivityInfo activityInfo = getActivityInfo(pluginPkg, clazz);
        Log.d(TAG, String.format("getPluginActivityIntent() resolved activityInfo = %s", activityInfo));

//        if (activityInfo == null) {
//            return null;
//        }

        // make a proxy activity for it:
        Intent intent = new Intent(originIntent);
        intent.setClassName(mContext, PluginHostProxy.STUB_ACTIVITY);
        intent.putExtra(EXTRA_INTENT_TARGET_COMPONENT, new ComponentName(pluginPkg, clazz));

        // register class for it:
//        registerActivity(activityInfo);

        return intent;
    }

    public void registerActivity(ActivityInfo activityInfo) {
        mPendingLoadActivity = activityInfo;
    }

    public Intent getPluginServiceIntent(Intent originIntent, String pluginPkg, String clazz) {
        Log.d(TAG, "getPluginServiceIntent() classloader = " + this.getClass().getClassLoader());
        Log.d(TAG, String.format("getPluginServiceIntent() pluginPkg = %s, clazz = %s", pluginPkg, clazz));

        // test resolve plugin activity:
        ServiceInfo serviceInfo = getServiceInfo(pluginPkg, clazz);
        Log.d(TAG, String.format("getPluginServiceIntent() resolved activityInfo = %s", serviceInfo));

//        if (serviceInfo == null) {
//            return null;
//        }

        // make a proxy activity for it:
        Intent intent = new Intent(originIntent);
        intent.setClassName(mContext, PluginHostProxy.STUB_SERVICE);
        intent.putExtra(EXTRA_INTENT_TARGET_COMPONENT, new ComponentName(pluginPkg, clazz));

        return intent;
    }



    public Class<?> findPluginClass(String className) throws ClassNotFoundException {
        Log.d(TAG, "findPluginClass() className = " + className);
        String targetPackage = null;
        String targetClass = className;

        // replace target class
        if (className.startsWith(PluginHostProxy.STUB_ACTIVITY)) {
            targetPackage = mPendingLoadActivity.packageName;
            targetClass = mPendingLoadActivity.name;
        }
        Log.d(TAG, "findPluginClass() targetPackage = " + targetPackage + " ,targetClass = " + className);

//        try {
        return loadPluginClass(targetPackage, targetClass);
//        } catch (ClassNotFoundException e) {
//            if (className.startsWith(PluginHostProxy.STUB_ACTIVITY)) {
//                Log.d(TAG, "findPluginClass() return " + VoidStubPluginActivity.class);
//                return VoidStubPluginActivity.class;
//            } else {
//                throw e;
//            }
//        }
    }

    public Class<?> loadPluginClass(String packageName, String className) throws ClassNotFoundException {
        Log.d(TAG, "loadPluginClass() className = " + className);
        PluginInfo pluginInfo;
        synchronized (sInstalledPluginMap) {
            pluginInfo = sInstalledPluginMap.get(packageName);
        }

        if (pluginInfo != null) {
            ClassLoader pluginClassLoader = pluginInfo.classLoader;
            try {
                Class<?> clazz = pluginClassLoader.loadClass(className);
                if (clazz != null) {
                    Log.d(TAG, "loadPluginClass() success!");
                    return clazz;
                }
            } catch (Exception e) {
//                    e.printStackTrace();
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

            ApplicationInfo applicationInfo = pluginInfo.pkgInfo.applicationInfo;
            Log.d(TAG, "initPluginApplication() applicationInfo.name = " + applicationInfo.name);

            if (applicationInfo.className == null) {
                applicationInfo.className = BasePluginApplication.class.getName();
            }

            Class pluginAppClass = pluginInfo.classLoader.loadClass(applicationInfo.className);
            pluginInfo.application = (Application) pluginAppClass.newInstance();
            Method attachMethod = android.app.Application.class
                    .getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(pluginInfo.application, pluginInfo.baseContext);
            ContextCompat.setOuterContext(pluginInfo.baseContext, pluginInfo.application);
            pluginInfo.application.onCreate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // packages:

    public static ActivityInfo getActivityInfo(String packageName, String clazz) {
        Log.d(TAG, "getActivityInfo() packageName = " + packageName + " , clazz = " + clazz);
        PluginInfo pluginInfo = getPluginInfo(packageName);
        if (pluginInfo == null) {
            return null;
        }

        Log.d(TAG, "getActivityInfo() pluginInfo = " + pluginInfo);

        PackageInfo packageInfo = pluginInfo.pkgInfo;
        if (packageInfo == null || packageInfo.activities == null) {
            return null;
        }

        ActivityInfo[] activityInfos = packageInfo.activities;

        for (ActivityInfo info : activityInfos) {
            Log.d(TAG, "getActivityInfo() check = " + info.name);
            if (info.name.equals(clazz)) {
                return info;
            }
        }

        return null;
    }

    public static ServiceInfo getServiceInfo(String packageName, String clazz) {
        Log.d(TAG, "getServiceInfo() packageName = " + packageName + " , clazz = " + clazz);
        PluginInfo pluginInfo = getPluginInfo(packageName);
        if (pluginInfo == null) {
            return null;
        }

        Log.d(TAG, "getServiceInfo() pluginInfo = " + pluginInfo);

        PackageInfo packageInfo = pluginInfo.pkgInfo;
        if (packageInfo == null || packageInfo.services == null) {
            return null;
        }

        ServiceInfo[] serviceInfos = packageInfo.services;

        for (ServiceInfo info : serviceInfos) {
            Log.d(TAG, "getServiceInfo() check = " + info.name);
            if (info.name.equals(clazz)) {
                return info;
            }
        }

        return null;
    }


}
