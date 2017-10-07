package com.android.common;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.IContentProvider;
import android.net.Uri;

import com.reginald.pluginm.utils.Logger;

import java.lang.reflect.Method;

/**
 * Created by lxy on 16-10-26.
 */
public class ContentProviderCompat {
    private static final String TAG = "ContentProviderCompat";

    private static Method sAcquireProviderMethod;
    private static Method sGetIContentProviderMethod;

    static {
        try {
            Class[] arrayOfClass = new Class[]{Uri.class};
            sAcquireProviderMethod = ContentResolver.class.getMethod("acquireProvider",
                    arrayOfClass);
            sGetIContentProviderMethod = ContentProvider.class.getMethod("getIContentProvider");
        } catch (Exception e) {
            Logger.d(TAG, "can not find acquireProvider or getIContentProvider");
            sAcquireProviderMethod = null;
            sGetIContentProviderMethod = null;
        }
    }

    public static boolean hasPorvider(ContentResolver cr, Uri uri) {
        if (sAcquireProviderMethod != null) {
            try {
                ContentProviderClient client = cr.acquireContentProviderClient(uri);
                if (client != null) {
                    client.release();
                    return true;
                }
                return false;
            } catch (Exception localInvocationTargetException) {
                // ignore this, will to the final
            }
        }
        return false;
    }

    public static IContentProvider getIContentProvider(ContentProvider cp) {
        if (sGetIContentProviderMethod != null) {
            try {
                return (IContentProvider) sGetIContentProviderMethod.invoke(cp);
            } catch (Exception e) {
                Logger.e(TAG, "getIContentProvider() error!", e);
            }
        }
        return null;
    }
}
