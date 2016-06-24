package com.example.multidexmodeplugin;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.util.Log;

import com.example.multidexmodeplugin.packages.ApkParser;
import com.example.multidexmodeplugin.pluginhost.HostClassLoader;
import com.example.multidexmodeplugin.pluginhost.PluginHostProxy;
import com.example.multidexmodeplugin.reflect.FieldUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-1.
 */
public class DexClassLoaderPluginManager {
    private static final String TAG = "DexClassLoderPM";
    private static final String PLUGIN_DEX_FOLDER_NAME = "plugin_dexes_2";
    private static final String PLUGIN_LIB_FOLDER_NAME = "plugin_lib_2";

    private static List<ClassLoader> sClassLoaderList = new ArrayList<>();
    private static HashMap<String, PluginInfo> sInstalledPluginMap = new HashMap<>();

    private String mPendingLoadActivity;

    private static Object sClassLoaderProxy;

    private static volatile DexClassLoaderPluginManager sInstance;

    private Context mContext;

    public static synchronized DexClassLoaderPluginManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new DexClassLoaderPluginManager(hostContext);
        }

        return sInstance;
    }

    private DexClassLoaderPluginManager(Context hostContext) {
        mContext = hostContext;
    }

    public boolean install(String pluginApkName) {
        return install(pluginApkName, true);
    }


    public void onAttachBaseContext(Application app) {
        try {
            Field mBaseField = FieldUtils.getField(ContextWrapper.class, "mBase");
            Object mBaseObj = mBaseField.get(app);
            Field mPackageInfoField = FieldUtils.getField(mBaseObj.getClass(), "mPackageInfo");
            Object mPackageInfoObj = mPackageInfoField.get(mBaseObj);
            Field mClassLoaderField = FieldUtils.getField(mPackageInfoObj.getClass(), "mClassLoader");
            ClassLoader loader = (ClassLoader) mClassLoaderField.get(mPackageInfoObj);
            ClassLoader newLoader = new HostClassLoader(this, loader);

            FieldUtils.writeField(mClassLoaderField, mPackageInfoObj, newLoader);

            Log.d(TAG, "onAttachBaseContext() replace classloader success!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onAttachBaseContext() replace classloader error!");
        }
    }

    public boolean install(String pluginApkName, boolean isStandAlone) {
        try {

            synchronized (sInstalledPluginMap) {
                if (sInstalledPluginMap.get(pluginApkName) != null) {
                    return true;
                }
            }

            File apkFile = AssetsManager.copyAssetsApk(mContext, pluginApkName);
            File pluginDexPath = mContext.getDir(PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = mContext.getDir(PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            ClassLoader parentClassLoader;
            if (isStandAlone) {
                parentClassLoader = mContext.getClassLoader().getParent();
            } else {
                parentClassLoader = mContext.getClassLoader();
            }
            DexClassLoader dexClassLoader = new DexClassLoader(apkFile.getAbsolutePath(), pluginDexPath.getAbsolutePath(), pluginLibPath.getAbsolutePath(), parentClassLoader);


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

            synchronized (sClassLoaderList) {
                sClassLoaderList.add(dexClassLoader);
            }
            synchronized (sInstalledPluginMap) {
                sInstalledPluginMap.put(pluginInfo.packageName, pluginInfo);
            }


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


    public void startPluginActivity(String pluginPkg, String clazz, Intent originalIntent) {
        if (originalIntent == null) {
            originalIntent = new Intent();
        }
        originalIntent.setClassName(pluginPkg, clazz);
        startPluginActivity(originalIntent);
    }

    public void startPluginActivity(Intent originalIntent) {
        Log.d(TAG, String.format("startPluginActivity() intent = %s", originalIntent));
        if (originalIntent == null) {
            return;
        }

        // resolve plugin activity:
        String pluginPackageName = originalIntent.getComponent().getPackageName();
        String className = originalIntent.getComponent().getClassName();
        ActivityInfo activityInfo = getActivityInfo(pluginPackageName, className);
        Log.d(TAG, String.format("startPluginActivity() resolve ok! activityInfo = %s", activityInfo));

        // make a proxy activity for it:
        originalIntent.setClassName(mContext, PluginHostProxy.PROXY_ACTIVITY);

        // register class for it:
        registerActivity(activityInfo);

        Log.d(TAG, String.format("startPluginActivity() new intent = %s", originalIntent));
        mContext.startActivity(originalIntent);
    }

    public void registerActivity(ActivityInfo activityInfo) {
        mPendingLoadActivity = activityInfo.name;
    }



    public Class<?> findPluginClass(String className) throws ClassNotFoundException {
        Log.d(TAG,"findPluginClass() className = " + className);
        if (className.startsWith(PluginHostProxy.PROXY_PREFIX)) {
            String targetClass = mPendingLoadActivity;
            return loadPluginClass(targetClass);
        }

        throw new ClassNotFoundException(className);
    }

    public Class<?> loadPluginClass(String className) throws ClassNotFoundException {
        Log.d(TAG,"loadPluginClass() className = " + className);

        synchronized (sClassLoaderList) {
            for (ClassLoader classLoader : sClassLoaderList) {
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz != null) {
                        Log.d(TAG,"findPluginClass() success!");
                        return clazz;
                    }
                } catch (Exception e) {
//                    e.printStackTrace();
                }

            }
        }

        throw new ClassNotFoundException(className);
    }












    // packages:

    public static ActivityInfo getActivityInfo(String packageName, String clazz) {

        PluginInfo pluginInfo = getPluginInfo(packageName);
        if (pluginInfo == null) {
            return null;
        }
        PackageInfo packageInfo = pluginInfo.pkgInfo;
        if (packageInfo == null || packageInfo.activities == null) {
            return null;
        }

        ActivityInfo[] activityInfos = packageInfo.activities;

        for (ActivityInfo info : activityInfos) {
            if (info.name.equals(clazz)) {
                return info;
            }
        }

        return null;
    }



}
