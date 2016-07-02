package com.example.testhost;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

public class HostService extends Service {

    private static final String TAG = "HostService";

    private Binder myBinder = new Binder();


    public HostService() {
        Log.d(TAG,"HostService()");
    }

    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "service created!", Toast.LENGTH_LONG).show();
        Log.d(TAG,"onCreate()");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand()");
        if (intent != null && "stopself".equals(intent.getAction())) {
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
        showAction(intent);
        return new Binder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean res = false;
        Log.d(TAG,"onUnbind() return " + res);
        showAction(intent);
        return res;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG,"onRebind()");
        showAction(intent);
    }

    public void showAction(Intent intent) {
        Log.d(TAG, "ACTION = " + intent.getAction());
    }
}
