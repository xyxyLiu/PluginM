package com.reginald.pluginm.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.view.ContextThemeWrapper;

import com.android.common.ActivityThreadCompat;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.stub.Stubs;
import com.reginald.pluginm.utils.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by lxy on 17-8-9.
 */
public class HostInstrumentation extends Instrumentation {
    public static final String TAG = "HostInstrumentation";

    private static Class<?> sIAppTaskClazz;

    private Instrumentation mBase;

    PluginManager mPluginManager;

    public static Instrumentation install(Context hostContext) {
        Object target = ActivityThreadCompat.currentActivityThread();
        Class ActivityThreadClass = target.getClass();

        try {
        /*替换ActivityThread.mH.mCallback，拦截组件调度消息*/
            Field mInstrumentationField = FieldUtils.getField(ActivityThreadClass, "mInstrumentation");
            Instrumentation baseInstrumentation = (Instrumentation) FieldUtils.readField(mInstrumentationField, target);
            Instrumentation newInstrumentation = new HostInstrumentation(
                    PluginManager.getInstance(hostContext), baseInstrumentation);
            FieldUtils.writeField(mInstrumentationField, target, newInstrumentation);
            Logger.i(TAG, "HostInstrumentation has installed!");
            return newInstrumentation;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public HostInstrumentation(PluginManager pluginManager, Instrumentation base) {
        this.mPluginManager = pluginManager;
        this.mBase = base;
    }

    /**
     * public ActivityResult execStartActivity(
     * Context who, IBinder contextThread, IBinder token, Activity target,
     * Intent intent, int requestCode, Bundle options)
     *
     * public void execStartActivities(Context who, IBinder contextThread,
     * IBinder token, Activity target, Intent[] intents, Bundle options);
     *
     * public void execStartActivitiesAsUser(Context who, IBinder contextThread,
     * IBinder token, Activity target, Intent[] intents, Bundle options,
     * int userId);
     *
     * public ActivityResult execStartActivity(
     * Context who, IBinder contextThread, IBinder token, Fragment target,
     * Intent intent, int requestCode, Bundle options);
     *
     * public ActivityResult execStartActivity(
     * Context who, IBinder contextThread, IBinder token, Activity target,
     * Intent intent, int requestCode, Bundle options, UserHandle user);
     *
     * public ActivityResult execStartActivityAsCaller(
     * Context who, IBinder contextThread, IBinder token, Activity target,
     * Intent intent, int requestCode, Bundle options, int userId);
     *
     * public void execStartActivityFromAppTask(
     * Context who, IBinder contextThread, IAppTask appTask,
     * Intent intent, Bundle options);
     *
     *
     * // 4.0.4
     *
     * public ActivityResult execStartActivity(
     * Context who, IBinder contextThread, IBinder token, Activity target,
     * Intent intent, int requestCode)
     *
     * public void execStartActivities(Context who, IBinder contextThread,
     * IBinder token, Activity target, Intent[] intents);
     *
     * public ActivityResult execStartActivity(
     * Context who, IBinder contextThread, IBinder token, Fragment target,
     * Intent intent, int requestCode);
     */


    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        Intent resolvedIntent = resolveIntent(intent);

        ActivityResult result = (ActivityResult) MethodUtils.invokeMethodNoThrow(mBase, "execStartActivity",
                new Object[]{who, contextThread, token, target, resolvedIntent, requestCode, options},
                new Class[]{Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class}
        );

        return result;

    }

    public void execStartActivitiesAsUser(Context who, IBinder contextThread,
            IBinder token, Activity target, Intent[] intents, Bundle options,
            int userId) {

        Intent[] resolvedIntents = resolveIntents(intents);

        MethodUtils.invokeMethodNoThrow(mBase, "execStartActivitiesAsUser",
                new Object[]{who, contextThread, token, target, resolvedIntents, options, userId},
                new Class[]{Context.class, IBinder.class, IBinder.class, Activity.class, Intent[].class, Bundle.class, int.class}
        );
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Fragment target,
            Intent intent, int requestCode, Bundle options) {
        Intent resolvedIntent = resolveIntent(intent);

        ActivityResult result = (ActivityResult) MethodUtils.invokeMethodNoThrow(mBase, "execStartActivity",
                new Object[]{who, contextThread, token, target, resolvedIntent, requestCode, options},
                new Class[]{Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class, int.class, Bundle.class}
        );

        return result;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options, UserHandle user) {
        Intent resolvedIntent = resolveIntent(intent);

        ActivityResult result = (ActivityResult) MethodUtils.invokeMethodNoThrow(mBase, "execStartActivity",
                new Object[]{who, contextThread, token, target, resolvedIntent, requestCode, options, user},
                new Class[]{Context.class, IBinder.class, IBinder.class, Fragment.class,
                        Intent.class, int.class, Bundle.class, UserHandle.class}
        );

        return result;
    }

    public ActivityResult execStartActivityAsCaller(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options, int userId) {
        Intent resolvedIntent = resolveIntent(intent);

        ActivityResult result = (ActivityResult) MethodUtils.invokeMethodNoThrow(mBase, "execStartActivityAsCaller",
                new Object[]{who, contextThread, token, target, resolvedIntent, requestCode, options, userId},
                new Class[]{Context.class, IBinder.class, IBinder.class, Fragment.class,
                        Intent.class, int.class, Bundle.class, int.class}
        );

        return result;
    }

    public void execStartActivityFromAppTask(
            Context who, IBinder contextThread, Object appTask,
            Intent intent, Bundle options) {
        Intent resolvedIntent = resolveIntent(intent);

        if (sIAppTaskClazz == null) {
            try {
                sIAppTaskClazz = Class.forName("android.app.IAppTask");
            } catch (ClassNotFoundException e) {
                Logger.e(TAG, "execStartActivityFromAppTask() android.app.IAppTask NOT found!", e);
            }
        }

        MethodUtils.invokeMethodNoThrow(mBase, "execStartActivityFromAppTask",
                new Object[]{who, contextThread, appTask, resolvedIntent, options},
                new Class[]{Context.class, IBinder.class, sIAppTaskClazz, Intent.class, Bundle.class}
        );
    }


    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode) {
        Intent resolvedIntent = resolveIntent(intent);

        ActivityResult result = (ActivityResult) MethodUtils.invokeMethodNoThrow(mBase, "execStartActivity",
                new Object[]{who, contextThread, token, target, resolvedIntent, requestCode},
                new Class[]{Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class}
        );

        return result;
    }

    public void execStartActivities(Context who, IBinder contextThread,
            IBinder token, Activity target, Intent[] intents) {
        Intent[] resolvedIntents = resolveIntents(intents);

        MethodUtils.invokeMethodNoThrow(mBase, "execStartActivities",
                new Object[]{who, contextThread, token, target, resolvedIntents},
                new Class[]{Context.class, IBinder.class, IBinder.class, Activity.class, Intent[].class}
        );
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Fragment target,
            Intent intent, int requestCode) {
        Intent resolvedIntent = resolveIntent(intent);

        ActivityResult result = (ActivityResult) MethodUtils.invokeMethodNoThrow(mBase, "execStartActivity",
                new Object[]{who, contextThread, token, target, resolvedIntent, requestCode},
                new Class[]{Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class, int.class}
        );

        return result;
    }

    private Intent[] resolveIntents(Intent[] intents) {
        if (intents == null) {
            return null;
        }

        Intent[] resolvedIntents = new Intent[intents.length];

        for (int i = 0; i < intents.length; i++) {
            resolvedIntents[i] = resolveIntent(intents[i]);
        }

        return resolvedIntents;
    }

    private Intent resolveIntent(Intent intent) {
        if (intent == null) {
            return null;
        }

        Intent resolvedIntent = mPluginManager.getPluginActivityIntent(intent);

        if (resolvedIntent == null) {
            resolvedIntent = intent;
        }

        return resolvedIntent;
    }

    private ActivityResult realExecStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        ActivityResult result = null;
        try {
            Method realExecStartActivityMethod = Instrumentation.class
                    .getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class,
                            Activity.class, Intent.class, int.class, Bundle.class);
            realExecStartActivityMethod.setAccessible(true);
            result = (ActivityResult) realExecStartActivityMethod.invoke(mBase, who, contextThread, token,
                    target, intent, requestCode, options);
        } catch (NoSuchMethodException e) {
            Logger.e(TAG, "realExecStartActivity error!", e);
        } catch (IllegalAccessException e) {
            Logger.e(TAG, "realExecStartActivity error!", e);
        } catch (InvocationTargetException e) {
            Logger.e(TAG, "realExecStartActivity error!", e);
        }

        return result;
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Logger.d(TAG, "newActivity() className = " + className);
        if (className.startsWith(Stubs.Activity.class.getName())) {
            ActivityInfo activityInfo = intent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_ACTIVITYINFO);
            Logger.d(TAG, "newActivity() target activityInfo = " + activityInfo);
            if (activityInfo != null) {
                PluginInfo pluginInfo = mPluginManager.getLoadedPluginInfo(activityInfo.packageName);
                Activity activity = mBase.newActivity(pluginInfo.classLoader, activityInfo.name, intent);
                activity.setIntent(intent);
                try {
                    FieldUtils.writeField(ContextThemeWrapper.class, "mResources", activity, pluginInfo.resources);
                    Logger.d(TAG, "newActivity() replace mResources ok! ");
                } catch (Exception e) {
                    Logger.e(TAG, "newActivity() replace mResources error! ");
                    e.printStackTrace();
                }

                return activity;
            }
        }

        return mBase.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        Logger.d(TAG, "callActivityOnCreate() activity = " + activity);
        Intent intent = activity.getIntent();
        ActivityInfo activityInfo = intent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_ACTIVITYINFO);
        Logger.d(TAG, "callActivityOnCreate() target activityInfo = " + activityInfo);
        if (activityInfo != null) {
            PluginInfo pluginInfo = mPluginManager.getLoadedPluginInfo(activityInfo.packageName);
            Context pluginContext = mPluginManager.createPluginContext(
                    activityInfo.packageName, activity.getBaseContext());
            try {
                FieldUtils.writeField(activity.getBaseContext().getClass(), "mResources", activity.getBaseContext(), pluginInfo.resources);
                FieldUtils.writeField(ContextWrapper.class, "mBase", activity, pluginContext);
                FieldUtils.writeField(activity, "mTheme", pluginContext.getTheme());
                FieldUtils.writeField(activity, "mApplication", pluginInfo.application);
                FieldUtils.writeField(ContextThemeWrapper.class, "mBase", activity, pluginContext);
                Logger.d(TAG, "callActivityOnCreate() replace context ok! ");
            } catch (IllegalAccessException e) {
                Logger.e(TAG, "callActivityOnCreate() replace context error! ", e);
            }
            activity.setIntent(PluginManagerService.recoverOriginalIntent(intent, activity.getClassLoader()));
        }

        mBase.callActivityOnCreate(activity, icicle);
    }

    @Override
    public Context getContext() {
        return mBase.getContext();
    }

    @Override
    public Context getTargetContext() {
        return mBase.getTargetContext();
    }

    @Override
    public ComponentName getComponentName() {
        return mBase.getComponentName();
    }

}
