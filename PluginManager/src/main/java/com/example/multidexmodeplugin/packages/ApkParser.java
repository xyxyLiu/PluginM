package com.example.multidexmodeplugin.packages;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.LogPrinter;

import com.example.multidexmodeplugin.PluginInfo;

/**
 * Created by lxy on 16-6-21.
 */
public class ApkParser {

    private static final String TAG = "ApkParser";

    public static PluginInfo parsePackage(Context context, String apkFile) {

        PluginInfo pluginInfo = new PluginInfo();

        PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(apkFile, PackageManager.GET_ACTIVITIES);

        // test
        if (packageInfo != null) {

            pluginInfo.packageName = packageInfo.packageName;

            if (packageInfo.activities != null) {
                Log.d(TAG, "## packageInfo.activities: ");
                int i = 0;
                for (ActivityInfo activityInfo : packageInfo.activities) {
                    Log.d(TAG, "packageInfo.activitie No." + ++i);
                    activityInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                }
            }


            pluginInfo.pkgInfo = packageInfo;
        } else {
            return null;
        }



        return pluginInfo;
    }
}
