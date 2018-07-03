package com.reginald.pluginm.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;

import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.stub.StubManager;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by lxy on 17-9-28.
 */

public class ProcessHelper {

    public static int sPid;

    public static String sProcessName;

    public static Application sApp;

    private static Handler sHandler;

    public static void init(Application context) {
        sApp = context;
        sHandler = new Handler(Looper.getMainLooper());
        sPid = Process.myPid();
        sProcessName = getProcessName(context, sPid);
        if (TextUtils.isEmpty(sProcessName)) {
            throw new IllegalStateException("CAN NOT get processName for pid " + sPid);
        }
    }

    public static Context getHostContext() {
        return sApp;
    }

    public static void post(Runnable runnable) {
        sHandler.post(runnable);
    }

    public static void postDelayed(Runnable runnable, long delayMillis) {
        sHandler.postDelayed(runnable, delayMillis);
    }

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

    public static boolean isPluginProcess(Context context) {
        StubManager.ProcessInfo processInfo = StubManager.getInstance(context).getProcessInfo(sProcessName);
        if (processInfo != null) {
            return true;
        }

        return false;
    }

    public static final void setArgV0(String name) {
        try {
            MethodUtils.invokeStaticMethod(Class.forName("android.os.Process"), "setArgV0", name);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
