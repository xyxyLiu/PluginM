package com.reginald.pluginm.hook;

import android.os.SystemClock;

import com.reginald.pluginm.utils.Logger;

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
    protected final Map<String, MethodHandler> mMethodHandlers = new HashMap<String, MethodHandler>(2);
    protected MethodHandler mAllMethodHandler;

    /**
     * 1. replace the host object(mBase) with the new one(Hook)
     * 2. init MethodHandlers
     * @return
     */
    public abstract boolean install();

    protected void addMethodHandler(MethodHandler methodHandler) {
        Logger.d(TAG, "addMethodHandler " + methodHandler);
        mMethodHandlers.put(methodHandler.getName(), methodHandler);
    }

    protected void setAllMethodHandler(MethodHandler methodHandler) {
        Logger.d(TAG, "setAllMethodHandler " + methodHandler);
        mAllMethodHandler = methodHandler;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Logger.d(TAG, String.format("invoke %s.%s(%s)",
                method.getDeclaringClass().getName(), method.getName(),
                args != null ? Arrays.asList(args) : "NULL"));
        try {
            if (method != null) {
                MethodHandler methodHandler = mMethodHandlers.get(method.getName());
                if (methodHandler != null) {
                    return methodHandler.invoke(mBase, method, args);
                }

                if (mAllMethodHandler != null) {
                    return mAllMethodHandler.invoke(mBase, method, args);
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
        public static final Object NONE_RETURN = new Object();
        public Object invoke(Object receiver, Method method, Object[] args) throws Throwable {
            long startTime = SystemClock.elapsedRealtime();

            try {
                Object invokeResult = onStartInvoke(receiver, method, args);
                if (invokeResult == NONE_RETURN) {
                    invokeResult = method.invoke(receiver, args);
                }
                invokeResult = onEndInvoke(receiver, method, args, invokeResult);

                return invokeResult;
            } finally {
                long endTime = SystemClock.elapsedRealtime();
                Logger.d(TAG, String.format("MethodHandler.invoke method %s.%s costs %d ms",
                        method.getDeclaringClass().getName(), method.getName(), endTime - startTime));
            }

        }

        /**
         * 子类需要覆盖此方法提供hook的方法名
         * @return
         */
        public String getName() {
            return getClass().getSimpleName();
        }

        /**
         * 调用IActivityManager接口之前的操作
         * @param receiver
         * @param method
         * @param args
         * @return 是否需要实际调用IActivityManager的接口
         */
        public Object onStartInvoke(Object receiver, Method method, Object[] args) {
            return NONE_RETURN;
        }

        /**
         * 调用IActivityManager接口之后的操作
         * @param receiver
         * @param method
         * @param args
         * @param invokeResult 原始返回的结果
         * @return 实际要处理返回的结果，默认不做任何处理，返回 invokeResult
         */
        public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
            return invokeResult;
        }
    }

}
