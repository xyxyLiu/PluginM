package com.reginald.pluginm.packages;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.util.Log;
import android.util.LogPrinter;

import com.reginald.pluginm.PluginInfo;

/**
 * Created by lxy on 16-6-21.
 */
public class ApkParser {

    private static final String TAG = "ApkParser";

    public static PluginInfo parsePackage(Context context, String apkFile) {

        PluginInfo pluginInfo = new PluginInfo();

        PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(apkFile,
                PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS |
                        PackageManager.GET_RECEIVERS | PackageManager.GET_META_DATA);

        if (packageInfo != null) {

            pluginInfo.packageName = packageInfo.packageName;


            // test
            Log.d(TAG, "## packageInfo.packageInfo.applicationInfo: ");
            packageInfo.applicationInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
            Log.d(TAG, "\n\n");

            if (packageInfo.activities != null) {
                Log.d(TAG, "## packageInfo.activities: ");
                int i = 0;
                for (ActivityInfo activityInfo : packageInfo.activities) {
                    Log.d(TAG, "packageInfo.activitie No." + ++i);
                    activityInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    Log.d(TAG, "\n");
                }
            }

            if (packageInfo.services != null) {
                Log.d(TAG, "## packageInfo.services: ");
                int i = 0;
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    Log.d(TAG, "packageInfo.service No." + ++i);
                    serviceInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    Log.d(TAG, "\n");
                }
            }


            pluginInfo.pkgInfo = packageInfo;
        } else {
            return null;
        }



        return pluginInfo;
    }
}
