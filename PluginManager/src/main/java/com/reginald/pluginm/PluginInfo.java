package com.reginald.pluginm;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import com.reginald.pluginm.parser.PluginPackageParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lxy on 16-6-21.
 */
public final class PluginInfo implements Parcelable {
    // install info
    public String packageName;
    public String apkPath;
    public String versionName;
    public int versionCode;
    public long fileSize;
    public long lastModified;
    public String dataDir;
    public String dexDir;
    public String nativeLibDir;
    public final Map<String, Map<String, String>> pluginInvokerClassMap = new HashMap<>();

    // loaded info
    public PluginPackageParser pkgParser;
    public ClassLoader classLoader;
    public ClassLoader parentClassLoader;
    public ApplicationInfo applicationInfo;
    public Application application;
    public Context baseContext;
    public Resources resources;
    public PackageManager packageManager;

    public PluginInfo() {

    }

    protected PluginInfo(Parcel in) {
        packageName = in.readString();
        apkPath = in.readString();
        versionName = in.readString();
        versionCode = in.readInt();
        fileSize = in.readLong();
        lastModified = in.readLong();
        dataDir = in.readString();
        dexDir = in.readString();
        nativeLibDir = in.readString();
        in.readMap(pluginInvokerClassMap, PluginInfo.class.getClassLoader());

        applicationInfo = in.readParcelable(ApplicationInfo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(apkPath);
        dest.writeString(versionName);
        dest.writeInt(versionCode);
        dest.writeLong(fileSize);
        dest.writeLong(lastModified);
        dest.writeString(dataDir);
        dest.writeString(dexDir);
        dest.writeString(nativeLibDir);
        dest.writeMap(pluginInvokerClassMap);
        dest.writeParcelable(applicationInfo, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PluginInfo> CREATOR = new Creator<PluginInfo>() {
        @Override
        public PluginInfo createFromParcel(Parcel in) {
            return new PluginInfo(in);
        }

        @Override
        public PluginInfo[] newArray(int size) {
            return new PluginInfo[size];
        }
    };

    public String toString() {
        return String.format("PluginInfo[ packageName = %s, apkPath = %s, versionName = %s, versionCode = %d, fileSize = %d, " +
                        "lastModified = %d, dataDir = %s, dexDir = %s, nativeLibDir = %s, pluginInvokerClassMap = %s]",
                packageName, apkPath, versionName, versionCode, fileSize, lastModified,
                dataDir, dexDir, nativeLibDir, pluginInvokerClassMap);
    }

}
