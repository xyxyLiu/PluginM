package com.android.common;

import android.content.Context;
import android.content.ContextWrapper;

import com.reginald.pluginm.utils.Logger;

import java.lang.reflect.Method;

@SuppressWarnings("NewApi")
public class ContextCompat {
    private final static String TAG = "ContextCompat";
    private final static boolean DEBUG = true;

    private static Class<?> sClassContextImpl;
    private static Method sMethodSetOuterContext;

    static {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try {
            sClassContextImpl = classLoader.loadClass("android.app.ContextImpl");
            sMethodSetOuterContext = sClassContextImpl.getDeclaredMethod("setOuterContext", Context.class);
            sMethodSetOuterContext.setAccessible(true);
        } catch (Exception e) {
            sClassContextImpl = null;
            sMethodSetOuterContext = null;
        }
    }

    public static void setOuterContext(Context context, Context outerContext) {
        if (sMethodSetOuterContext != null) {
            while (!sClassContextImpl.isInstance(context)) {
                if (context instanceof ContextWrapper) {
                    context = ((ContextWrapper) context).getBaseContext();
                } else {
                    Logger.e(TAG, "setOuterContext error context=" + context);
                    return;
                }
            }

            try {
                sMethodSetOuterContext.invoke(context, outerContext);
            } catch (Exception e) {
                Logger.e(TAG, "setOuterContext fail context=" + context, e);
            }
        }
    }
}
