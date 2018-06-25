package com.reginald.pluginm.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityGroup;
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
import com.reginald.pluginm.stub.PluginStubLocalActivityManager;
import com.reginald.pluginm.stub.Stubs;
import com.reginald.pluginm.utils.Logger;

import java.lang.ref.WeakReference;
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

    private PluginManager mPluginManager;

    private Intent mLastNewTargetIntent;
    private WeakReference<Activity> mLastNewTargetActivity;

    public HostInstrumentation(PluginManager pluginManager, Instrumentation base) {
        this.mPluginManager = pluginManager;
        this.mBase = base;
    }

    public static Instrumentation install(Context hostContext) {
        Object target = ActivityThreadCompat.currentActivityThread();
        Class ActivityThreadClass = target.getClass();

        try {
            Field mInstrumentationField = FieldUtils.getField(ActivityThreadClass, "mInstrumentation");
            Instrumentation baseInstrumentation = (Instrumentation) FieldUtils.readField(mInstrumentationField, target);
            if (baseInstrumentation instanceof HostInstrumentation) {
                Logger.i(TAG, "HostInstrumentation has already installed!");
                return baseInstrumentation;
            } else {
                Instrumentation newInstrumentation = new HostInstrumentation(
                        PluginManager.getInstance(), baseInstrumentation);
                FieldUtils.writeField(mInstrumentationField, target, newInstrumentation);
                Logger.i(TAG, "HostInstrumentation is now installed!");
                return newInstrumentation;
            }
        } catch (Exception e) {
            Logger.e(TAG, "install() error!", e);
        }

        return null;
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

        Logger.d(TAG, "resolveIntent() intent = " + intent + " -> result = " + resolvedIntent);

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
            Activity activity;
            ActivityInfo activityInfo = intent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_ACTIVITYINFO);
            Logger.d(TAG, "newActivity() target activityInfo = " + activityInfo);
            if (activityInfo != null) {
                PluginInfo pluginInfo = mPluginManager.loadPlugin(activityInfo.packageName);

                if (pluginInfo != null) {
                    try {
                        ComponentName component = new ComponentName(activityInfo.packageName,
                                activityInfo.name);

                        if (activityInfo.targetActivity != null) {
                            component = new ComponentName(activityInfo.packageName,
                                    activityInfo.targetActivity);
                        }

                        activity = mBase.newActivity(pluginInfo.classLoader, component.getClassName(), intent);

                        if (activity instanceof ActivityGroup) {
                            Logger.d(TAG, "replace LocalActivityManager for ActivityGroup " + activity);
                            ActivityGroup ag = (ActivityGroup) activity;
                            FieldUtils.writeField(ag, "mLocalActivityManager",
                                    new PluginStubLocalActivityManager(ag.getLocalActivityManager()));
                        }

                        activity.setIntent(intent);
                        mLastNewTargetIntent = new Intent(intent);
                        mLastNewTargetActivity = new WeakReference<Activity>(activity);
                        try {
                            FieldUtils.writeField(ContextThemeWrapper.class, "mResources", activity, pluginInfo.resources);
                            Logger.d(TAG, "newActivity() replace mResources ok! ");
                        } catch (Exception e) {
                            Logger.e(TAG, "newActivity() replace mResources error! ", e);
                        }
                        return activity;
                    } catch (Exception e) {
                        Logger.e(TAG, "newActivity() load plugin activity error!", e);
                    }
                } else {
                    Logger.e(TAG, "newActivity() no loaded plugininfo found for " + activityInfo);
                }
            }

            Logger.e(TAG, "newActivity() no valid plugin activity loaded for stub " + className + ", load fake activity!");
            Activity fakeActivity = mBase.newActivity(cl, Stubs.Activity.Fake.class.getName(), intent);
            return fakeActivity;
        }

        return mBase.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        Logger.d(TAG, "callActivityOnCreate() activity = " + activity);

        // check if Fake activity
        if (activity instanceof Stubs.Activity.Fake) {
            mBase.callActivityOnCreate(activity, icicle);
            return;
        }

        // fetch intent for target activity
        Intent lastNewTargetIntent = mLastNewTargetIntent;
        mLastNewTargetIntent = null;

        Activity lastNewActivity = null;
        if (mLastNewTargetActivity != null) {
            if (mLastNewTargetActivity.get() != null) {
                lastNewActivity = mLastNewTargetActivity.get();
                mLastNewTargetActivity.clear();
            }
            mLastNewTargetActivity = null;
        }

        Intent intent;
        if (lastNewActivity == activity && lastNewTargetIntent != null) {
            intent = lastNewTargetIntent;
        } else {
            intent = activity.getIntent();
        }

        // fetch tartget and stub activity info
        ActivityInfo activityInfo = lastNewTargetIntent.getParcelableExtra(PluginManager.EXTRA_INTENT_TARGET_ACTIVITYINFO);
        ActivityInfo stubInfo = lastNewTargetIntent.getParcelableExtra(PluginManager.EXTRA_INTENT_STUB_INFO);

        Logger.d(TAG, "callActivityOnCreate() target activityInfo = " + activityInfo);
        if (activityInfo != null && stubInfo != null) {
            PluginInfo pluginInfo = mPluginManager.getLoadedPluginInfo(activityInfo.packageName);

            if (pluginInfo != null) {
                Context pluginContext = mPluginManager.createPluginContext(
                        activityInfo.packageName, activity.getBaseContext());
                try {
                    Context baseContext = activity.getBaseContext();
                    while (baseContext instanceof ContextWrapper) {
                        baseContext = ((ContextWrapper) baseContext).getBaseContext();
                    }
                    Logger.d(TAG, "baseContext = " + baseContext);
                    FieldUtils.writeField(baseContext, "mResources", pluginInfo.resources);
                    FieldUtils.writeField(ContextWrapper.class, "mBase", activity, pluginContext);
                    FieldUtils.writeField(activity, "mApplication", pluginInfo.application);
                    FieldUtils.writeField(ContextThemeWrapper.class, "mBase", activity, pluginContext);
                    Logger.d(TAG, "callActivityOnCreate() replace context ok! ");
                } catch (IllegalAccessException e) {
                    Logger.e(TAG, "callActivityOnCreate() replace context error! ", e);
                }

                try {
                    FieldUtils.writeField(activity, "mInstrumentation", this);
                    Logger.d(TAG, "callActivityOnCreate() replace mInstrumentation ok! ");
                } catch (IllegalAccessException e) {
                    Logger.e(TAG, "callActivityOnCreate() replace mInstrumentation error! ", e);
                }

                // orientation:
                if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        && activityInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    activity.setRequestedOrientation(activityInfo.screenOrientation);
                }

                // theme:
                try {
                    FieldUtils.writeField(activity, "mTheme", null);
                    int themeResId = activityInfo.getThemeResource();
                    if (themeResId != 0) {
                        activity.setTheme(themeResId);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "modify theme error!", e);
                }

                // mActivityInfo
                try {
                    FieldUtils.writeField(Activity.class, "mActivityInfo", activity, activityInfo);
                } catch (Exception e) {
                    Logger.e(TAG, "modify mActivityInfo error!", e);
                }

                // mTitle
                try {
                    FieldUtils.writeField(Activity.class, "mTitle", activity,
                            activityInfo.loadLabel(pluginContext.getPackageManager()));
                } catch (Exception e) {
                    Logger.e(TAG, "modify mActivityInfo error!", e);
                }

                activity.setIntent(PluginManagerService.recoverOriginalIntent(intent, activity.getClassLoader()));
                mPluginManager.callActivityOnCreate(activity, stubInfo, activityInfo);
            }
        }

        mBase.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callActivityOnNewIntent(Activity activity, Intent intent) {
        Intent origIntent = PluginManagerService.recoverOriginalIntent(intent, activity.getClassLoader());
        mBase.callActivityOnNewIntent(activity, origIntent);
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        mPluginManager.callActivityOnDestory(activity);

        mBase.callActivityOnDestroy(activity);
    }

}
