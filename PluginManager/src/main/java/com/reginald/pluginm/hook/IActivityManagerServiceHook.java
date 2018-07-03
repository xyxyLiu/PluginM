package com.reginald.pluginm.hook;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.reflect.Utils;
import com.reginald.pluginm.stub.PluginStubMainService;
import com.reginald.pluginm.utils.Logger;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Created by lxy on 17-8-16.
 */

public class IActivityManagerServiceHook extends ServiceHook {
    private static final String TAG = "IActivityManagerHook";
    private PluginManager mPluginManager;

    public IActivityManagerServiceHook(Context context) {
        mPluginManager = PluginManager.getInstance();
    }

    public static boolean init(Context context) {
        IActivityManagerServiceHook hook = new IActivityManagerServiceHook(context);
        return hook.install();
    }

    @Override
    public boolean install() {
        Logger.d(TAG, "install() ");
        // hook
        try {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Class activityManagerNativeCls = Class.forName("android.app.ActivityManagerNative");
                Class iActivityManagerCls = Class.forName("android.app.IActivityManager");
                Class singletonCls = Class.forName("android.util.Singleton");
                Object targetObj = FieldUtils.readStaticField(activityManagerNativeCls, "gDefault");

                if (targetObj != null) {
                    if (iActivityManagerCls.isInstance(targetObj)) {
                        List<Class<?>> interfaces = Utils.getAllInterfaces(targetObj.getClass());
                        Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces
                                .toArray(new Class[interfaces.size()]) : new Class[0];
                        Object proxiedActivityManager =
                                Proxy.newProxyInstance(targetObj.getClass().getClassLoader(), ifs, this);
                        FieldUtils.writeStaticField(activityManagerNativeCls, "gDefault", proxiedActivityManager);
                        mBase = targetObj;
                    } else if (singletonCls.isInstance(targetObj)) {
                        Object iActivityManagerObj = FieldUtils.readField(targetObj, "mInstance");
                        List<Class<?>> interfaces = Utils.getAllInterfaces(iActivityManagerObj.getClass());
                        Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces
                                .toArray(new Class[interfaces.size()]) : new Class[0];
                        Object proxiedActivityManager =
                                Proxy.newProxyInstance(iActivityManagerObj.getClass().getClassLoader(), ifs, this);
                        FieldUtils.writeField(targetObj, "mInstance", proxiedActivityManager);
                        mBase = iActivityManagerObj;
                    }
                }
            } else {
                Object targetObj = FieldUtils.readStaticField(ActivityManager.class, "IActivityManagerSingleton");
                Object iActivityManagerObj = FieldUtils.readField(targetObj, "mInstance");
                List<Class<?>> interfaces = Utils.getAllInterfaces(iActivityManagerObj.getClass());
                Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces
                        .toArray(new Class[interfaces.size()]) : new Class[0];
                Object proxiedActivityManager =
                        Proxy.newProxyInstance(iActivityManagerObj.getClass().getClassLoader(), ifs, this);
                FieldUtils.writeField(targetObj, "mInstance", proxiedActivityManager);
                mBase = iActivityManagerObj;
            }


        } catch (Exception e) {
            Logger.e(TAG, "install() hook error! ", e);
            return false;
        }
        Logger.d(TAG, "install() hook ok!");

        // register method handlers
        addMethodHandler(new getIntentSender());
        addMethodHandler(new stopServiceToken());
        addMethodHandler(new overridePendingTransition());
        addMethodHandler(new setServiceForeground());
        addMethodHandler(new getRunningAppProcesses());

        Logger.d(TAG, "install() install ok!");
        return true;
    }

    /**
     * public IIntentSender getIntentSender(int type,String packageName, IBinder token, String resultWho,
     * int requestCode, Intent[] intents, String[] resolvedTypes,int flags, Bundle bOptions, int userId);
     */
    private class getIntentSender extends ServiceHook.MethodHandler {
        /**
         * Type for IActivityManaqer.getIntentSender: this PendingIntent is
         * for a sendBroadcast operation.
         *
         * @hide
         */
        public static final int INTENT_SENDER_BROADCAST = 1;

        /**
         * Type for IActivityManaqer.getIntentSender: this PendingIntent is
         * for a startActivity operation.
         *
         * @hide
         */
        public static final int INTENT_SENDER_ACTIVITY = 2;

        /**
         * Type for IActivityManaqer.getIntentSender: this PendingIntent is
         * for an activity result operation.
         *
         * @hide
         */
        public static final int INTENT_SENDER_ACTIVITY_RESULT = 3;

        /**
         * Type for IActivityManaqer.getIntentSender: this PendingIntent is
         * for a startService operation.
         *
         * @hide
         */
        public static final int INTENT_SENDER_SERVICE = 4;

        @Override
        public String getName() {
            return "getIntentSender";
        }

        public Object onStartInvoke(Object receiver, Method method, Object[] args) {
            int type = (int) args[0];
            Intent pluginIntent = ((Intent[]) args[5])[0];
            Intent newIntent = null;
            args[1] = mPluginManager.getHostContext().getPackageName();

            Logger.d(TAG, "getIntentSender() onStartInvoke : pluginIntent = " + pluginIntent);

            switch (type) {
                case INTENT_SENDER_ACTIVITY:
                    newIntent = mPluginManager.getPluginActivityIntent(pluginIntent);
                    break;
                case INTENT_SENDER_SERVICE:
                    newIntent = mPluginManager.getPluginServiceIntent(pluginIntent);
                    if (newIntent != null) {
                        newIntent.putExtra(PluginStubMainService.INTENT_EXTRA_START_TYPE_KEY,
                                PluginStubMainService.INTENT_EXTRA_START_TYPE_START);
                    }
                    break;
            }

            Logger.d(TAG, "getIntentSender() onStartInvoke : newIntent = " + newIntent);

            if (newIntent != null) {
                ((Intent[]) args[5])[0] = newIntent;
            }

            return super.onStartInvoke(receiver, method, args);
        }
    }

    /**
     * public boolean stopServiceToken(ComponentName className, IBinder token, int startId)
     */
    private class stopServiceToken extends ServiceHook.MethodHandler {

        @Override
        public String getName() {
            return "stopServiceToken";
        }

        public Object onStartInvoke(Object receiver, Method method, Object[] args) {
            ComponentName component = (ComponentName) args[0];
            int startId = (int) args[2];
            Logger.d(TAG, "stopServiceToken() onStartInvoke : component = " + component);
            Intent pluginIntent = new Intent();
            pluginIntent.setComponent(component);
            Intent newIntent = mPluginManager.getPluginServiceIntent(pluginIntent);
            Logger.d(TAG, "stopServiceToken() onStartInvoke : newIntent = " + newIntent);
            if (newIntent != null) {
                newIntent.putExtra(PluginStubMainService.INTENT_EXTRA_START_TYPE_KEY,
                        PluginStubMainService.INTENT_EXTRA_START_TYPE_STOP);
                newIntent.putExtra(PluginStubMainService.INTENT_EXTRA_START_TYPE_STARTID, startId);
                mPluginManager.getHostContext().startService(newIntent);
                return true;
            } else {
                return super.onStartInvoke(receiver, method, args);
            }
        }
    }

    /**
     * public void setServiceForeground(ComponentName className, IBinder token,
     * int id, Notification notification, int flags) throws RemoteException;
     */
    private class setServiceForeground extends ServiceHook.MethodHandler {

        @Override
        public String getName() {
            return "setServiceForeground";
        }

        public Object onStartInvoke(Object receiver, Method method, Object[] args) {
            ComponentName component = (ComponentName) args[0];
            Logger.d(TAG, "setServiceForeground() component = " + component);
            if (component != null) {
                ComponentName newComponentName = mPluginManager.getStubServiceComponent(component);
                if (newComponentName != null) {
                    args[0] = newComponentName;
                } else {
                    Logger.e(TAG, "setServiceForeground() can not found stub service info!");
                }
            }
            return super.onStartInvoke(receiver, method, args);
        }
    }

    /**
     * public void overridePendingTransition(IBinder token, String packageName, int enterAnim, int exitAnim)
     */
    private class overridePendingTransition extends ServiceHook.MethodHandler {

        @Override
        public String getName() {
            return "overridePendingTransition";
        }

        public Object onStartInvoke(Object receiver, Method method, Object[] args) {
            int enterAnim = (int) args[2];
            int exitAnim = (int) args[3];
            Logger.d(TAG, "overridePendingTransition() onStartInvoke : enterAnim = " +
                    enterAnim + " , exitAnim = " + exitAnim);
            return null;
        }
    }

    /**
     * public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses()
     * throws RemoteException;
     */
    private class getRunningAppProcesses extends ServiceHook.MethodHandler {

        @Override
        public String getName() {
            return "getRunningAppProcesses";
        }

        public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
            Logger.d(TAG, "getRunningAppProcesses()");
            if (invokeResult != null && invokeResult instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> infos = (List<Object>) invokeResult;
                if (infos.size() > 0) {
                    for (Object info : infos) {
                        if (info instanceof ActivityManager.RunningAppProcessInfo) {
                            ActivityManager.RunningAppProcessInfo myInfo = (ActivityManager.RunningAppProcessInfo) info;
                            if (myInfo.uid != android.os.Process.myUid()) {
                                continue;
                            }
                            String processname = PluginManager.getInstance().getPluginProcessName(myInfo.pid);
                            if (processname != null) {
                                Logger.d(TAG, String.format("getRunningAppProcesses() map processName %s -> %s", myInfo
                                        .processName, processname));
                                myInfo.processName = processname;
                            }
                        }
                    }
                }
            }
            return invokeResult;
        }
    }

}
