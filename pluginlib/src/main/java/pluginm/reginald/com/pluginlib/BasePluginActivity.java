package pluginm.reginald.com.pluginlib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by lxy on 16-6-6.
 */
public class BasePluginActivity extends Activity{
    private static final String TAG = "BasePluginActivity";

    static {
        Log.d(TAG,"classloader = " + BasePluginActivity.class.getClassLoader());
    }

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

    private void test() {
        Log.d(TAG, "getFilesDir() = " + getFilesDir());
        Log.d(TAG, "getCacheDir() = " + getCacheDir());
        Log.d(TAG, "getApplication() = " + getApplication());
        Log.d(TAG, "getApplicationContext() = " + getApplicationContext());
    }


}
