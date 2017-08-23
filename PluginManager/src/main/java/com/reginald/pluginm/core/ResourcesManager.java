package com.reginald.pluginm.core;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

/**
 * Created by lxy on 16-6-2.
 */
public class ResourcesManager {

    Resources mResources;

    public ResourcesManager(Context hostContext, String apkPath) {
        try {
            AssetManager assetManager = createAssetManager(apkPath);
            if (assetManager != null) {
                mResources = new Resources(assetManager, hostContext.getResources().getDisplayMetrics(), hostContext.getResources().getConfiguration());
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("can not load resources from " + apkPath);
    }

    public static Resources getPluginResources(Context hostContext, String apkPath) {
        try {
            AssetManager assetManager = createAssetManager(apkPath);
            if (assetManager != null) {
                return new Resources(assetManager, hostContext.getResources().getDisplayMetrics(), hostContext.getResources().getConfiguration());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static AssetManager createAssetManager(String apkPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(assetManager, apkPath);
            return assetManager;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
