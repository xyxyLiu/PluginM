package com.reginald.pluginm;

import android.app.Application;
import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.PackageInfo;
import android.content.res.Resources;

import java.util.HashMap;

/**
 * Created by lxy on 16-6-21.
 */
public class PluginInfo {

    public String packageName;
    public PackageInfo pkgInfo;
    public ClassLoader classLoader;
    public ClassLoader parentClassLoader;
    public Application applicationObject;
    public Context hostContext;
//    public AssetManager assetManager;
    public Resources resources;
//    public l serviceDispatcher;
//    public ArrayList<i> activityIntentFilters = new ArrayList();
//    public ArrayList<i> receiverIntentFilters = new ArrayList();
//    public ArrayList<i> serviceIntentFilters = new ArrayList();
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
