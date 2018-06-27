package com.reginald.pluginm.core;

import java.lang.reflect.Field;
import java.util.List;

import com.android.common.ActivityThreadCompat;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.utils.Logger;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.UserHandle;

/**
 * Created by lxy on 18-6-26.
 * <p>
 * 替换ActivityThread中的mInitialApplication的 base context。
 * 这个替换不是必须的，主要目的为hook系统通过ActivityThread获取到Context来进行插件资源获取，如：
 * 1. RemoteViews的创建过程
 * 2. Webview中加载插件中的assets资源。
 * 。。。。
 *
 */
public class HostContext extends ContextWrapper {

    private static final String TAG = "HostContext";

    private Context mBase;
    private PluginManager mPluginManager;

    public static boolean install(Context appContext) {
        Logger.d(TAG, "install()");
        Object target = ActivityThreadCompat.currentActivityThread();
        if (target != null) {
            Class ActivityThreadClass = target.getClass();

            try {
                Field initApplicationField = FieldUtils.getField(ActivityThreadClass, "mInitialApplication");
                Application initApplication = (Application) FieldUtils.readField(initApplicationField, target);
                Logger.d(TAG, "install() initApplication = " + initApplication);
                Context baseContext = initApplication.getBaseContext();
                HostContext hostContext = new HostContext(baseContext);
                Logger.d(TAG, "install() hostContext = " + hostContext);
                FieldUtils.writeField(ContextWrapper.class, "mBase", initApplication, hostContext);
                boolean isSuc = hostContext == initApplication.getBaseContext();
                Logger.d(TAG, "install() success? %b", isSuc);
                return isSuc;
            } catch (Exception e) {
                Logger.e(TAG, "install() error!", e);
            }
        }
        return false;
    }

    private HostContext(Context baseContext) {
        super(baseContext);
        mBase = baseContext;
        mPluginManager = PluginManager.getInstance();
    }

    public Context createPackageContextAsUser(String packageName, int flags, UserHandle user)
            throws PackageManager.NameNotFoundException {
        Logger.d(TAG, "createPackageContextAsUser() packageName =  " + packageName);
        PluginInfo pluginInfo = mPluginManager.getLoadedPluginInfo(packageName);
        if (pluginInfo != null) {
            Logger.d(TAG, "createPackageContextAsUser() return plugin context for " + pluginInfo.packageName);
            return pluginInfo.baseContext;
        }

        try {
            return (Context) MethodUtils.invokeMethod(mBase, "createPackageContextAsUser", packageName, flags, user);
        } catch (Exception e) {
            Logger.e(TAG, "createPackageContextAsUser()", e);
        }

        return null;
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        PluginInfo pluginInfo = mPluginManager.getLoadedPluginInfo(packageName);
        if (pluginInfo != null) {
            Logger.d(TAG, "createPackageContext() return plugin context for " + pluginInfo.packageName);
            return pluginInfo.baseContext;
        }

        return super.createPackageContext(packageName, flags);
    }

    @Override
    public AssetManager getAssets() {
        Logger.d(TAG, "getAssets() ");
        List<PluginInfo> pluginInfos = mPluginManager.getLoadedPluginInfos();
        if (pluginInfos != null && !pluginInfos.isEmpty()) {
            AssetManager combinedAssets = ResourcesManager.getCombinedAssetManager(mBase, pluginInfos);
            if (combinedAssets != null) {
                Logger.d(TAG, "getAssets() return combinedAssets = " + combinedAssets);
                return combinedAssets;
            }
        }

        return super.getAssets();
    }

    @Override
    public Resources getResources() {
        List<PluginInfo> pluginInfos = mPluginManager.getLoadedPluginInfos();
        if (pluginInfos != null && !pluginInfos.isEmpty()) {
            AssetManager combinedAssets = ResourcesManager.getCombinedAssetManager(mBase, pluginInfos);
            if (combinedAssets != null) {
                Logger.d(TAG, "getResources() return combinedResources ");
                return ResourcesManager.createResources(mBase, combinedAssets);
            }
        }

        return super.getResources();
    }
}
