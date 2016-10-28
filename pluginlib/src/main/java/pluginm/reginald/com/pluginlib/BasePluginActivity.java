package pluginm.reginald.com.pluginlib;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by lxy on 16-6-6.
 */
public class BasePluginActivity extends Activity{
    private static final String TAG = "BasePluginActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        Context pluginContext = PluginHelper.pluginManager.createPluginContext(PluginHelper.getPackageName(), newBase);
        Log.d(TAG, "attachBaseContext() pluginContext = " + pluginContext);
        super.attachBaseContext(pluginContext == null ? newBase : pluginContext);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() " + this);
        test();
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() " + this);
        scheduleFinalCleanup();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle bundle) {
        Intent pluginIntent = PluginHelper.pluginManager.getPluginActivityIntent(intent);
        if(pluginIntent != null) {
            super.startActivityForResult(pluginIntent, requestCode, bundle);
        } else {
            super.startActivityForResult(intent, requestCode, bundle);
        }
    }

    private void scheduleFinalCleanup() {
        try {
            Object contextImpl = ((ContextWrapper) super.getBaseContext()).getBaseContext();
            Class<?> contextImplClazz = Class.forName("android.app.ContextImpl");
            if (contextImplClazz != null) {
                Method cleanUpMethod = contextImplClazz.getDeclaredMethod("scheduleFinalCleanup", new Class[]{String.class, String.class});
                cleanUpMethod.setAccessible(true);
                if (cleanUpMethod != null) {
                    cleanUpMethod.invoke(contextImpl, getClass().getName(), "Activity");
                    Log.d(TAG, "doFinalCleanUp() for " + getClass().getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test() {
        Log.d(TAG, "getFilesDir() = " + getFilesDir());
        Log.d(TAG, "getCacheDir() = " + getCacheDir());
        Log.d(TAG, "getApplication() = " + getApplication());
        Log.d(TAG, "getApplicationContext() = " + getApplicationContext());
    }


}
