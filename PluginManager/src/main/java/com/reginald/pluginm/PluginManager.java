package com.reginald.pluginm;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.util.LogPrinter;

import com.android.common.ContextCompat;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.stub.ActivityStub;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pluginm.reginald.com.pluginlib.ILocalPluginManager;
import pluginm.reginald.com.pluginlib.PluginHelper;

/**
 * Created by lxy on 16-11-1.
 */
public class PluginManager implements ILocalPluginManager {
    private static final String TAG = "PluginManager";
    private static Map<String, PluginInfo> sLoadedPluginMap = new HashMap<>();
    private ActivityInfo mPendingLoadActivity;
    private static Context sAppContext;


    private static volatile PluginManager sInstance;

    private Context mContext;

    public static synchronized PluginManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new PluginManager(hostContext);
        }

        return sInstance;
    }

    private PluginManager(Context hostContext) {
        mContext = hostContext;
    }

    public PluginInfo loadPlugin(ApplicationInfo applicationInfo) {
        Log.d(TAG, "loadPlugin() applicationInfo = " + applicationInfo);
        try {
            String pluginPackageName = applicationInfo.packageName;
            PluginInfo pluginInfo = null;
            Log.d(TAG, "loadPlugin() pluginPackageName = " + pluginPackageName);
            synchronized (sLoadedPluginMap) {
                pluginInfo = sLoadedPluginMap.get(pluginPackageName);
                if (pluginInfo != null) {
                    Log.d(TAG, "loadPlugin() found loaded pluginInfo " + pluginInfo);
                    return pluginInfo;
                }
            }

            pluginInfo = PluginManagerNative.getInstance(mContext).install(applicationInfo.packageName);

            Log.d(TAG, "loadPlugin() pluginInfo = " + pluginInfo);

            if (pluginInfo == null) {
                Log.e(TAG, "loadPlugin() install error with " + pluginInfo);
                return null;
            }

            synchronized (sLoadedPluginMap) {
                sLoadedPluginMap.put(pluginInfo.packageName, pluginInfo);
            }


            if (!initPlugin(pluginInfo, mContext)) {
                Log.e(TAG, "loadPlugin() initPlugin error!");
                return null;
            }


            Log.d(TAG, "loadPlugin() ok!");
            return pluginInfo;
        } catch (Exception e) {
            Log.e(TAG, "loadPlugin() error! exception: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private static boolean initPlugin(PluginInfo pluginInfo, Context hostContext) {
        Log.d(TAG, "initPlugin() pluginInfo = " + pluginInfo);
//        if (!initPluginHelper(pluginInfo, hostContext)) {
//            Log.e(TAG, "initPlugin() initPluginHelper error! ");
//            return false;
//        }

        if (!initPluginApplication(pluginInfo, hostContext)) {
            Log.e(TAG, "initPlugin() initPluginApplication error! ");
            return false;
        }

        return true;
    }

    private static boolean initPluginHelper(PluginInfo pluginInfo, Context hostContext) {
        Log.d(TAG, "initPluginHelper() pluginInfo = " + pluginInfo);
        Class<?> pluginHelperClazz;
        try {
            pluginHelperClazz = pluginInfo.classLoader.loadClass(PluginHelper.class.getName());
            ILocalPluginManager iPluginManager = PluginManager.getInstance(hostContext);
            if (pluginHelperClazz != null) {
                MethodUtils.invokeStaticMethod(pluginHelperClazz, "onInit",
                        new Object[]{iPluginManager, pluginInfo.packageName},
                        new Class[]{Object.class, String.class});
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean initPluginApplication(PluginInfo pluginInfo, Context hostContext) {
        Log.d(TAG, "initPluginApplication() pluginInfo = " + pluginInfo + " , hostContext = " + hostContext);
        try {
            Context hostBaseContext = hostContext.createPackageContext(hostContext.getPackageName(), Context.CONTEXT_INCLUDE_CODE);
            pluginInfo.baseContext = new PluginContext(pluginInfo, hostBaseContext);

            ApplicationInfo applicationInfo = pluginInfo.pkgParser.getApplicationInfo(0);
            Log.d(TAG, "initPluginApplication() applicationInfo.name = " + applicationInfo.name);

            if (applicationInfo.className == null) {
                applicationInfo.className = Application.class.getName();//BasePluginApplication.class.getName();
            }

            Class pluginAppClass = pluginInfo.classLoader.loadClass(applicationInfo.className);
            pluginInfo.application = (Application) pluginAppClass.newInstance();
            Method attachMethod = android.app.Application.class
                    .getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(pluginInfo.application, pluginInfo.baseContext);
            ContextCompat.setOuterContext(pluginInfo.baseContext, pluginInfo.application);
            pluginInfo.application.onCreate();

            initStaticReceivers(pluginInfo);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void initStaticReceivers(PluginInfo pluginInfo) {
        Map<ActivityInfo, List<IntentFilter>> receiverIntentFilters = pluginInfo.pkgParser.getReceiverIntentFilter();

        if (receiverIntentFilters != null) {
            for (Map.Entry<ActivityInfo, List<IntentFilter>> entry : receiverIntentFilters.entrySet()) {
                ActivityInfo receiverInfo = entry.getKey();
                List<IntentFilter> intentFilters = entry.getValue();

                try {
                    BroadcastReceiver receiver = (BroadcastReceiver) pluginInfo.classLoader.loadClass(receiverInfo.name).newInstance();
                    for (IntentFilter filter : intentFilters) {
                        pluginInfo.application.registerReceiver(receiver, filter);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                //test;
                Log.d(TAG, "initStaticReceivers() receiverInfo = " + receiverInfo);
                int i = 1;
                for (IntentFilter intentFilter : intentFilters) {
                    Log.d(TAG, "initStaticReceivers() IntentFilter No." + i++ + " :");
                    intentFilter.dump(new LogPrinter(Log.DEBUG, "initStaticReceivers() "), "");
                    Log.d(TAG, "initStaticReceivers() \n");
                }

            }
        }
    }


    public void registerActivity(ActivityInfo activityInfo) {
        mPendingLoadActivity = activityInfo;
    }

    /**
     * only called by classloader
     * 目前只有创建插件Activity时有效
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> findPluginClass(String className) throws ClassNotFoundException {
        Log.d(TAG, "findPluginClass() className = " + className + " ,mPendingLoadActivity = " + mPendingLoadActivity);
        String targetPackage = null;
        String targetClass = className;


        // replace target class
        if (className.startsWith(ActivityStub.class.getName()) && mPendingLoadActivity != null) {
            Log.d(TAG, "findPluginClass() mPendingLoadActivity = " + mPendingLoadActivity);
            targetPackage = mPendingLoadActivity.packageName;
            targetClass = mPendingLoadActivity.name;
        }

        Log.d(TAG, "findPluginClass() targetPackage = " + targetPackage + " ,targetClass = " + className);

        if (targetPackage != null && targetClass != null) {
            return loadPluginClass(targetPackage, targetClass);
        }

        throw new ClassNotFoundException(className);
    }

    /**
     * find plugin class by plugin packageName and className
     * @param packageName
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> loadPluginClass(String packageName, String className) throws ClassNotFoundException {
        Log.d(TAG, "loadPluginClass() className = " + className);
        PluginInfo pluginInfo;
        synchronized (sLoadedPluginMap) {
            pluginInfo = sLoadedPluginMap.get(packageName);
        }

        if (pluginInfo != null) {
            ClassLoader pluginClassLoader = pluginInfo.classLoader;
            Log.d(TAG, "loadPluginClass() pluginClassLoader = " + pluginClassLoader);
            try {
                Class<?> clazz = pluginClassLoader.loadClass(className);
                if (clazz != null) {
                    Log.d(TAG, "loadPluginClass() className = " + className + " success!");
                    return clazz;
                }
            } catch (Exception e) {
//                    e.printStackTrace();
                Log.e(TAG, "loadPluginClass() className = " + className + " fail!");
            }
        }
        throw new ClassNotFoundException(className);
    }

    public static PluginInfo getPluginInfo(String packageName) {
        synchronized (sLoadedPluginMap) {
            return sLoadedPluginMap.get(packageName);
        }
    }

    @Override
    public Context createPluginContext(String packageName, Context baseContext) {
        PluginInfo pluginInfo = getPluginInfo(packageName);
        if (pluginInfo != null) {
            return new PluginContext(pluginInfo, baseContext);
        } else {
            return null;
        }
    }


    @Override
    public Intent getPluginActivityIntent(Intent pluginIntent) {
        return PluginManagerNative.getInstance(mContext).getPluginActivityIntent(pluginIntent);
    }
}
