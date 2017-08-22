package com.reginald.pluginm.parser;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;
import android.util.LogPrinter;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.utils.Logger;

import java.io.File;
import java.util.List;

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
