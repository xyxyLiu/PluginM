package com.reginald.pluginm.parser;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.LogPrinter;

import com.reginald.pluginm.BuildConfig;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.utils.Logger;

import java.io.File;
import java.util.List;

/**
 * Created by lxy on 16-6-21.
 */
public class ApkParser {

    private static final String TAG = "ApkParser";
    private static final boolean DEBUG = BuildConfig.DEBUG_LOG & false;

    public static PluginInfo parsePluginInfo(Context context, String apkFile) {
        long startTime = SystemClock.elapsedRealtime();
        try {
            PluginPackageParser pluginPackageParser = getPackageParser(context, apkFile);
            Logger.d(TAG, String.format("parsePluginInfo() apk = %s, create parser cost %d ms",
                    apkFile, SystemClock.elapsedRealtime() - startTime));
            if (pluginPackageParser != null) {

                PluginInfo pluginInfo = new PluginInfo();
                pluginInfo.pkgParser = pluginPackageParser;
                pluginInfo.packageName = pluginPackageParser.getPackageName();
                pluginInfo.applicationInfo = pluginPackageParser.getApplicationInfo(0);

                PackageInfo packageInfo = pluginPackageParser.getPackageInfo(0);
                if (packageInfo != null) {
                    pluginInfo.versionName = packageInfo.versionName;
                    pluginInfo.versionCode = packageInfo.versionCode;
                }

                //test:
                if (DEBUG) {
                    showPluginInfo(pluginInfo.pkgParser);
                }

                return pluginInfo;

            }
        } catch (Exception e) {
            Logger.e(TAG, "parsePluginInfo() error!", e);
            return null;
        } finally {
            Logger.d(TAG, String.format("parsePluginInfo() apk = %s, all cost %d ms",
                    apkFile, SystemClock.elapsedRealtime() - startTime));
        }

        return null;
    }

    public static PluginPackageParser getPackageParser(Context context, String apkFile) {
        return parsePackage(context, apkFile);
    }

    private static void showPluginInfo(PluginPackageParser pluginPackageParser) {
        // test
        try {
            Logger.d(TAG, "\n## packageInfo.packageInfo.applicationInfo: ");
            pluginPackageParser.getApplicationInfo(0).dump(new LogPrinter(Log.DEBUG, TAG), "");
            Logger.d(TAG, "\n\n");

            List<ActivityInfo> activityInfoList = pluginPackageParser.getActivities();
            if (activityInfoList != null) {
                Logger.d(TAG, "\n## packageInfo.activities: ");
                int i = 0;
                for (ActivityInfo activityInfo : activityInfoList) {
                    Logger.d(TAG, "packageInfo.activitie No." + ++i);
                    activityInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    Logger.d(TAG, "\n");
                }
            }

            List<ServiceInfo> serviceInfos = pluginPackageParser.getServices();
            if (serviceInfos != null) {
                Logger.d(TAG, "\n## packageInfo.services: ");
                int i = 0;
                for (ServiceInfo serviceInfo : serviceInfos) {
                    Logger.d(TAG, "packageInfo.service No." + ++i);
                    serviceInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    Logger.d(TAG, "\n");
                }
            }

            List<ActivityInfo> receivers = pluginPackageParser.getReceivers();
            if (receivers != null) {
                Logger.d(TAG, "\n## packageInfo.receivers: ");
                int i = 0;
                for (ActivityInfo receiverInfo : receivers) {
                    Logger.d(TAG, "packageInfo.receiver No." + ++i);
                    receiverInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    Logger.d(TAG, "\n");
                }
            }

            List<ProviderInfo> providerInfos = pluginPackageParser.getProviders();
            if (providerInfos != null) {
                Logger.d(TAG, "\n## packageInfo.providers: ");
                int i = 0;
                for (ProviderInfo providerInfo : providerInfos) {
                    Logger.d(TAG, "packageInfo.provider No." + ++i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        providerInfo.dump(new LogPrinter(Log.DEBUG, TAG), "");
                    } else {
                        Logger.d(TAG, " " + providerInfo);
                    }
                    Logger.d(TAG, "\n");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
