package com.reginald.pluginm.stub;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.utils.Logger;

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
        Logger.d(TAG, "mOriginContentResolver = " + Arrays.asList(contentResolver.getClass().getDeclaredMethods()));
    }

    /** @Override **/
    protected IContentProvider acquireProvider(Context context, String auth) {
        Logger.d(TAG, "acquireProvider() auth = " + auth);
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
        Logger.d(TAG, "acquireExistingProvider() auth = " + auth);
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
        Logger.d(TAG, "releaseProvider() IContentProvider = " + provider);
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
        Logger.d(TAG, "acquireUnstableProvider() auth = " + auth);
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
        Logger.d(TAG, "releaseUnstableProvider() IContentProvider = " + icp);
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
        Logger.d(TAG, "acquireProvider() IContentProvider = " + icp);
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
        Logger.d(TAG, "appNotRespondingViaProvider() IContentProvider = " + icp);
        try {
            Method method = mOriginContentResolver.getClass().getDeclaredMethod("appNotRespondingViaProvider", new Class[]{IContentProvider.class});
            method.setAccessible(true);
            method.invoke(mOriginContentResolver, icp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private IContentProvider getTargetProvider(String auth) {
        Pair<Uri, Bundle> uriAndBundle = PluginManager.getInstance(mAppContext).getPluginProviderUri(auth);
        Logger.d(TAG, "getTargetProvider() auth = " + auth + "  ->  uriAndBundle = " + uriAndBundle);
        if (uriAndBundle != null) {
            return getIContentProvider(uriAndBundle);
        }

        return null;
    }

    private IContentProvider getIContentProvider(Pair<Uri, Bundle> uriAndBundle) {
        Logger.d(TAG, "getIContentProvider() uriAndBundle = " + uriAndBundle);
        final Uri uri = uriAndBundle.first;
        if (uri != null) {
            Bundle providerBundle = uriAndBundle.second;
            Bundle bundle = mOriginContentResolver.call(uri, PluginStubMainProvider.METHOD_GET_PROVIDER, null, providerBundle);
            Logger.d(TAG, "getIContentProvider() return bundle = " + bundle);
            if (bundle != null) {
                return PluginStubMainProvider.parseIContentProvider(bundle);
            }
        }

        return null;
    }
}
