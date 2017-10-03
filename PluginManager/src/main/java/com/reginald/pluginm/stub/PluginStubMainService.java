package com.reginald.pluginm.stub;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.common.ActivityThreadCompat;
import com.android.common.ContextCompat;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.core.PluginContext;
import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.core.PluginManagerService;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.utils.CommonUtils;
import com.reginald.pluginm.utils.Logger;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PluginStubMainService extends Service {

    public static final String INTENT_EXTRA_START_TYPE_KEY = "extra.plugin.stubservice.start.type";
    public static final String INTENT_EXTRA_START_TYPE_START = "extra.plugin.stubservice.start.type.start";
    public static final String INTENT_EXTRA_START_TYPE_STOP = "extra.plugin.stubservice.start.type.stop";
    public static final String INTENT_EXTRA_START_TYPE_STARTID = "extra.plugin.stubservice.start.type.startid";
    public static final String INTENT_ACTION_BIND_PREFIX = "action.plugin.stubservice.bind";

    private static final String TAG = "PluginStubMainService";

    private PluginManager mPluginManager;

    private final Map<ComponentName, ServiceRecord> mInstalledServices = new HashMap<>();

    private ServiceInfo mStubInfo;

    public void onCreate() {
        super.onCreate();
        mPluginManager = PluginManager.getInstance(getApplicationContext());
        mStubInfo = CommonUtils.getServiceInfo(this);
        Logger.d(TAG, "onCreate() mStubInfo = " + mStubInfo);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand() intent = " + intent);
        if (intent != null) {
            String commandType = intent.getStringExtra(INTENT_EXTRA_START_TYPE_KEY);
            ServiceInfo serviceInfo = intent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_SERVICEINFO);

            if (serviceInfo != null && commandType != null) {
                if (commandType.equals(INTENT_EXTRA_START_TYPE_START)) {
                    ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(serviceInfo);

                    Logger.d(TAG, "onStartCommand() call Service.onStartCommand() of " + pluginServiceRecord.service);
                    if (pluginServiceRecord != null) {
                        Intent origIntent = getOriginalIntent(intent, pluginServiceRecord.service);
                        pluginServiceRecord.started = true;
                        pluginServiceRecord.service.onStartCommand(origIntent, flags, pluginServiceRecord.makeNextStartId());
                    }

                } else if (commandType.equals(INTENT_EXTRA_START_TYPE_STOP)) {
                    ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(serviceInfo);
                    if (pluginServiceRecord != null) {
                        int stopId = intent.getIntExtra(INTENT_EXTRA_START_TYPE_STARTID, -1);
                        if (stopId < 0 || stopId == pluginServiceRecord.startId) {
                            pluginServiceRecord.started = false;
                            if (pluginServiceRecord.canStopped()) {
                                removePluginService(pluginServiceRecord);
                            }
                        }
                    }
                }
            }
        } else {
            Logger.d(TAG, "onStartCommand() not intent, stop stub service!");
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        Logger.d(TAG, "onDestroy()");
        synchronized (mInstalledServices) {
            for (ServiceRecord serviceRecord : mInstalledServices.values()) {

                Logger.d(TAG, "onDestroy() try destory" + serviceRecord);

                if (serviceRecord != null) {
                    Logger.d(TAG, "onDestroy() call Service.onDestroy() of " + serviceRecord.service);
                    destroyPluginService(serviceRecord);
                }
            }
            mInstalledServices.clear();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.d(TAG, "onBind()");
        showAction(intent);

        ServiceInfo serviceInfo = intent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_SERVICEINFO);
        if (serviceInfo != null) {
            ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(serviceInfo);
            Logger.d(TAG, "onBind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {
                Intent origIntent = getOriginalIntent(intent, pluginServiceRecord.service);
                BindRecord bindRecord = pluginServiceRecord.getBindRecord(origIntent);
                Logger.d(TAG, "onBind() before bindRecord = " + bindRecord);
                if (bindRecord == null) {
                    bindRecord = new BindRecord(pluginServiceRecord, new Intent(origIntent));
                    bindRecord.iBinder = pluginServiceRecord.service.onBind(origIntent);
                    bindRecord.needOnbind = false;
                    bindRecord.needUnbind = true;
                    pluginServiceRecord.addBindRecord(origIntent, bindRecord);
                    Logger.d(TAG, "onBind() return " + bindRecord.iBinder);
                } else if (bindRecord.needRebind) {
                    pluginServiceRecord.service.onRebind(origIntent);
                    bindRecord.needRebind = false;
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
        Logger.d(TAG, "onUnbind()");
        showAction(intent);

        ServiceInfo serviceInfo = intent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_SERVICEINFO);
        if (serviceInfo != null) {
            ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(serviceInfo);
            Logger.d(TAG, "onUnbind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {
                Intent origIntent = getOriginalIntent(intent, pluginServiceRecord.service);
                BindRecord bindRecord = pluginServiceRecord.getBindRecord(origIntent);
                if (bindRecord != null) {
                    Logger.d(TAG, "onUnbind() BindRecord = " + bindRecord);
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
                    Logger.e(TAG, "onUnbind() can not find BindRecord for " + origIntent);
                }
            }
        }
        Logger.d(TAG, "onUnbind() return " + res);
        return res;
    }

    @Override
    public void onRebind(Intent intent) {
        Logger.d(TAG, "onRebind() do nothing!");
//        showAction(intent);
//
//        ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
//        Intent pluginIntent = getPluginIntent(intent);
//        if (targetComponent != null) {
//            ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(targetComponent);
//            Logger.d(TAG, "onRebind() before pluginServiceRecord = " + pluginServiceRecord);
//            if (pluginServiceRecord != null) {
//                BindRecord bindRecord = pluginServiceRecord.getBindRecord(pluginIntent);
//
//                if (bindRecord == null) {
//                    Logger.d(TAG, "onRebind() add BindRecord for " + pluginServiceRecord);
//                    bindRecord = new BindRecord(pluginServiceRecord, new Intent(intent));
//                    pluginServiceRecord.addBindRecord(pluginIntent, bindRecord);
//                }
//
//                Logger.d(TAG, "onRebind() BindRecord = " + bindRecord);
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
        return PluginManagerService.recoverOriginalIntent(pluginIntent, service.getClassLoader());
    }

    private ServiceRecord fetchCachedOrCreateServiceRecord(ServiceInfo serviceInfo) {
        Logger.d(TAG, "fetchCachedOrCreateServiceRecord() serviceInfo = " + serviceInfo);
        ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(serviceInfo);
        if (pluginServiceRecord == null) {
            pluginServiceRecord = createPluginService(serviceInfo);
        }

        return pluginServiceRecord;
    }

    private ServiceRecord fetchCachedServiceRecord(ServiceInfo serviceInfo) {
        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);

        synchronized (mInstalledServices) {
            Logger.d(TAG, "fetchCachedServiceRecord() return " + mInstalledServices.get(componentName));
            return mInstalledServices.get(componentName);
        }
    }


    private ServiceRecord createPluginService(ServiceInfo serviceInfo) {
        PluginInfo loadedPluginInfo = PluginManager.getInstance(getApplicationContext()).loadPlugin(serviceInfo.packageName);
        if (loadedPluginInfo == null) {
            Logger.e(TAG, "createPluginService() no loaded plugininfo found for " + serviceInfo);
            return null;
        }

        ServiceRecord pluginServiceRecord = new ServiceRecord(serviceInfo);

        try {
            Class<?> serviceClass = loadedPluginInfo.classLoader.loadClass(serviceInfo.name);
            pluginServiceRecord.service = (Service) serviceClass.newInstance();
            Logger.d(TAG, "createPluginService() create service " + pluginServiceRecord.service);
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
                Logger.d(TAG, "mBase of service is " + FieldUtils.readField(pluginServiceRecord.service, "mBase"));

                synchronized (mInstalledServices) {
                    mInstalledServices.put(pluginServiceRecord.componentName, pluginServiceRecord);
                }
                Logger.d(TAG, "createPluginService() call Service.onCreate() of " + pluginServiceRecord.service);
                pluginServiceRecord.service.onCreate();

                mPluginManager.callServiceOnCreate(pluginServiceRecord.service, mStubInfo, serviceInfo);

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
            Logger.d(TAG, "removePluginService() " + serviceRecord + " suc ? " + isSuc);
            stopStubServiceIfNeededLocked();
            return isSuc;
        }

    }

    private void destroyPluginService(ServiceRecord serviceRecord) {
        Logger.d(TAG, "destroyPluginService() for " + serviceRecord.service.getClass().getName());
        serviceRecord.service.onDestroy();
        mPluginManager.callServiceOnDestory(serviceRecord.service);
        try {
            Object contextImpl = ((ContextWrapper) serviceRecord.service.getBaseContext()).getBaseContext();
            Class<?> contextImplClazz = Class.forName("android.app.ContextImpl");
            if (contextImplClazz != null) {
                Method cleanUpMethod = contextImplClazz.getDeclaredMethod("scheduleFinalCleanup", new Class[]{String.class, String.class});
                cleanUpMethod.setAccessible(true);
                if (cleanUpMethod != null) {
                    cleanUpMethod.invoke(contextImpl, serviceRecord.service.getClass().getName(), "Service");
                    Logger.d(TAG, "scheduleFinalCleanup() for " + serviceRecord.service.getClass().getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopStubServiceIfNeededLocked() {
        if (mInstalledServices.isEmpty()) {
            stopSelf();
            Logger.d(TAG, "stopStubServiceIfNeededLocked() stop stub service!");
        }
    }

    public void showAction(Intent intent) {
        Logger.d(TAG, "ACTION = " + intent.getAction());
    }

    public static String getPluginAppendAction(Intent pluginIntent) {
        ServiceInfo serviceInfo = (ServiceInfo) pluginIntent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_SERVICEINFO);
        Logger.d(TAG, "getPluginAppendAction() for intent " + pluginIntent + " , serviceInfo = " + serviceInfo);
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
        public int startId;

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

        public int getStartId() {
            return startId;
        }

        public int makeNextStartId() {
            startId++;
            if (startId < 1) {
                startId = 1;
            }
            return startId;
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
//            Logger.d(TAG, "needConnect() " + bindRecord);
//            if (bindRecord != null) {
//                return bindRecord.;
//            } else {
//                return false;
//            }
//        }

        @Override
        public ComponentName getComponentName() throws RemoteException {
            BindRecord bindRecord = getBindRecord();
            Logger.d(TAG, "getComponentName() " + bindRecord);
            if (bindRecord != null) {
                return bindRecord.serviceRecord.componentName;
            } else {
                return null;
            }
        }

        @Override
        public IBinder getBinder() throws RemoteException {
            BindRecord bindRecord = getBindRecord();
            Logger.d(TAG, "getBinder() " + bindRecord);
            if (bindRecord != null) {
                return bindRecord.iBinder;
            } else {
                return null;
            }
        }

        private BindRecord getBindRecord() {
            Logger.d(TAG, "getBindRecord() " + mBindRecord + " , " + mIntent);
            ServiceRecord serviceRecord = fetchCachedServiceRecord(mBindRecord.serviceRecord.serviceInfo);

            if (serviceRecord != null) {
                return serviceRecord.getBindRecord(mIntent);
            } else {
                return null;
            }
        }
    }

}
