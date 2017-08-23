package com.reginald.pluginm.stub;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.reginald.pluginm.utils.Logger;

import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Created by lxy on 16-7-1.
 */
public class PluginServiceConnection implements ServiceConnection {
    private static final String TAG = "PluginServiceConnection";

    private static WeakHashMap<ServiceConnection, PluginServiceConnection> sConnectionMap = new WeakHashMap<>();
    private HashMap<ComponentName, ConnectionInfo> mBinderMap = new HashMap<>();
    private ServiceConnection mBase;

    public PluginServiceConnection(ServiceConnection serviceConnection) {
        mBase = serviceConnection;
    }

    public static PluginServiceConnection getConnection(ServiceConnection conn) {
        synchronized (sConnectionMap) {
            PluginServiceConnection pluginConn = sConnectionMap.get(conn);
            Logger.d(TAG, String.format("getConnection(%s) return %s", conn, pluginConn));
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
            Logger.d(TAG, String.format("fetchConnection(%s) return %s", conn, pluginConn));
            return pluginConn;
        }
    }

    public void unbind() {
        for (ConnectionInfo connectionInfo : mBinderMap.values()) {
            connectionInfo.binder.unlinkToDeath(connectionInfo.deathMonitor, 0);
        }
        mBinderMap.clear();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Logger.d(TAG, String.format("onServiceConnected(%s, %s)", name, service));
        if (service != null && service.isBinderAlive()) {
            IPluginServiceStubBinder stubBinder = IPluginServiceStubBinder.Stub.asInterface(service);
            try {
                ComponentName componentName = stubBinder.getComponentName();
                IBinder iBinder = stubBinder.getBinder();
                if (mBase != null && componentName != null) {
                    ConnectionInfo oldConnectionInfo = mBinderMap.get(componentName);
                    Logger.d(TAG, String.format("onServiceConnected() oldConnectionInfo = %s", oldConnectionInfo));
                    if (oldConnectionInfo != null && oldConnectionInfo.binder != null && oldConnectionInfo.binder == iBinder) {
                        Logger.w(TAG, String.format("onServiceConnected() componentName = %s, oldBinder = newBinder = %s  same!", componentName, iBinder));
                        return;
                    }

                    ConnectionInfo newConnectionInfo = new ConnectionInfo();
                    newConnectionInfo.binder = iBinder;
                    newConnectionInfo.deathMonitor = new DeathMonitor(componentName, iBinder);
                    mBinderMap.put(componentName, newConnectionInfo);
                    newConnectionInfo.binder.linkToDeath(newConnectionInfo.deathMonitor, 0);


                    if (oldConnectionInfo != null) {
                        death(componentName, oldConnectionInfo.binder, oldConnectionInfo.deathMonitor);
                    }

                    Logger.d(TAG, String.format("call %s onServiceConnected(%s , %s)", mBase, componentName, iBinder));
                    mBase.onServiceConnected(componentName, iBinder);

                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    private void death(ComponentName name, IBinder service, IBinder.DeathRecipient deathRecipient) {
        if (mBase != null && name != null) {
            Logger.d(TAG, String.format("call %s onServiceDisconnected(%s , %s, %s)", mBase, name, service, deathRecipient));
            mBase.onServiceDisconnected(name);
            if (service != null && deathRecipient != null) {
                service.unlinkToDeath(deathRecipient, 0);
            }
        }
    }

    private final class DeathMonitor implements IBinder.DeathRecipient {
        DeathMonitor(ComponentName name, IBinder service) {
            mName = name;
            mService = service;
        }

        public void binderDied() {
            Logger.d(TAG, String.format("binderDied() name = %s, service = %s", mName, mService));
            death(mName, mService, this);
            mBinderMap.remove(mName);
        }

        final ComponentName mName;
        final IBinder mService;
    }

    private static class ConnectionInfo {
        IBinder binder;
        IBinder.DeathRecipient deathMonitor;
    }
}
