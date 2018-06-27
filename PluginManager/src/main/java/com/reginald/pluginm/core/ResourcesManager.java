package com.reginald.pluginm.core;

import java.util.List;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.utils.Logger;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

/**
 * Created by lxy on 16-6-2.
 */
public class ResourcesManager {

    private static final String TAG = "ResourcesManager";

    public static Resources createResources(Context hostContext, AssetManager assetManager) {
        try {
            if (assetManager != null) {
                return new Resources(assetManager, hostContext.getResources().getDisplayMetrics(), hostContext.getResources().getConfiguration());
            }
        } catch (Exception e) {
            Logger.e(TAG, "createResources() error!", e);
        }
        return null;
    }

    public static AssetManager getCombinedAssetManager(Context hostContext, List<PluginInfo> pluginInfos) {
        try {

            /* 5.0以上这种方式会有问题
            AssetManager assetManager = AssetManager.class.newInstance();
            AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(assetManager,
                    hostContext.getApplicationInfo().sourceDir);
            */
            // TODO 在插件进程中，宿主资源与插件资源混合，如果没有对插件包进行资源id隔离，有产生资源冲突的风险。
            AssetManager assetManager = hostContext.getAssets();
            for (PluginInfo pluginInfo : pluginInfos) {
                MethodUtils.invokeMethod(assetManager, "addAssetPath", pluginInfo.apkPath);
            }
            return assetManager;
        } catch (Exception e) {
            Logger.e(TAG, "getCombinedAssetManager() error!", e);
        }

        return null;
    }

    public static AssetManager createAssetManager(String apkPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            MethodUtils.invokeMethod(assetManager, "addAssetPath", apkPath);
            return assetManager;
        } catch (Exception e) {
            Logger.e(TAG, "createAssetManager() error!", e);
        }

        return null;
    }
}
