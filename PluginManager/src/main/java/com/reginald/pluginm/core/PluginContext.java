package com.reginald.pluginm.core;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.stub.PluginContentResolver;
import com.reginald.pluginm.stub.PluginServiceConnection;
import com.reginald.pluginm.stub.PluginStubMainService;
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

    public PluginContext(PluginInfo pluginInfo, Context baseContext) {
        // test theme
        super(baseContext, pluginInfo.applicationInfo.theme);
        Logger.d(TAG, "PluginActivityContext() pluginInfo = " + pluginInfo);
        mBaseContext = baseContext;
        mPluginInfo = pluginInfo;
        mApkPath = pluginInfo.apkPath;
        mPackageManager = pluginInfo.packageManager;
        mAssetManager = pluginInfo.resources.getAssets();
        mResources = pluginInfo.resources;
        mClassLoader = pluginInfo.classLoader;
        mContentResolver = new PluginContentResolver(mBaseContext, super.getContentResolver());
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

    public Context getApplicationContext() {
        return mPluginInfo.application;
    }

    public ApplicationInfo getApplicationInfo() {
        return mPluginInfo.applicationInfo;
    }

    @Override
    public ComponentName startService(Intent intent) {
        Intent pluginIntent = PluginManager.getInstance(getApplicationContext()).getPluginServiceIntent(intent);
        if (pluginIntent != null) {
            pluginIntent.putExtra(PluginStubMainService.INTENT_EXTRA_START_TYPE_KEY, PluginStubMainService.INTENT_EXTRA_START_TYPE_START);
            if (super.startService(pluginIntent) != null) {
                ServiceInfo serviceInfo = pluginIntent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_SERVICEINFO);
                if (serviceInfo != null) {
                    return new ComponentName(serviceInfo.packageName, serviceInfo.name);
                }
            }
            return null;
        } else {
            return super.startService(intent);
        }
    }

    @Override
    public boolean stopService(Intent intent) {
        Intent pluginIntent = PluginManager.getInstance(getApplicationContext()).getPluginServiceIntent(intent);
        if (pluginIntent != null) {
            pluginIntent.putExtra(PluginStubMainService.INTENT_EXTRA_START_TYPE_KEY, PluginStubMainService.INTENT_EXTRA_START_TYPE_STOP);
            super.startService(pluginIntent);
            return true;
        } else {
            return super.stopService(intent);
        }
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection conn,
            int flags) {
        Intent pluginIntent = PluginManager.getInstance(getApplicationContext()).getPluginServiceIntent(intent);
        if (pluginIntent != null) {
            // pluginIntent 中的extras和action会被清空，可以直接利用
            String pluginAppendedAction = PluginStubMainService.getPluginAppendAction(pluginIntent);
            pluginIntent.setAction(pluginAppendedAction);
            return super.bindService(pluginIntent, PluginServiceConnection.fetchConnection(conn), flags);
        } else {
            return super.bindService(intent, conn, flags);
        }
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        PluginServiceConnection pluginServiceConnection = PluginServiceConnection.getConnection(conn);
        if (pluginServiceConnection != null) {
            pluginServiceConnection.unbind();
            super.unbindService(pluginServiceConnection);
        } else {
            super.unbindService(conn);
        }
    }

    @Override
    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

}
