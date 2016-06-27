package com.reginald.pluginm.pluginbase;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.reginald.pluginm.PluginInfo;

/**
 * Created by lxy on 16-6-28.
 */
public class PluginContext extends ContextThemeWrapper {

    private static final String TAG = "PluginActivityContext";

    private PluginInfo mPluginInfo;
    private String mApkPath;
    private AssetManager mAssetManager;
    private Resources mResources;
    private ClassLoader mClassLoader;

    public PluginContext(PluginInfo pluginInfo, Context baseContext) {
        // test theme
        super(baseContext, pluginInfo.pkgInfo.applicationInfo.theme);
        Log.d(TAG, "PluginActivityContext() pluginInfo = " + pluginInfo);
        mPluginInfo = pluginInfo;
        mApkPath = pluginInfo.apkPath;
        mAssetManager = pluginInfo.resources.getAssets();
        mResources = pluginInfo.resources;
        mClassLoader = pluginInfo.classLoader;
    }

    public String getPackageResourcePath() {
        return mApkPath;
    }

    public String getPackageCodePath() {
        return mApkPath;
    }

    public String getPackageName() {
        return super.getPackageName();
    }

    public PackageManager getPackageManager() {
        return super.getPackageManager();
    }

    public ClassLoader getClassLoader() {
        return mClassLoader != null ? mClassLoader : ClassLoader.getSystemClassLoader();
    }

    public AssetManager getAssets() {
        return mAssetManager;
    }

    public Resources getResources() {
        return mResources;
    }

    public Context getApplicationContext() {
        return mPluginInfo.application;
    }

    public ApplicationInfo getApplicationInfo() {
        return mPluginInfo.pkgInfo.applicationInfo;
    }
}
