package com.reginald.pluginm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.android.common.ActivityThreadCompat;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.stub.Stubs;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class HostHCallback {

    private static final String TAG = "HostHCallback";
    private static List<Handler.Callback> sCallbacks = new ArrayList<>(1);


    public static boolean install(Context hostContext) {
        Object target = ActivityThreadCompat.currentActivityThread();
        Class ActivityThreadClass = target.getClass();

        try {
        /*替换ActivityThread.mH.mCallback，拦截组件调度消息*/
            Field mHField = FieldUtils.getField(ActivityThreadClass, "mH");
            Handler handler = null;

            handler = (Handler) FieldUtils.readField(mHField, target);

            Field mCallbackField = FieldUtils.getField(Handler.class, "mCallback");
            //*这里读取出旧的callback并处理*/
            Object mCallback = FieldUtils.readField(mCallbackField, handler);
            if (!PluginCallback.class.isInstance(mCallback)) {
                PluginCallback pluginCallback = mCallback != null ? new PluginCallback(hostContext, handler, (Handler.Callback) mCallback) : new PluginCallback(hostContext, handler, null);
                pluginCallback.setEnable(true);
                sCallbacks.add(pluginCallback);
                FieldUtils.writeField(mCallbackField, handler, pluginCallback);
                Log.i(TAG, "HostHCallback has installed!");
            } else {
                Log.i(TAG, "HostHCallback has installed,skip");
            }
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return false;
    }


    public static class PluginCallback implements Handler.Callback {

        private static final String TAG = "PluginCallback";

        public static final int LAUNCH_ACTIVITY = 100;
        public static final int PAUSE_ACTIVITY = 101;
        public static final int PAUSE_ACTIVITY_FINISHING = 102;
        public static final int STOP_ACTIVITY_SHOW = 103;
        public static final int STOP_ACTIVITY_HIDE = 104;
        public static final int SHOW_WINDOW = 105;
        public static final int HIDE_WINDOW = 106;
        public static final int RESUME_ACTIVITY = 107;
        public static final int SEND_RESULT = 108;
        public static final int DESTROY_ACTIVITY = 109;
        public static final int BIND_APPLICATION = 110;
        public static final int EXIT_APPLICATION = 111;
        public static final int NEW_INTENT = 112;
        public static final int RECEIVER = 113;
        public static final int CREATE_SERVICE = 114;
        public static final int SERVICE_ARGS = 115;
        public static final int STOP_SERVICE = 116;
        public static final int REQUEST_THUMBNAIL = 117;
        public static final int CONFIGURATION_CHANGED = 118;
        public static final int CLEAN_UP_CONTEXT = 119;
        public static final int GC_WHEN_IDLE = 120;
        public static final int BIND_SERVICE = 121;
        public static final int UNBIND_SERVICE = 122;
        public static final int DUMP_SERVICE = 123;
        public static final int LOW_MEMORY = 124;
        public static final int ACTIVITY_CONFIGURATION_CHANGED = 125;
        public static final int RELAUNCH_ACTIVITY = 126;
        public static final int PROFILER_CONTROL = 127;
        public static final int CREATE_BACKUP_AGENT = 128;
        public static final int DESTROY_BACKUP_AGENT = 129;
        public static final int SUICIDE = 130;
        public static final int REMOVE_PROVIDER = 131;
        public static final int ENABLE_JIT = 132;
        public static final int DISPATCH_PACKAGE_BROADCAST = 133;
        public static final int SCHEDULE_CRASH = 134;
        public static final int DUMP_HEAP = 135;
        public static final int DUMP_ACTIVITY = 136;
        public static final int SLEEPING = 137;
        public static final int SET_CORE_SETTINGS = 138;
        public static final int UPDATE_PACKAGE_COMPATIBILITY_INFO = 139;
        public static final int TRIM_MEMORY = 140;
        public static final int DUMP_PROVIDER = 141;
        public static final int UNSTABLE_PROVIDER_DIED = 142;
        public static final int REQUEST_ASSIST_CONTEXT_EXTRAS = 143;
        public static final int TRANSLUCENT_CONVERSION_COMPLETE = 144;
        public static final int INSTALL_PROVIDER = 145;
        public static final int ON_NEW_ACTIVITY_OPTIONS = 146;
        public static final int CANCEL_VISIBLE_BEHIND = 147;
        public static final int BACKGROUND_VISIBLE_BEHIND_CHANGED = 148;
        public static final int ENTER_ANIMATION_COMPLETE = 149;

        String codeToString(int code) {
            switch (code) {
                case LAUNCH_ACTIVITY:
                    return "LAUNCH_ACTIVITY";
                case PAUSE_ACTIVITY:
                    return "PAUSE_ACTIVITY";
                case PAUSE_ACTIVITY_FINISHING:
                    return "PAUSE_ACTIVITY_FINISHING";
                case STOP_ACTIVITY_SHOW:
                    return "STOP_ACTIVITY_SHOW";
                case STOP_ACTIVITY_HIDE:
                    return "STOP_ACTIVITY_HIDE";
                case SHOW_WINDOW:
                    return "SHOW_WINDOW";
                case HIDE_WINDOW:
                    return "HIDE_WINDOW";
                case RESUME_ACTIVITY:
                    return "RESUME_ACTIVITY";
                case SEND_RESULT:
                    return "SEND_RESULT";
                case DESTROY_ACTIVITY:
                    return "DESTROY_ACTIVITY";
                case BIND_APPLICATION:
                    return "BIND_APPLICATION";
                case EXIT_APPLICATION:
                    return "EXIT_APPLICATION";
                case NEW_INTENT:
                    return "NEW_INTENT";
                case RECEIVER:
                    return "RECEIVER";
                case CREATE_SERVICE:
                    return "CREATE_SERVICE";
                case SERVICE_ARGS:
                    return "SERVICE_ARGS";
                case STOP_SERVICE:
                    return "STOP_SERVICE";
                case CONFIGURATION_CHANGED:
                    return "CONFIGURATION_CHANGED";
                case CLEAN_UP_CONTEXT:
                    return "CLEAN_UP_CONTEXT";
                case GC_WHEN_IDLE:
                    return "GC_WHEN_IDLE";
                case BIND_SERVICE:
                    return "BIND_SERVICE";
                case UNBIND_SERVICE:
                    return "UNBIND_SERVICE";
                case DUMP_SERVICE:
                    return "DUMP_SERVICE";
                case LOW_MEMORY:
                    return "LOW_MEMORY";
                case ACTIVITY_CONFIGURATION_CHANGED:
                    return "ACTIVITY_CONFIGURATION_CHANGED";
                case RELAUNCH_ACTIVITY:
                    return "RELAUNCH_ACTIVITY";
                case PROFILER_CONTROL:
                    return "PROFILER_CONTROL";
                case CREATE_BACKUP_AGENT:
                    return "CREATE_BACKUP_AGENT";
                case DESTROY_BACKUP_AGENT:
                    return "DESTROY_BACKUP_AGENT";
                case SUICIDE:
                    return "SUICIDE";
                case REMOVE_PROVIDER:
                    return "REMOVE_PROVIDER";
                case ENABLE_JIT:
                    return "ENABLE_JIT";
                case DISPATCH_PACKAGE_BROADCAST:
                    return "DISPATCH_PACKAGE_BROADCAST";
                case SCHEDULE_CRASH:
                    return "SCHEDULE_CRASH";
                case DUMP_HEAP:
                    return "DUMP_HEAP";
                case DUMP_ACTIVITY:
                    return "DUMP_ACTIVITY";
                case SLEEPING:
                    return "SLEEPING";
                case SET_CORE_SETTINGS:
                    return "SET_CORE_SETTINGS";
                case UPDATE_PACKAGE_COMPATIBILITY_INFO:
                    return "UPDATE_PACKAGE_COMPATIBILITY_INFO";
                case TRIM_MEMORY:
                    return "TRIM_MEMORY";
                case DUMP_PROVIDER:
                    return "DUMP_PROVIDER";
                case UNSTABLE_PROVIDER_DIED:
                    return "UNSTABLE_PROVIDER_DIED";
                case REQUEST_ASSIST_CONTEXT_EXTRAS:
                    return "REQUEST_ASSIST_CONTEXT_EXTRAS";
                case TRANSLUCENT_CONVERSION_COMPLETE:
                    return "TRANSLUCENT_CONVERSION_COMPLETE";
                case INSTALL_PROVIDER:
                    return "INSTALL_PROVIDER";
                case ON_NEW_ACTIVITY_OPTIONS:
                    return "ON_NEW_ACTIVITY_OPTIONS";
                case CANCEL_VISIBLE_BEHIND:
                    return "CANCEL_VISIBLE_BEHIND";
                case BACKGROUND_VISIBLE_BEHIND_CHANGED:
                    return "BACKGROUND_VISIBLE_BEHIND_CHANGED";
                case ENTER_ANIMATION_COMPLETE:
                    return "ENTER_ANIMATION_COMPLETE";
            }
            return Integer.toString(code);
        }


        private Handler mOldHandle = null;
        private Handler.Callback mCallback = null;
        private Context mHostContext;

        private boolean mEnable = false;

        public PluginCallback(Context hostContext, Handler oldHandle, Handler.Callback callback) {
            mOldHandle = oldHandle;
            mCallback = callback;
            mHostContext = hostContext;
        }

        public void setEnable(boolean enable) {
            this.mEnable = enable;
        }

        public boolean isEnable() {
            return mEnable;
        }

        @Override
        public boolean handleMessage(Message msg) {
            long b = System.currentTimeMillis();
            try {
                if (!mEnable) {
                    return false;
                }

//                if (PluginProcessManager.isPluginProcess(mHostContext)) {
//                    if (!PluginManager.getInstance().isConnected()) {
//                        //这里必须要这么做。如果当前进程是插件进程，并且，还没有绑定上插件管理服务，我们则把消息延迟一段时间再处理。
//                        //这样虽然会降低启动速度，但是可以解决在没绑定服务就启动，会导致的一系列时序问题。
//                        Log.i(TAG, "handleMessage not isConnected post and wait,msg=%s", msg);
//                        mOldHandle.sendMessageDelayed(Message.obtain(msg), 5);
//                        //返回true，告诉下面的handle不要处理了。
//                        return true;
//                    }
//                }

                if (msg.what == LAUNCH_ACTIVITY) {
                    handleLaunchActivity(msg);
                } else if (msg.what == CREATE_SERVICE) {
                    handleCreateService(msg);
                }
                /*else if (msg.what == INSTALL_PROVIDER) {
                return handleInstallProvider(msg);
            } else if (msg.what == CREATE_BACKUP_AGENT) {
                //TODO 处理CREATE_BACKUP_AGENT
            } else if (msg.what == DESTROY_BACKUP_AGENT) {
                //TODO 处理DESTROY_BACKUP_AGENT
            }  else if (msg.what == BIND_SERVICE) {
    //            return handleBindService(msg);
            } else if (msg.what == UNBIND_SERVICE) {
    //            return handleUnbindService(msg);
            } else if (msg.what == SERVICE_ARGS) {
    //            return handleServiceArgs(msg);
            }*/
                if (mCallback != null) {
                    return mCallback.handleMessage(msg);
                } else {
                    return false;
                }
            } finally {
                Log.i(TAG, String.format("handleMessage(%s,%s) cost %s ms", msg.what, codeToString(msg.what), (System.currentTimeMillis() - b)));

            }
        }

//    private boolean handleServiceArgs(Message msg) {
//        // handleServiceArgs((ServiceArgsData)msg.obj);
//        try {
//            Object obj = msg.obj;
//            Intent intent = (Intent) FieldUtils.readField(obj, "args", true);
//            if (intent != null) {
//                intent.setExtrasClassLoader(getClass().getClassLoader());
//                Intent originPluginIntent = intent.getParcelableExtra(Env.EXTRA_TARGET_INTENT);
//                if (originPluginIntent != null) {
//                    FieldUtils.writeDeclaredField(msg.obj, "args", originPluginIntent, true);
//                    Log.i(TAG, "handleServiceArgs OK");
//                } else {
//                    Log.w(TAG, "handleServiceArgs pluginInfo==null");
//                }
//            }
//
//        } catch (Exception e) {
//            Log.e(TAG, "handleServiceArgs", e);
//        }
//        return false;
//    }
//
//    private boolean handleUnbindService(Message msg) {
//        //  handleUnbindService((BindServiceData)msg.obj);
//        try {
//            Object obj = msg.obj;
//            Intent intent = (Intent) FieldUtils.readField(obj, "intent", true);
//            intent.setExtrasClassLoader(getClass().getClassLoader());
//            Intent originPluginIntent = intent.getParcelableExtra(Env.EXTRA_TARGET_INTENT);
//            if (originPluginIntent != null) {
//                FieldUtils.writeDeclaredField(msg.obj, "intent", originPluginIntent, true);
//                Log.i(TAG, "handleUnbindService OK");
//            } else {
//                Log.w(TAG, "handleUnbindService pluginInfo==null");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "handleUnbindService", e);
//        }
//        return false;
//    }
//
//    private boolean handleBindService(Message msg) {
//        // handleBindService((BindServiceData)msg.obj);
//        //其实这里什么都不用做的。
//        try {
//            Object obj = msg.obj;
//            Intent intent = (Intent) FieldUtils.readField(obj, "intent", true);
//            intent.setExtrasClassLoader(getClass().getClassLoader());
//            Intent originPluginIntent = intent.getParcelableExtra(Env.EXTRA_TARGET_INTENT);
//            if (originPluginIntent != null) {
//
//
//                FieldUtils.writeDeclaredField(msg.obj, "intent", originPluginIntent, true);
//                Log.i(TAG, "handleBindService OK");
//            } else {
//                Log.w(TAG, "handleBindService pluginInfo==null");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "handleBindService", e);
//        }
//        return false;
//    }
//
//    private boolean handleCreateService(Message msg) {
//        // handleCreateService((CreateServiceData)msg.obj);
//        try {
//            Object obj = msg.obj;
//            ServiceInfo info = (ServiceInfo) FieldUtils.readField(obj, "info", true);
//            if (info != null) {
//                ServiceInfo newServiceInfo = PluginManager.getInstance().getTargetServiceInfo(info);
//                if (newServiceInfo != null) {
//                    FieldUtils.writeDeclaredField(msg.obj, "info", newServiceInfo, true);
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "handleCreateService", e);
//        }
//        return false;
//    }

        private void handleLaunchActivity(Message msg) {
            Log.d(TAG, "handleLaunchActivity() msg = " + msg);

            Object obj = msg.obj;
            Intent stubIntent = null;
            ActivityInfo rawActivityInfo = null;
            try {
                stubIntent = (Intent) FieldUtils.readField(obj, "intent");
                rawActivityInfo = (ActivityInfo) FieldUtils.readField(obj, "activityInfo");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }

            if (stubIntent == null) {
                return;
            }

            Log.d(TAG, "handleLaunchActivity() stubIntent = " + stubIntent);
            String proxyClassName = stubIntent.getComponent().getClassName();
            ActivityInfo targetActivityInfo = stubIntent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_ACTIVITYINFO);

            Log.d(TAG, String.format("handleLaunchActivity() proxyClassName = %s, targetActivityInfo = %s", proxyClassName, targetActivityInfo));

            if (targetActivityInfo == null) {
                return;
            }

            PluginInfo loadedPluginInfo = PluginManager.getInstance(mHostContext).loadPlugin(targetActivityInfo.applicationInfo);

            if (loadedPluginInfo != null && proxyClassName.startsWith(Stubs.Activity.class.getName())) {
                PluginManager.getInstance(mHostContext).registerActivity(targetActivityInfo);
            }

            Log.d(TAG, "handleLaunchActivity() rawActivityInfo = " + rawActivityInfo);
            if (rawActivityInfo != null) {
                rawActivityInfo.theme = targetActivityInfo.theme != 0 ? targetActivityInfo.theme :
                        targetActivityInfo.applicationInfo.theme;
                Log.d(TAG, "handleLaunchActivity() set theme = " + rawActivityInfo.theme);
            }
        }


        //test
        private void handleCreateService(Message msg) {
            Log.d(TAG, "handleCreateService() msg = " + msg);

            Object obj = msg.obj;
            IBinder token = null;
            try {
                token = (IBinder) FieldUtils.readField(obj, "token");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }

            Log.d(TAG, "handleCreateService() server token = " + token);

        }


    }
}