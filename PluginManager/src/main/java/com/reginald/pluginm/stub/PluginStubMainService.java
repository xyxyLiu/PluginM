package com.reginald.pluginm.stub;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.common.ActivityThreadCompat;
import com.android.common.ContextCompat;
import com.example.multidexmodeplugin.IPluginServiceStubBinder;
import com.reginald.pluginm.PluginContext;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.PluginManager;
import com.reginald.pluginm.PluginManager;
import com.reginald.pluginm.reflect.FieldUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.reginald.pluginm.PluginManager.EXTRA_INTENT_TARGET_SERVICEINFO;

public class PluginStubMainService extends Service {

    public static final String INTENT_EXTRA_START_TYPE_KEY = "extra.plugin.stubservice.start.type";
    public static final String INTENT_EXTRA_START_TYPE_START = "extra.plugin.stubservice.start.type.start";
    public static final String INTENT_EXTRA_START_TYPE_STOP = "extra.plugin.stubservice.start.type.stop";
    public static final String INTENT_ACTION_BIND_PREFIX = "action.plugin.stubservice.bind";

    private static final String TAG = "PluginStubMainService";

    PluginManager mPluginManager;

    Map<ComponentName, ServiceRecord> mInstalledServices = new HashMap<>();

    public void onCreate() {
        super.onCreate();
        mPluginManager = PluginManager.getInstance(getApplicationContext());
        Log.d(TAG, "onCreate()");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() intent = " + intent);
        if (intent != null) {
            String commandType = intent.getStringExtra(INTENT_EXTRA_START_TYPE_KEY);
            ServiceInfo serviceInfo = intent.getParcelableExtra(EXTRA_INTENT_TARGET_SERVICEINFO);

            if (serviceInfo != null && commandType != null) {
                if (commandType.equals(INTENT_EXTRA_START_TYPE_START)) {
                    ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(serviceInfo);

                    Log.d(TAG, "onStartCommand() call Service.onStartCommand() of " + pluginServiceRecord.service);
                    if (pluginServiceRecord != null) {
                        Intent origIntent = getOriginalIntent(intent, pluginServiceRecord.service);
                        pluginServiceRecord.started = true;
                        pluginServiceRecord.service.onStartCommand(origIntent, flags, startId);
                    }

                } else if (commandType.equals(INTENT_EXTRA_START_TYPE_STOP)) {
                    ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(serviceInfo);
                    if (pluginServiceRecord != null) {
                        pluginServiceRecord.started = false;
                        if (pluginServiceRecord.canStopped()) {
                            removePluginService(pluginServiceRecord);
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "onStartCommand() not intent, stop stub service!");
            stopSelf();
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
                    destroyPluginService(serviceRecord);
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

        ServiceInfo serviceInfo = intent.getParcelableExtra(EXTRA_INTENT_TARGET_SERVICEINFO);
        if (serviceInfo != null) {
            ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(serviceInfo);
            Log.d(TAG, "onBind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {
                Intent origIntent = getOriginalIntent(intent, pluginServiceRecord.service);
                BindRecord bindRecord = pluginServiceRecord.getBindRecord(origIntent);
                Log.d(TAG, "onBind() before bindRecord = " + bindRecord);
                if (bindRecord == null) {
                    bindRecord = new BindRecord(pluginServiceRecord, new Intent(origIntent));
                    bindRecord.iBinder = pluginServiceRecord.service.onBind(origIntent);
                    bindRecord.needOnbind = false;
                    bindRecord.needUnbind = true;
                    pluginServiceRecord.addBindRecord(origIntent, bindRecord);
//                pluginServiceRecord.bindCount++;
                    Log.d(TAG, "onBind() return " + bindRecord.iBinder);
                } else if (bindRecord.needRebind) {
                    pluginServiceRecord.service.onRebind(origIntent);
                    bindRecord.needUnbind = true;
                } else if (bindRecord.needOnbind) {
                    bindRecord.iBinder = pluginServiceRecord.service.onBind(origIntent);
                    bindRecord.needOnbind = false;
                    bindRecord.needUnbind = true;
                }

                bindRecord.bindCount++;
                return new StubBinder(bindRecord);
            }
        }

        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean res = false;
        Log.d(TAG, "onUnbind()");
        showAction(intent);

        ServiceInfo serviceInfo = intent.getParcelableExtra(EXTRA_INTENT_TARGET_SERVICEINFO);
        if (serviceInfo != null) {
            ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(serviceInfo);
            Log.d(TAG, "onUnbind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {
                Intent origIntent = getOriginalIntent(intent, pluginServiceRecord.service);
                BindRecord bindRecord = pluginServiceRecord.getBindRecord(origIntent);
                if (bindRecord != null) {
                    Log.d(TAG, "onUnbind() BindRecord = " + bindRecord);
                    bindRecord.bindCount--;
//                    if (pluginServiceRecord.bindCount > 0) {
                    if (bindRecord.bindCount == 0 && bindRecord.needUnbind) {
                        bindRecord.needRebind = pluginServiceRecord.service.onUnbind(origIntent);
                        bindRecord.needUnbind = false;
//                            bindRecord.needOnbind = true;
//                            pluginServiceRecord.removeBindRecord(pluginIntent);
                    }
//                        pluginServiceRecord.bindCount--;
//                    }

                    if (pluginServiceRecord.canStopped()) {
                        removePluginService(pluginServiceRecord);
                    }
                } else {
                    Log.e(TAG, "onUnbind() can not find BindRecord for " + origIntent);
                }
            }
        }
        Log.d(TAG, "onUnbind() return " + res);
        return res;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind() do nothing!");
//        showAction(intent);
//
//        ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
//        Intent pluginIntent = getPluginIntent(intent);
//        if (targetComponent != null) {
//            ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(targetComponent);
//            Log.d(TAG, "onRebind() before pluginServiceRecord = " + pluginServiceRecord);
//            if (pluginServiceRecord != null) {
//                BindRecord bindRecord = pluginServiceRecord.getBindRecord(pluginIntent);
//
//                if (bindRecord == null) {
//                    Log.d(TAG, "onRebind() add BindRecord for " + pluginServiceRecord);
//                    bindRecord = new BindRecord(pluginServiceRecord, new Intent(intent));
//                    pluginServiceRecord.addBindRecord(pluginIntent, bindRecord);
//                }
//
//                Log.d(TAG, "onRebind() BindRecord = " + bindRecord);
//
////                pluginServiceRecord.bindCount++;
//                if (bindRecord.needRebind) {
//                    pluginServiceRecord.service.onRebind(pluginIntent);
//                    bindRecord.needUnbind = true;
//                } else if (bindRecord.needOnbind) {
//                    bindRecord.iBinder = pluginServiceRecord.service.onBind(pluginIntent);
//                    bindRecord.needOnbind = false;
//                    bindRecord.needUnbind = true;
//                }
////                else {
////                    bindRecord.needUnbind = false;
////                }
//
//            }
//        }
    }

    private Intent getOriginalIntent(Intent pluginIntent, Service service) {
        ComponentName componentName = new ComponentName(service.getPackageName(), service.getClass().getName());
        Intent origIntent = PluginManager.recoverOriginalIntent(pluginIntent, componentName, service.getClassLoader());

        String action = origIntent.getAction();
        if (action != null && action.endsWith(INTENT_ACTION_BIND_PREFIX)) {
            String[] actions = action.split(INTENT_ACTION_BIND_PREFIX);
            origIntent.setAction(actions[0]);
        }

        return origIntent;
    }

    private ServiceRecord fetchCachedOrCreateServiceRecord(ServiceInfo serviceInfo) {
        Log.d(TAG, "fetchCachedOrCreateServiceRecord() serviceInfo = " + serviceInfo);
        ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(serviceInfo);
        if (pluginServiceRecord == null) {
            pluginServiceRecord = createPluginService(serviceInfo);
        }

        return pluginServiceRecord;
    }

    private ServiceRecord fetchCachedServiceRecord(ServiceInfo serviceInfo) {
        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);

        synchronized (mInstalledServices) {
            Log.d(TAG, "fetchCachedServiceRecord() return " + mInstalledServices.get(componentName));
            return mInstalledServices.get(componentName);
        }
    }


    private ServiceRecord createPluginService(ServiceInfo serviceInfo) {
        PluginInfo loadedPluginInfo = PluginManager.getInstance(getApplicationContext()).loadPlugin(serviceInfo.applicationInfo);
        if (loadedPluginInfo == null) {
            return null;
        }

        ServiceRecord pluginServiceRecord = new ServiceRecord(serviceInfo);

        try {
            Class<?> serviceClass = loadedPluginInfo.classLoader.loadClass(serviceInfo.name);
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


                Context pluginServiceContext = new PluginContext(loadedPluginInfo, getBaseContext());
                ContextCompat.setOuterContext(pluginServiceContext, pluginServiceRecord.service);

                attachMethod.invoke(pluginServiceRecord.service, pluginServiceContext, FieldUtils.readField(this, "mThread"), serviceInfo.name,
                        FieldUtils.readField(this, "mToken"), loadedPluginInfo.application, FieldUtils.readField(this, "mActivityManager"));

                // test
                Log.d(TAG, "mBase of service is " + FieldUtils.readField(pluginServiceRecord.service, "mBase"));

                synchronized (mInstalledServices) {
                    mInstalledServices.put(pluginServiceRecord.componentName, pluginServiceRecord);
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
        destroyPluginService(serviceRecord);
        synchronized (mInstalledServices) {

            boolean isSuc = mInstalledServices.remove(serviceRecord.componentName) != null;
            Log.d(TAG, "removePluginService() " + serviceRecord + " suc ? " + isSuc);
            stopStubServiceIfNeededLocked();
            return isSuc;
        }

    }

    private void destroyPluginService(ServiceRecord serviceRecord) {
        Log.d(TAG, "destroyPluginService() for " + serviceRecord.service.getClass().getName());
        serviceRecord.service.onDestroy();
        try {
            Object contextImpl = ((ContextWrapper) serviceRecord.service.getBaseContext()).getBaseContext();
            Class<?> contextImplClazz = Class.forName("android.app.ContextImpl");
            if (contextImplClazz != null) {
                Method cleanUpMethod = contextImplClazz.getDeclaredMethod("scheduleFinalCleanup", new Class[]{String.class, String.class});
                cleanUpMethod.setAccessible(true);
                if (cleanUpMethod != null) {
                    cleanUpMethod.invoke(contextImpl, serviceRecord.service.getClass().getName(), "Service");
                    Log.d(TAG, "scheduleFinalCleanup() for " + serviceRecord.service.getClass().getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopStubServiceIfNeededLocked() {
        if (mInstalledServices.isEmpty()) {
            stopSelf();
            Log.d(TAG, "stopStubServiceIfNeededLocked() stop stub service!");
        }
    }

    public void showAction(Intent intent) {
        Log.d(TAG, "ACTION = " + intent.getAction());
    }

    public static String getPluginAppendAction(Intent pluginIntent) {
        ServiceInfo serviceInfo = (ServiceInfo) pluginIntent.getParcelableExtra(EXTRA_INTENT_TARGET_SERVICEINFO);
        if (serviceInfo == null) {
            return null;
        }

        //加上System.nanoTime()是因为让每一次bindService都产生一次onBind()回调
        return PluginStubMainService.INTENT_ACTION_BIND_PREFIX + System.nanoTime() +
                serviceInfo.packageName + "#" + serviceInfo.name;
    }


    private static class ServiceRecord {
        public ServiceInfo serviceInfo;
        public ComponentName componentName;
        public Service service;
        public boolean started;

        HashMap<Intent.FilterComparison, BindRecord> bindRecords = new HashMap<>();

        public ServiceRecord(ServiceInfo serviceInfo) {
            this.serviceInfo = serviceInfo;
            this.componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        }

        public boolean canStopped() {
            if (started) {
                return false;
            }

            for (BindRecord bindRecord : bindRecords.values()) {
                if (bindRecord.needUnbind || bindRecord.bindCount != 0) {
                    return false;
                }
            }

            return true;
        }

        public BindRecord getBindRecord(Intent intent) {
            Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
            return bindRecords.get(filterComparison);
        }

        public void addBindRecord(Intent intent, BindRecord bindRecord) {
            Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
            bindRecords.put(filterComparison, bindRecord);
        }

        public void removeBindRecord(Intent intent) {
            Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
            bindRecords.remove(filterComparison);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + String.format("[ service = %s, started = %b, bindRecords = %s]",
                    service, started, bindRecords);
        }
    }


    private static class BindRecord {
        public ServiceRecord serviceRecord;
        public Intent intent;
        public IBinder iBinder;
        public boolean needRebind;
        public boolean needUnbind;
        public boolean needOnbind = true;
        public int bindCount;

        public BindRecord(ServiceRecord serviceRecord, Intent intent) {
            this.serviceRecord = serviceRecord;
            this.intent = intent;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + String.format("[ intent = %s, iBinder = %s, needRebind = %b, needUnbind = %b, needOnbind = %b, bindCount = %d ]",
                    intent, iBinder, needRebind, needUnbind, needOnbind, bindCount);
        }
    }

    private class StubBinder extends IPluginServiceStubBinder.Stub {
        private BindRecord mBindRecord;
        private Intent mIntent;

        public StubBinder(BindRecord bindRecord) {
            mBindRecord = bindRecord;
            mIntent = bindRecord.intent;
        }

//        @Override
//        public boolean needConnect() {
//            BindRecord bindRecord = getBindRecord();
//            Log.d(TAG, "needConnect() " + bindRecord);
//            if (bindRecord != null) {
//                return bindRecord.;
//            } else {
//                return false;
//            }
//        }

        @Override
        public ComponentName getComponentName() throws RemoteException {
            BindRecord bindRecord = getBindRecord();
            Log.d(TAG, "getComponentName() " + bindRecord);
            if (bindRecord != null) {
                return bindRecord.serviceRecord.componentName;
            } else {
                return null;
            }
        }

        @Override
        public IBinder getBinder() throws RemoteException {
            BindRecord bindRecord = getBindRecord();
            Log.d(TAG, "getBinder() " + bindRecord);
            if (bindRecord != null) {
                return bindRecord.iBinder;
            } else {
                return null;
            }
        }

        private BindRecord getBindRecord() {
            Log.d(TAG, "getBindRecord() " + mBindRecord + " , " + mIntent);
            ServiceRecord serviceRecord = fetchCachedServiceRecord(mBindRecord.serviceRecord.serviceInfo);

            if (serviceRecord != null) {
                return serviceRecord.getBindRecord(mIntent);
            } else {
                return null;
            }
        }
    }

}
