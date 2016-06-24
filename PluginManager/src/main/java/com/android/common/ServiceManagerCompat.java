package com.android.common;

import android.os.IBinder;
import android.util.Log;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ServiceManagerCompat {
    private static final String TAG = "ServiceManagerCompat";

    private static final String CLASSNAME_SERVICE_MANAGER = "android.os.ServiceManager";

    private static Class<?> sServiceManagerClass;
    private static Method sGetServiceMethod;
    private static Method sCheckServiceMethod;
    private static Method sAddServiceMethod;
    private static Method sListServicesMethod;

    static {
        try {
            sServiceManagerClass = Class.forName(CLASSNAME_SERVICE_MANAGER, false, Thread.currentThread().getContextClassLoader());
            sGetServiceMethod = sServiceManagerClass.getMethod("getService", String.class);
            sCheckServiceMethod = sServiceManagerClass.getMethod("checkService", String.class);
            sAddServiceMethod = sServiceManagerClass.getMethod("addService", String.class, IBinder.class);
            sListServicesMethod = sServiceManagerClass.getMethod("listServices");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "unexpected", e);
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "unexpected", e);
        }
    }

    public static IBinder getService(Object name) {
        if (sGetServiceMethod != null) {
            try {
                Method localMethod = sGetServiceMethod;
                Object[] arrayOfObject = new Object[]{name};
                Object ret = localMethod.invoke(null, arrayOfObject);
                return (IBinder) ret;
            } catch (IllegalAccessException e) {
                Log.w(TAG, "unexpected", e);
            } catch (InvocationTargetException e) {
                Log.w(TAG, "unexpected", e);
            } catch (Exception e) {
                Log.w(TAG, "unexpected", e);
            }
        }
        return null;
    }

    public static IBinder checkService(Object name) {
        if (sCheckServiceMethod != null) {
            try {
                Method localMethod = sCheckServiceMethod;
                Object[] arrayOfObject = new Object[]{name};
                Object ret = localMethod.invoke(null, arrayOfObject);
                return (IBinder) ret;
            } catch (IllegalAccessException e) {
                Log.w(TAG, "unexpected", e);
            } catch (InvocationTargetException e) {
                Log.w(TAG, "unexpected", e);
            } catch (Exception e) {
                Log.w(TAG, "unexpected", e);
            }
        }
        return null;
    }

    public static void addService(String name, IBinder service) {
        if (sAddServiceMethod != null) {
            try {
                Method localMethod = sAddServiceMethod;
                Object[] arrayOfObject = new Object[]{name, service};
                localMethod.invoke(null, arrayOfObject);
            } catch (IllegalAccessException e) {
                Log.w(TAG, "unexpected", e);
            } catch (InvocationTargetException e) {
                Log.w(TAG, "unexpected", e);
            } catch (Exception e) {
                Log.w(TAG, "unexpected", e);
            }
        }
    }

    public static String[] listServices() {
        if (sListServicesMethod != null) {
            try {
                Method localMethod = sListServicesMethod;
                return (String[]) localMethod.invoke(null);
            } catch (IllegalAccessException e) {
                Log.w(TAG, "unexpected", e);
            } catch (InvocationTargetException e) {
                Log.w(TAG, "unexpected", e);
            } catch (Exception e) {
                Log.w(TAG, "unexpected", e);
            }
        }
        return null;
    }

}
