package com.example.testplugin;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
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

        if (intent != null && "killself".equals(intent.getAction())) {
            Log.d(TAG,"onStartCommand() stopself!");
            stopSelf();
        }

        return super.onStartCommand(intent,flags,startId);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind()");
        return new Binder();
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind()");
        return false;
    }

    public void onRebind(Intent intent) {
        Log.d(TAG,"onRebind()");
    }

}
