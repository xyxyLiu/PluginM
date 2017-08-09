package com.reginald.pluginm.stub;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.reginald.pluginm.PluginManagerNative;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by lxy on 16-10-25.
 */
public class PluginContentResolver extends ContentResolver {
    private static final String TAG = "PluginContentResolver";
    private Context mAppContext;
    private ContentResolver mOriginContentResolver;

    public PluginContentResolver(Context context, ContentResolver contentResolver) {
        super(context);
        mAppContext = context.getApplicationContext();
        mOriginContentResolver = contentResolver;
        Log.d(TAG, "mOriginContentResolver = " + Arrays.asList(contentResolver.getClass().getDeclaredMethods()));
    }

    /** @Override **/
    protected IContentProvider acquireProvider(Context context, String auth) {
        Log.d(TAG, "acquireProvider() auth = " + auth);
        try {
            IContentProvider iContentProvider = getTargetProvider(auth);
            if (iContentProvider != null) {
                return iContentProvider;
            }
            Method method = mOriginContentResolver.getClass().getDeclaredMethod("acquireProvider", new Class[]{Context.class, String.class});
            method.setAccessible(true);
            return (IContentProvider) method.invoke(mOriginContentResolver, context, auth);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /** @Override **/
    protected IContentProvider acquireExistingProvider(Context context, String auth) {
        Log.d(TAG, "acquireExistingProvider() auth = " + auth);
        try {
            IContentProvider iContentProvider = getTargetProvider(auth);
            if (iContentProvider != null) {
                return iContentProvider;
            }
            Method method = mOriginContentResolver.getClass().getDeclaredMethod("acquireExistingProvider", new Class[]{Context.class, String.class});
            method.setAccessible(true);
            return (IContentProvider) method.invoke(mOriginContentResolver, context, auth);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /** @Override **/
    public boolean releaseProvider(IContentProvider provider) {
        Log.d(TAG, "releaseProvider() IContentProvider = " + provider);
        try {
            Method method = mOriginContentResolver.getClass().getDeclaredMethod("releaseProvider", new Class[]{IContentProvider.class});
            method.setAccessible(true);
            method.invoke(mOriginContentResolver, provider);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /** @Override **/
    protected IContentProvider acquireUnstableProvider(Context context, String auth) {
        Log.d(TAG, "acquireUnstableProvider() auth = " + auth);
        try {
            IContentProvider iContentProvider = getTargetProvider(auth);
            if (iContentProvider != null) {
                return iContentProvider;
            }

            Method method = mOriginContentResolver.getClass().getDeclaredMethod("acquireUnstableProvider", new Class[]{Context.class, String.class});
            method.setAccessible(true);
            return (IContentProvider) method.invoke(mOriginContentResolver, context, auth);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /** @Override **/
    public boolean releaseUnstableProvider(IContentProvider icp) {
        Log.d(TAG, "releaseUnstableProvider() IContentProvider = " + icp);
        try {
            Method method = mOriginContentResolver.getClass().getDeclaredMethod("releaseUnstableProvider", new Class[]{IContentProvider.class});
            method.setAccessible(true);
            return (Boolean) method.invoke(mOriginContentResolver, icp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /** @Override **/
    public void unstableProviderDied(IContentProvider icp) {
        Log.d(TAG, "acquireProvider() IContentProvider = " + icp);
        try {
            Method method = mOriginContentResolver.getClass().getDeclaredMethod("unstableProviderDied", new Class[]{IContentProvider.class});
            method.setAccessible(true);
            method.invoke(mOriginContentResolver, icp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** @Override **/
    public void appNotRespondingViaProvider(IContentProvider icp) {
        Log.d(TAG, "appNotRespondingViaProvider() IContentProvider = " + icp);
        try {
            Method method = mOriginContentResolver.getClass().getDeclaredMethod("appNotRespondingViaProvider", new Class[]{IContentProvider.class});
            method.setAccessible(true);
            method.invoke(mOriginContentResolver, icp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private IContentProvider getTargetProvider(String auth) {
        ProviderInfo providerInfo = PluginManagerNative.getInstance(mAppContext).resolveProviderInfo(auth);
        if (providerInfo != null) {
            return getIContentProvider(providerInfo);
        }

        return null;
    }

    private IContentProvider getIContentProvider(ProviderInfo providerInfo) {
        Log.d(TAG, "getIContentProvider() providerInfo = " + providerInfo);
        final Uri uri = Uri.parse("content://" + PluginStubMainProvider.AUTH_PREFIX);
        Bundle providerBundle = new Bundle();
        providerBundle.putParcelable(PluginManagerNative.EXTRA_INTENT_TARGET_PROVIDERINFO, providerInfo);
        Bundle bundle = mOriginContentResolver.call(uri, providerInfo.packageName, providerInfo.name, providerBundle);
        Log.d(TAG, "getIContentProvider() providerInfo = " + providerInfo + " , bundle = " + bundle);
        if (bundle != null) {
            return PluginStubMainProvider.parseIContentProvider(bundle);
        }

        return null;
    }
}
