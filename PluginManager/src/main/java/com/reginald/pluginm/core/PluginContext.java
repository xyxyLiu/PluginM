package com.reginald.pluginm.core;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.PluginNotFoundException;
import com.reginald.pluginm.stub.PluginContentResolver;
import com.reginald.pluginm.stub.StubManager;
import com.reginald.pluginm.utils.Logger;

/**
 * Created by lxy on 16-6-28.
 */
public class PluginContext extends ContextThemeWrapper {

    private static final String TAG = "PluginContext";

    private Context mBaseContext;
    private PluginInfo mPluginInfo;
    private String mApkPath;
    private AssetManager mAssetManager;
    private PackageManager mPackageManager;
    private Resources mResources;
    private ClassLoader mClassLoader;
    private ContentResolver mContentResolver;
    private PluginManager mPluginManager;

    public PluginContext(PluginInfo pluginInfo, Context baseContext) {
        super(baseContext, android.R.style.Theme);
        Logger.d(TAG, "PluginActivityContext() pluginInfo = " + pluginInfo);
        mBaseContext = baseContext;
        mPluginInfo = pluginInfo;
        mApkPath = pluginInfo.apkPath;
        mPackageManager = pluginInfo.packageManager;
        mAssetManager = pluginInfo.resources.getAssets();
        mResources = pluginInfo.resources;
        mClassLoader = pluginInfo.classLoader;
        mContentResolver = new PluginContentResolver(mBaseContext, super.getContentResolver());
        mPluginManager = PluginManager.getInstance();
    }

    public String getPackageResourcePath() {
        return mApkPath;
    }

    public String getPackageCodePath() {
        return mApkPath;
    }

    public String getPackageName() {
        return PluginManager.getPackageNameCompat(mPluginInfo.packageName, super.getPackageName());
    }

    public PackageManager getPackageManager() {
        return mPackageManager;
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

    @Override
    public Resources.Theme getTheme() {
        return super.getTheme();
    }

    public Context getApplicationContext() {
        return mPluginInfo.application;
    }

    public ApplicationInfo getApplicationInfo() {
        return mPluginInfo.applicationInfo;
    }

    @Override
    public ComponentName startService(Intent intent) {
        if (!StubManager.isStubIntent(mBaseContext, intent)) {
            try {
                return mPluginManager.startService(this, intent);
            } catch (PluginNotFoundException e) {
            }
        }

        return super.startService(intent);
    }

    @Override
    public boolean stopService(Intent intent) {
        if (!StubManager.isStubIntent(mBaseContext, intent)) {
            try {
                return mPluginManager.stopService(this, intent);
            } catch (PluginNotFoundException e) {
            }
        }

        return super.stopService(intent);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection conn,
            int flags) {
        if (!StubManager.isStubIntent(mBaseContext, intent)) {
            try {
                return mPluginManager.bindService(this, intent, conn, flags);
            } catch (PluginNotFoundException e) {
            }
        }

        return super.bindService(intent, conn, flags);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        try {
            mPluginManager.unbindService(this, conn);
            return;
        } catch (PluginNotFoundException e) {
        }

        super.unbindService(conn);
    }

    @Override
    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

}
