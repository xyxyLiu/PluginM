package com.reginald.pluginm;

import android.app.Application;
import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.PackageInfo;
import android.content.res.Resources;

import com.reginald.pluginm.parser.PluginPackageParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lxy on 16-6-21.
 */
public final class PluginInfo {

    public String packageName;
    public PluginPackageParser pkgParser;
    public PackageInfo pkgInfo;
    public ClassLoader classLoader;
    public ClassLoader parentClassLoader;
    public Application application;
    public Context baseContext;
//    public AssetManager assetManager;
    public Resources resources;
//    public l serviceDispatcher;
//    public List<IntentFilterInfo> activityIntentFilters = new ArrayList();
//    public List<IntentFilterInfo> receiverIntentFilters = new ArrayList();
//    public List<IntentFilterInfo> serviceIntentFilters = new ArrayList();
    public String apkPath;
    public long fileSize;
    public long lastModified;
    public String dataDir;
    public String dexDir;
    public String nativeLibDir;
    public String libraryPaths;
    public HashMap<String, IContentProvider> providers = new HashMap();


    public String toString() {
        return "PluginInfo[ packageName = " + packageName + " , pkgInfo = " + pkgInfo + " , classLoader = " +
                classLoader + " , resources = " + resources + " , apkPath = " + apkPath + "]";
    }

}
