package com.example.testplugin;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by lxy on 16-6-28.
 */
public class PluginApplication extends Application {

    private static final String TAG = "PluginApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "attachBaseContext() base = " + base);
        Cursor cursor = getContentResolver().query(PluginBackContentProvider.CONTENT_URI, null, null, null, null);
        Log.d(TAG, "attachBaseContext() plugin cursor = " + cursor);
        if (cursor != null) {
            cursor.moveToFirst();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }
}
