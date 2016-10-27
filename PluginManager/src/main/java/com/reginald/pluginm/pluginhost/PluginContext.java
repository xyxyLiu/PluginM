package com.reginald.pluginm.pluginhost;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.reginald.pluginm.DexClassLoaderPluginManager;
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
        super(baseContext, pluginInfo.applicationInfo.theme);
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
        return mPluginInfo.applicationInfo;
    }

    @Override
    public ComponentName startService(Intent intent) {
        Intent pluginIntent = DexClassLoaderPluginManager.getInstance(getApplicationContext()).getPluginServiceIntent(intent);
        if (pluginIntent != null) {
            pluginIntent.putExtra(PluginStubMainService.INTENT_EXTRA_START_TYPE_KEY, PluginStubMainService.INTENT_EXTRA_START_TYPE_START);
            return super.startService(pluginIntent);
        } else {
            return super.startService(intent);
        }
    }

    @Override
    public boolean stopService(Intent intent) {
        Intent pluginIntent = DexClassLoaderPluginManager.getInstance(getApplicationContext()).getPluginServiceIntent(intent);
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
        Intent pluginIntent = DexClassLoaderPluginManager.getInstance(getApplicationContext()).getPluginServiceIntent(intent);
        if (pluginIntent != null) {
            String action = pluginIntent.getAction();
            String pluginAppendedAction = PluginStubMainService.getPluginAppendAction(
                    intent.getComponent().getPackageName(), intent.getComponent().getClassName());
            String finalAction = (action != null ? action : "") + pluginAppendedAction;
            pluginIntent.setAction(finalAction);
            Log.d(TAG, "plugin bindService() intent = " + intent);
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
    public void startActivity(Intent intent) {
        startActivity(intent, null);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        Intent pluginIntent = DexClassLoaderPluginManager.getInstance(getApplicationContext()).getPluginActivityIntent(intent);
        if(pluginIntent != null) {
            super.startActivity(pluginIntent, options);
        } else {
            super.startActivity(intent, options);
        }
    }

    @Override
    public void startActivities(Intent[] intents) {
        startActivities(intents, null);
    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {
        for (int i = 0; i < intents.length; i++) {
            Intent intent = intents[i];
            Intent pluginIntent = DexClassLoaderPluginManager.getInstance(getApplicationContext()).getPluginActivityIntent(intent);
            if(pluginIntent != null) {
                intents[i] = pluginIntent;
            }
        }
        startActivities(intents, options);
    }

    @Override
    public ContentResolver getContentResolver() {
        return new PluginContentResolver(this, super.getContentResolver());
    }

}