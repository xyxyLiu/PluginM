package com.reginald.pluginm.parser;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
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
            try {
                PluginInfo pluginInfo = new PluginInfo();
                pluginInfo.pkgParser = pluginPackageParser;
                pluginInfo.packageName = pluginPackageParser.getPackageName();
                pluginInfo.applicationInfo = pluginPackageParser.getApplicationInfo(0);

                //test:
                showPluginInfo(pluginInfo.pkgParser);

                return pluginInfo;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    private static void showPluginInfo(PluginPackageParser pluginPackageParser) {

        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.pkgParser = pluginPackageParser;

        PackageInfo packageInfo = null;

        try {
            packageInfo = pluginPackageParser.getPackageInfo(
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS |
                            PackageManager.GET_RECEIVERS | PackageManager.GET_META_DATA);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (packageInfo != null) {
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

            if (packageInfo.providers != null) {
                Log.d(TAG, "\n## packageInfo.providers: ");
                int i = 0;
                for (ProviderInfo providerInfo : packageInfo.providers) {
                    Log.d(TAG, "packageInfo.provider No." + ++i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        providerInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    } else {
                        Log.d(TAG, " " + providerInfo);
                    }
                    Log.d(TAG, "\n");
                }
            }

        }
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
