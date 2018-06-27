package com.example.testplugin;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.InputFilter;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by lxy on 16-6-30.
 */
public class PluginServiceA extends Service {

    private static final String TAG = "PluginServiceA";

    private IBinder myBinder = new ITestBinder.Stub() {

        @Override
        public String getTestString() throws RemoteException {
            return "Hi, I'm ITestBinder, from process " + android.os.Process.myPid();
        }
    };

    private BroadcastReceiver mReceiver;

    public PluginServiceA() {
        Log.d(TAG,"PluginServiceA()");
    }

    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "service created!", Toast.LENGTH_LONG).show();
        Log.d(TAG,"onCreate()");
        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG,"PluginServiceA.BroadcastReceiver onReceive");
                }
            };
            IntentFilter localIntentFilter = new IntentFilter();
            localIntentFilter.addAction("android.intent.action.MEDIA_EJECT");
            localIntentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
            localIntentFilter.addDataScheme("file");
            registerReceiver(mReceiver, localIntentFilter);
        }

    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() intent = " + intent + " , startId = " + startId);

        if (intent != null && "killself".equals(intent.getAction())) {
            int stopId = 3;
            Log.d(TAG,"onStartCommand() stopself(" + stopId + ")!");
            stopSelf(stopId);
        } else if (intent != null && "startactivity".equals(intent.getAction())) {
            Intent actIntent = new Intent(this, PluginMainActivity.class);

//            try {
//                Log.d(TAG, "## mBase of service is " + FieldUtils.readField(this, "mBase"));
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
            actIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(actIntent);
        }

        return super.onStartCommand(intent,flags,startId);
    }

    public void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
        Log.d(TAG,"onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind()");
        return myBinder;//new Binder();
    }

    public boolean onUnbind(Intent intent) {
        boolean res = true;
        Log.d(TAG,"onUnbind() return " + res);
        return res;
    }

    public void onRebind(Intent intent) {
        Log.d(TAG,"onRebind()");
    }
}
