package com.example.testplugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class PluginService extends Service {
    private static final String TAG = "PluginService";

    public PluginService() {
        Log.d(TAG,"PluginService()");
    }

    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "service created!", Toast.LENGTH_LONG).show();
        Log.d(TAG,"onCreate()");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand()");
        return super.onStartCommand(intent,flags,startId);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind()");
        return null;
    }
}
