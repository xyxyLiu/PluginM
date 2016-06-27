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
        if (intent.getAction() != null && intent.getAction().equals("kill")) {
            android.os.Process.killProcess(Process.myPid());
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
}
