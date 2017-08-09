package com.reginald.pluginm;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.util.LogPrinter;

import com.android.common.ContextCompat;
import com.reginald.pluginm.parser.ApkParser;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.stub.ActivityStub;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexClassLoader;
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

    public boolean loadPlugin(ApplicationInfo applicationInfo) {
        // test
        String pluginPackageName = "com.example.testplugin";
        final boolean isInstallSuc = PluginManagerNative.getInstance(mContext).install(pluginPackageName);

        return loadPlugin(mContext, applicationInfo, true);
    }


    private static boolean loadPlugin(Context context, ApplicationInfo applicationInfo, boolean isStandAlone) {
        Log.d(TAG, "loadPlugin() applicationInfo = " + applicationInfo);
        try {
            String pluginPackageName = applicationInfo.packageName;
            String apkPath = applicationInfo.publicSourceDir;
            Log.d(TAG, "loadPlugin() pluginPackageName = " + pluginPackageName + " ,apkPath = " + apkPath);
            synchronized (sLoadedPluginMap) {
                if (sLoadedPluginMap.get(pluginPackageName) != null) {
                    return true;
                }
            }

            if (TextUtils.isEmpty(apkPath)) {
                Log.d(TAG, "loadPlugin() apkPath = " + apkPath + " error! no config found!");
                return false;
            }


            File apkFile = new File(apkPath);//PackageUtils.copyAssetsApk(context, apkPath);
            File pluginDexPath = context.getDir(PluginManagerNative.PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = context.getDir(PluginManagerNative.PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginNativeLibPath = new File(pluginLibPath, pluginPackageName);
            ClassLoader parentClassLoader;

            // create classloader
            Log.d(TAG, "loadPlugin() mContext.getClassLoader() = " + context.getClassLoader());

            if (isStandAlone) {
                parentClassLoader = context.getClassLoader().getParent();
            } else {
                parentClassLoader = context.getClassLoader();
            }
            DexClassLoader dexClassLoader = new PluginDexClassLoader(
                    apkFile.getAbsolutePath(),
                    pluginDexPath.getAbsolutePath(),
                    pluginNativeLibPath.getAbsolutePath(),
                    parentClassLoader);
            Log.d(TAG, "loadPlugin() dexClassLoader = " + dexClassLoader);
            Log.d(TAG, "loadPlugin() dexClassLoader's parent = " + dexClassLoader.getParent());

            PluginInfo pluginInfo = ApkParser.parsePluginInfo(context, apkFile.getAbsolutePath());
            pluginInfo.classLoader = dexClassLoader;
            pluginInfo.parentClassLoader = parentClassLoader;
            pluginInfo.apkPath = apkFile.getAbsolutePath();
            pluginInfo.fileSize = apkFile.length();


            // loadPlugin so
            File apkParent = apkFile.getParentFile();
            File tempSoDir = new File(apkParent, "temp");
            Set<String> soList = PackageUtils.unZipSo(apkFile, tempSoDir);
            if (soList != null) {
                for (String soName : soList) {
                    PackageUtils.copySo(tempSoDir, soName, pluginNativeLibPath.getAbsolutePath());
                }
                //删掉临时文件
                PackageUtils.deleteAll(tempSoDir);
            }
            pluginInfo.nativeLibDir = pluginNativeLibPath.getAbsolutePath();

            // replace resources
            Resources resources = ResourcesManager.getPluginResources(context, apkFile.getAbsolutePath());
            if (resources != null) {
                pluginInfo.resources = resources;
            } else {
                Log.e(TAG, "loadPlugin() error! resources is null!");
                return false;
            }


            Log.d(TAG, "loadPlugin() pluginInfo = " + pluginInfo);

            if (pluginInfo == null) {
                Log.e(TAG, "loadPlugin() error! pluginInfo is null!");
                return false;
            }

            synchronized (sLoadedPluginMap) {
                sLoadedPluginMap.put(pluginInfo.packageName, pluginInfo);
            }

            if (!initPlugin(pluginInfo, context)) {
                Log.e(TAG, "loadPlugin() initPlugin error!");
                return false;
            }

            Log.d(TAG, "loadPlugin() ok!");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "loadPlugin() error! exception: " + e);
            e.printStackTrace();
            return false;
        }
    }

    private static boolean initPlugin(PluginInfo pluginInfo, Context hostContext) {
        Log.d(TAG, "initPlugin() pluginInfo = " + pluginInfo);
        if (!initPluginHelper(pluginInfo, hostContext)) {
            Log.e(TAG, "initPlugin() initPluginHelper error! ");
            return false;
        }

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
