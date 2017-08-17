package com.reginald.pluginm.hook;

import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lxy on 17-8-16.
 */

public abstract class ServiceHook implements InvocationHandler {
    private static final String TAG = "ServiceHook";

    protected Object mBase;
    protected Map<String, MethodHandler> mMethodHandlers = new HashMap<String, MethodHandler>(2);

    /**
     * 1. replace the host object(mBase) with the new one(Hook)
     * 2. init MethodHandlers
     * @return
     */
    public abstract boolean install();

    protected void addMethodHandler(MethodHandler methodHandler) {
        Log.d(TAG, "addMethodHandler " + methodHandler);
        mMethodHandlers.put(methodHandler.getName(), methodHandler);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.d(TAG, String.format("invoke %s.%s(%s)",
                method.getDeclaringClass().getName(), method.getName(),
                args != null ? Arrays.asList(args) : "NULL"));
        try {
            if (method != null) {
                MethodHandler methodHandler = mMethodHandlers.get(method.getName());
                if (methodHandler != null) {
                    return methodHandler.invoke(mBase, method, args);
                }
            }
            return method.invoke(mBase, args);
        } catch (InvocationTargetException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }


    public static class MethodHandler {
        public Object invoke(Object receiver, Method method, Object[] args) throws Throwable {
            long startTime = SystemClock.elapsedRealtime();

            try {
                Object invokeResult = null;
                boolean isInvokeBase = onStartInvoke(receiver, method, args);
                if (isInvokeBase) {
                    invokeResult = method.invoke(receiver, args);
                }
                invokeResult = onEndInvoke(receiver, method, args, invokeResult);

                return invokeResult;
            } finally {
                long endTime = SystemClock.elapsedRealtime();
                Log.d(TAG, String.format("MethodHandler.invoke method %s.%s costs %d ms",
                        method.getDeclaringClass().getName(), method.getName(), endTime - startTime));
            }

        }

        public String getName() {
            return getClass().getSimpleName();
        }

        public boolean onStartInvoke(Object receiver, Method method, Object[] args) {
            return true;
        }

        public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
            return invokeResult;
        }
    }

}
