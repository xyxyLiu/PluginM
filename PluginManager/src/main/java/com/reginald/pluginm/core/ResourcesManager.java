package com.reginald.pluginm.core;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.reginald.pluginm.utils.Logger;

/**
 * Created by lxy on 16-6-2.
 */
public class ResourcesManager {

    private static final String TAG = "ResourcesManager";

    public static Resources getPluginResources(Context hostContext, String apkPath) {
        try {
            AssetManager assetManager = createAssetManager(apkPath);
            if (assetManager != null) {
                return new Resources(assetManager, hostContext.getResources().getDisplayMetrics(), hostContext.getResources().getConfiguration());
            }
        } catch (Exception e) {
            Logger.e(TAG, "getPluginResources() error!", e);
        }
        return null;
    }

    private static AssetManager createAssetManager(String apkPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(assetManager, apkPath);
            return assetManager;
        } catch (Exception e) {
            Logger.e(TAG, "createAssetManager() error!", e);
        }

        return null;
    }
}
