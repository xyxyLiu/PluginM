package com.reginald.pluginm;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import com.reginald.pluginm.parser.PluginPackageParser;

/**
 * Created by lxy on 16-6-21.
 */
public final class PluginInfo {

    public String packageName;
    public PluginPackageParser pkgParser;
    public ClassLoader classLoader;
    public ClassLoader parentClassLoader;
    public ApplicationInfo applicationInfo;
    public Application application;
    public Context baseContext;
    //    public AssetManager assetManager;
    public Resources resources;
    public PackageManager packageManager;
    public String apkPath;
    public long fileSize;
    public long lastModified;
    public String dataDir;
    public String dexDir;
    public String nativeLibDir;
    public String libraryPaths;

    public String toString() {
        return "PluginInfo[ packageName = " + packageName + " , pkgParser = " + pkgParser + " , classLoader = " +
                classLoader + " , resources = " + resources + " , apkPath = " + apkPath + "]";
    }

}
