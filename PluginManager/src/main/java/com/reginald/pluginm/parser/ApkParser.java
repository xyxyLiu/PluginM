package com.reginald.pluginm.parser;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.util.Log;
import android.util.LogPrinter;

import com.reginald.pluginm.PluginInfo;

import java.io.File;

/**
 * Created by lxy on 16-6-21.
 */
public class ApkParser {

    private static final String TAG = "ApkParser";


    public static PluginInfo parsePluginInfo(Context context, String apkFile) {
        PluginPackageParser pluginPackageParser = parsePackage(context, apkFile);
        if (pluginPackageParser != null) {
            return getPluginInfo(pluginPackageParser);
        }

        return null;
    }

    private static PluginInfo getPluginInfo(PluginPackageParser pluginPackageParser) {

        PluginInfo pluginInfo = new PluginInfo();

        PackageInfo packageInfo = null;

        try {
            packageInfo = pluginPackageParser.getPackageInfo(
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS |
                            PackageManager.GET_RECEIVERS | PackageManager.GET_META_DATA);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (packageInfo != null) {
            pluginInfo.pkgParser = pluginPackageParser;
            pluginInfo.packageName = packageInfo.packageName;


            // test
            Log.d(TAG, "\n## packageInfo.packageInfo.applicationInfo: ");
            packageInfo.applicationInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
            Log.d(TAG, "\n\n");

            if (packageInfo.activities != null) {
                Log.d(TAG, "\n## packageInfo.activities: ");
                int i = 0;
                for (ActivityInfo activityInfo : packageInfo.activities) {
                    Log.d(TAG, "packageInfo.activitie No." + ++i);
                    activityInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    Log.d(TAG, "\n");
                }
            }

            if (packageInfo.services != null) {
                Log.d(TAG, "\n## packageInfo.services: ");
                int i = 0;
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    Log.d(TAG, "packageInfo.service No." + ++i);
                    serviceInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    Log.d(TAG, "\n");
                }
            }

            if (packageInfo.receivers != null) {
                Log.d(TAG, "\n## packageInfo.receivers: ");
                int i = 0;
                for (ActivityInfo receiverInfo : packageInfo.receivers) {
                    Log.d(TAG, "packageInfo.receiver No." + ++i);
                    receiverInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    Log.d(TAG, "\n");
                }
            }


            pluginInfo.pkgInfo = packageInfo;
        } else {
            return null;
        }



        return pluginInfo;
    }

    private static PluginPackageParser parsePackage(Context context, String apkFile) {
        try {
            return new PluginPackageParser(context, new File(apkFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
