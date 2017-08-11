package com.reginald.pluginm;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.android.common.ActivityThreadCompat;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.stub.ActivityStub;
import com.reginald.pluginm.stub.StubManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import pluginm.reginald.com.pluginlib.PluginHelper;

/**
 * Created by lxy on 17-8-9.
 */
public class HostInstrumentation extends Instrumentation {
    public static final String TAG = "HostInstrumentation";

    private Instrumentation mBase;

    PluginManager mPluginManager;

    public static boolean onInstall(Context hostContext) {
        Object target = ActivityThreadCompat.currentActivityThread();
        Class ActivityThreadClass = target.getClass();

        try {
        /*替换ActivityThread.mH.mCallback，拦截组件调度消息*/
            Field mInstrumentationField = FieldUtils.getField(ActivityThreadClass, "mInstrumentation");
            Instrumentation baseInstrumentation = (Instrumentation) FieldUtils.readField(mInstrumentationField, target);
            Instrumentation newInstrumentation = new HostInstrumentation(
                    PluginManager.getInstance(hostContext), baseInstrumentation);
            FieldUtils.writeField(mInstrumentationField, target, newInstrumentation);
            Log.i(TAG, "HostInstrumentation has installed!");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public HostInstrumentation(PluginManager pluginManager, Instrumentation base) {
        this.mPluginManager = pluginManager;
        this.mBase = base;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        Intent pluginIntent = mPluginManager.getPluginActivityIntent(intent);

        if (pluginIntent != null) {
            intent = pluginIntent;
        }

        ActivityResult result = realExecStartActivity(who, contextThread, token, target,
                intent, requestCode, options);

        return result;

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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.d(TAG, "newActivity() className = " + className);
        if (className.startsWith(ActivityStub.class.getName())){
            ActivityInfo activityInfo = intent.getParcelableExtra(PluginManagerNative.EXTRA_INTENT_TARGET_ACTIVITYINFO);
            Log.d(TAG, "newActivity() target activityInfo = " + activityInfo);
            if (activityInfo != null) {
                PluginInfo pluginInfo = PluginManager.getPluginInfo(activityInfo.packageName);
                Activity activity = mBase.newActivity(pluginInfo.classLoader, activityInfo.name, intent);
                activity.setIntent(intent);
                try {
                    FieldUtils.writeField(activity, "mResources", pluginInfo.resources);
                    Log.d(TAG, "newActivity() replace mResources ok! ");
                } catch (Exception ignored) {
                    Log.e(TAG, "newActivity() replace mResources error! ");
                    ignored.printStackTrace();
                }
            }
        }

        return mBase.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        Log.d(TAG, "callActivityOnCreate() activity = " + activity);
        Intent intent = activity.getIntent();
        ActivityInfo activityInfo = intent.getParcelableExtra(PluginManagerNative.EXTRA_INTENT_TARGET_ACTIVITYINFO);
        Log.d(TAG, "callActivityOnCreate() target activityInfo = " + activityInfo);
        if (activityInfo != null) {
            PluginInfo pluginInfo = PluginManager.getPluginInfo(activityInfo.packageName);
            Context pluginContext = mPluginManager.createPluginContext(
                    activityInfo.packageName, activity.getBaseContext());
            try {
                FieldUtils.writeField(activity.getBaseContext(), "mResources", pluginInfo.resources);
                FieldUtils.writeField(activity, "mResources", pluginInfo.resources);
                FieldUtils.writeField(activity, "mTheme", pluginContext.getTheme());
                FieldUtils.writeField(activity, "mApplication", pluginInfo.application);
                FieldUtils.writeField(activity, "mBase", pluginContext);
                Log.d(TAG, "callActivityOnCreate() replace context ok! ");
            } catch (IllegalAccessException e) {
                Log.e(TAG, "callActivityOnCreate() replace context error! ");
                e.printStackTrace();
            }
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
