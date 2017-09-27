package com.reginald.pluginm.utils;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;

import com.reginald.pluginm.stub.Stubs;

import java.util.List;

/**
 * Created by lxy on 17-8-22.
 */

public class CommonUtils {

    public static String getProcessName(Context context, int pid) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> raps = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo rap : raps) {
            if (rap != null && rap.pid == pid) {
                return rap.processName;
            }
        }
        return null;
    }

    public static boolean isComponentInfoMatch(ComponentInfo a, ComponentInfo b) {
        if (a == null && b == null) {
            return true;
        } else if (a != null && b != null) {
            return TextUtils.equals(a.name, b.name) && TextUtils.equals(a.packageName, b.packageName);
        } else {
            return false;
        }
    }

    public static ServiceInfo getServiceInfo(Service service) {
        PackageManager pm = service.getPackageManager();
        if (pm != null) {
            ComponentName componentName = new ComponentName(service, service.getClass());
            try {
                return pm.getServiceInfo(componentName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static ProviderInfo getProviderInfo(ContentProvider provider) {
        PackageManager pm = provider.getContext().getPackageManager();
        if (pm != null) {
            ComponentName componentName = new ComponentName(provider.getContext(), provider.getClass());
            try {
                return pm.getProviderInfo(componentName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

}
