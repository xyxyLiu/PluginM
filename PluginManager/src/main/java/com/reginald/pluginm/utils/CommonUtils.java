package com.reginald.pluginm.utils;

import android.app.ActivityManager;
import android.content.Context;

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

}
