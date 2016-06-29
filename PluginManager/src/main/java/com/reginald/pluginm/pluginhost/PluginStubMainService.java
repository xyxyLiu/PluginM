package com.reginald.pluginm.pluginhost;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.android.common.ActivityThreadCompat;
import com.android.common.ContextCompat;
import com.reginald.pluginm.DexClassLoaderPluginManager;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.pluginbase.PluginContext;
import com.reginald.pluginm.reflect.FieldUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PluginStubMainService extends Service {

    public static final String INTENT_EXTRA_START_TYPE_KEY = "extra.plugin.stubservice.start.type";
    public static final String INTENT_EXTRA_START_TYPE_START = "extra.plugin.stubservice.start.type.start";
    public static final String INTENT_EXTRA_START_TYPE_STOP = "extra.plugin.stubservice.start.type.stop";
    public static final String INTENT_EXTRA_BIND_ACTION_PREFIX = "action.plugin.stubservice.bind";

    private static final String TAG = "PluginStubMainService";

    DexClassLoaderPluginManager mDexClassLoaderPluginManager;

    Map<ComponentName, ServiceRecord> mInstalledServices = new HashMap<>();

    public void onCreate() {
        super.onCreate();
        mDexClassLoaderPluginManager.getInstance(getApplicationContext());
        Log.d(TAG, "onCreate()");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        if (intent != null) {
            String commandType = intent.getStringExtra(INTENT_EXTRA_START_TYPE_KEY);
            ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
            if (targetComponent != null && commandType != null) {

                if (commandType.equals(INTENT_EXTRA_START_TYPE_START)) {

                    ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(targetComponent);

                    Log.d(TAG, "onStartCommand() call Service.onStartCommand() of " + pluginServiceRecord.service);
                    if (pluginServiceRecord != null) {
                        pluginServiceRecord.started = true;
                        pluginServiceRecord.service.onStartCommand(intent, flags, startId);
                    }

                } else if (commandType.equals(INTENT_EXTRA_START_TYPE_STOP)) {
                    ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(targetComponent);
                    if (pluginServiceRecord != null) {
                        pluginServiceRecord.started = false;
                        if (pluginServiceRecord.canStopped()) {
                            removePluginService(pluginServiceRecord);
                        }
                    }
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        synchronized (mInstalledServices) {
            for (ServiceRecord serviceRecord : mInstalledServices.values()) {

                Log.d(TAG, "onDestroy() try destory" + serviceRecord);

                if (serviceRecord != null) {
                    Log.d(TAG, "onDestroy() call Service.onDestroy() of " + serviceRecord.service);
                    serviceRecord.service.onDestroy();
                }
            }
            mInstalledServices.clear();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        showAction(intent);

        ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
        Intent pluginIntent = getPluginIntent(intent);
        if (targetComponent != null) {
            ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(targetComponent);
            Log.d(TAG, "onBind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {
                pluginServiceRecord.iBinder = pluginServiceRecord.service.onBind(pluginIntent);
                pluginServiceRecord.bindCount++;
                Log.d(TAG, "onBind() return " + pluginServiceRecord.iBinder);
                return pluginServiceRecord.iBinder;
            }
        }

        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean res = false;
        Log.d(TAG, "onUnbind()");
        showAction(intent);

        ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
        Intent pluginIntent = getPluginIntent(intent);
        if (targetComponent != null) {
            ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(targetComponent);
            Log.d(TAG, "onUnbind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {

                if (pluginServiceRecord.bindCount > 0) {
                    boolean result = pluginServiceRecord.service.onUnbind(pluginIntent);
                    pluginServiceRecord.bindCount--;
                }

                if (pluginServiceRecord.canStopped()) {
                    removePluginService(pluginServiceRecord);
                }
            }
        }
        Log.d(TAG, "onUnbind() return " + res);
        return res;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");
        showAction(intent);

        Intent pluginIntent = getPluginIntent(intent);
    }

    private Intent getPluginIntent(Intent intent) {
        Intent resIntent = new Intent(intent);
        String action = resIntent.getAction();
        if (action != null) {
            String[] actions = action.split(INTENT_EXTRA_BIND_ACTION_PREFIX);
            resIntent.setAction(actions[0]);
        }
        resIntent.removeExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
        return resIntent;
    }

    private ServiceRecord fetchCachedOrCreateServiceRecord(ComponentName componentName) {
        ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(componentName);
        if (pluginServiceRecord == null) {
            pluginServiceRecord = createPluginService(componentName);
        }

        return pluginServiceRecord;
    }

    private ServiceRecord fetchCachedServiceRecord(ComponentName componentName) {
        // install plugin if needed
        PluginInfo pluginInfo = DexClassLoaderPluginManager.getPluginInfo(componentName.getPackageName());
        if (pluginInfo == null) {
            mDexClassLoaderPluginManager.install(componentName.getPackageName(), false);
        }

        synchronized (mInstalledServices) {
            Log.d(TAG, "fetchCachedServiceRecord() return" + mInstalledServices.get(componentName));
            return mInstalledServices.get(componentName);
        }
    }

    private ServiceRecord createPluginService(ComponentName componentName) {
        ServiceRecord pluginServiceRecord = new ServiceRecord();
        pluginServiceRecord.componentName = componentName;
        PluginInfo pluginInfo = DexClassLoaderPluginManager.getPluginInfo(componentName.getPackageName());
        ServiceInfo serviceInfo = DexClassLoaderPluginManager.getServiceInfo(componentName.getPackageName(), componentName.getClassName());

        if (serviceInfo == null) {
            return null;
        }

        try {
            Class<?> serviceClass = pluginInfo.classLoader.loadClass(serviceInfo.name);
            pluginServiceRecord.service = (Service) serviceClass.newInstance();
            Log.d(TAG, "createPluginService() create service " + pluginServiceRecord.service);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (pluginServiceRecord.service != null) {
            try {
                Method attachMethod = android.app.Service.class
                        .getDeclaredMethod("attach",
                                Context.class, ActivityThreadCompat.getActivityThreadClass(), String.class,
                                IBinder.class, Application.class, Object.class);
                attachMethod.setAccessible(true);


                Context pluginServiceContext = new PluginContext(pluginInfo, getBaseContext());
                ContextCompat.setOuterContext(pluginServiceContext, pluginServiceRecord.service);

                attachMethod.invoke(pluginServiceRecord.service, pluginServiceContext, FieldUtils.readField(this, "mThread"), serviceInfo.name,
                        FieldUtils.readField(this, "mToken"), pluginInfo.application, FieldUtils.readField(this, "mActivityManager"));

                synchronized (mInstalledServices) {
                    mInstalledServices.put(componentName, pluginServiceRecord);
                }
                Log.d(TAG, "createPluginService() call Service.onCreate() of " + pluginServiceRecord.service);
                pluginServiceRecord.service.onCreate();

                return pluginServiceRecord;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    private boolean removePluginService(ServiceRecord serviceRecord) {
        serviceRecord.service.onDestroy();
        synchronized (mInstalledServices) {
            Log.d(TAG, "removePluginService() " + serviceRecord);
            return mInstalledServices.remove(serviceRecord.componentName) != null;
        }
    }

    public void showAction(Intent intent) {
        Log.d(TAG, "ACTION = " + intent.getAction());
    }


    private static class ServiceRecord {
        public ComponentName componentName;
        public Service service;
        public boolean started;
        public int bindCount = 0;
        public IBinder iBinder;

        public boolean canStopped() {
            return service != null && bindCount == 0 && !started;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + String.format("[ service = %s, bindCount = %d started = %b]", service, bindCount, started);
        }
    }
}
