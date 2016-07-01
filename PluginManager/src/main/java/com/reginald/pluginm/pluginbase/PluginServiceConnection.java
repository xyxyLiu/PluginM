package com.reginald.pluginm.pluginbase;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.example.multidexmodeplugin.IPluginServiceStubBinder;

import java.util.WeakHashMap;

/**
 * Created by lxy on 16-7-1.
 */
public class PluginServiceConnection implements ServiceConnection {
    private static final String TAG = "PluginServiceConnection";

    private static WeakHashMap<ServiceConnection, PluginServiceConnection> sConnectionMap = new WeakHashMap<>();
    private ServiceConnection mBase;

    public PluginServiceConnection(ServiceConnection serviceConnection) {
        mBase = serviceConnection;
    }

    public static PluginServiceConnection getConnection(ServiceConnection conn) {
        synchronized (sConnectionMap) {
            PluginServiceConnection pluginConn = sConnectionMap.get(conn);
            Log.d(TAG, String.format("getConnection(%s) return %s", conn, pluginConn));
            return pluginConn;
        }
    }

    public static PluginServiceConnection fetchConnection(ServiceConnection conn) {
        synchronized (sConnectionMap) {
            PluginServiceConnection pluginConn = sConnectionMap.get(conn);
            if (pluginConn == null) {
                pluginConn = new PluginServiceConnection(conn);
                sConnectionMap.put(conn, pluginConn);
            }
            Log.d(TAG, String.format("fetchConnection(%s) return %s", conn, pluginConn));
            return pluginConn;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, String.format("onServiceConnected(%s, %s)", name, service));
        if (service != null && service.isBinderAlive()) {
            IPluginServiceStubBinder stubBinder = IPluginServiceStubBinder.Stub.asInterface(service);
            try {
                ComponentName componentName = stubBinder.getComponentName();
                IBinder iBinder = stubBinder.getBinder();
                if (mBase != null && componentName != null) {
                    Log.d(TAG, String.format("call %s onServiceConnected(%s , %s)", mBase, componentName, iBinder));
                    mBase.onServiceConnected(componentName, iBinder);
                    iBinder.linkToDeath(new DeathMonitor(componentName, iBinder), 0);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    private void death(ComponentName name, IBinder service) {
        if (mBase != null && name != null) {
            Log.d(TAG, String.format("call %s onServiceDisconnected(%s , %s)", mBase, name));
            mBase.onServiceDisconnected(name);
        }
    }

    private final class DeathMonitor implements IBinder.DeathRecipient
    {
        DeathMonitor(ComponentName name, IBinder service) {
            mName = name;
            mService = service;
        }

        public void binderDied() {
            Log.d(TAG, String.format("binderDied() name = %s, service = %s", mName, mService));
            death(mName, mService);
            mService.unlinkToDeath(this, 0);
        }

        final ComponentName mName;
        final IBinder mService;
    }
}
