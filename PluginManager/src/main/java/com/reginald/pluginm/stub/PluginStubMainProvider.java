package com.reginald.pluginm.stub;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.IContentProvider;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.common.ContentProviderCompat;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.PluginManager;
import com.reginald.pluginm.PluginManager;
import com.reginald.pluginm.reflect.MethodUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lxy on 16-10-24.
 */
public class PluginStubMainProvider extends ContentProvider {
    public static final String AUTH_PREFIX = "com.reginald.pluginm.stub.provider";
    private static final String TAG = "PluginStubMainProvider";
    private static final String EXTRA_BINDER = "extra.binder";

    private final Map<String, IContentProvider> mIContentProviderMap = new HashMap<>();

    private static Class sContentProviderNativeClass;

    static {
        try {
            sContentProviderNativeClass = Class.forName("android.content.ContentProviderNative");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate() ");
        return true;
    }


    public
    @Nullable
    Bundle call(@NonNull String method, @Nullable String arg,
            @Nullable Bundle extras) {
        Log.d(TAG, "call() method =" + method + ", arg=" + arg);
        if (!TextUtils.isEmpty(method) && !TextUtils.isEmpty(arg)) {
            String packageName = method;
            String providerName = arg;
            ProviderInfo providerInfo = extras.getParcelable(PluginManager.EXTRA_INTENT_TARGET_PROVIDERINFO);

            PluginInfo loadedPluginInfo = PluginManager.getInstance(getContext()).loadPlugin(providerInfo.applicationInfo);

            if (loadedPluginInfo == null) {
                return null;
            }

            try {
                IContentProvider iContentProvider = getIContentProvider(loadedPluginInfo, providerInfo);
                Log.d(TAG, "call() iContentProvider = " + iContentProvider);
                if (iContentProvider != null) {
                    Bundle bundle = new Bundle();
                    BinderParcelable binderParcelable = new BinderParcelable(iContentProvider.asBinder());
                    bundle.putParcelable(EXTRA_BINDER, binderParcelable);
                    return bundle;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return null;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private IContentProvider getIContentProvider(@NonNull PluginInfo pluginInfo, @NonNull ProviderInfo providerInfo) {
        if (pluginInfo != null && providerInfo != null) {
            try {
                IContentProvider iContentProvider;
                synchronized (mIContentProviderMap) {
                    iContentProvider = mIContentProviderMap.get(providerInfo.name);
                }
                if (iContentProvider != null) {
                    return iContentProvider;
                }

                Log.d(TAG, "getIContentProvider() loadPlugin provider " + providerInfo.name);
                Class clazz = pluginInfo.classLoader.loadClass(providerInfo.name);
                ContentProvider contentProvider = (ContentProvider) clazz.newInstance();
                contentProvider.attachInfo(pluginInfo.baseContext, providerInfo);
                iContentProvider = ContentProviderCompat.getIContentProvider(contentProvider);
                synchronized (mIContentProviderMap) {
                    mIContentProviderMap.put(providerInfo.name, iContentProvider);
                }
                return iContentProvider;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static IContentProvider parseIContentProvider(Bundle bundle) {
        if (bundle != null) {
            bundle.setClassLoader(BinderParcelable.class.getClassLoader());
            BinderParcelable binderParcelable = bundle.getParcelable(EXTRA_BINDER);
            try {
                return (IContentProvider) MethodUtils.invokeStaticMethod(sContentProviderNativeClass, "asInterface", binderParcelable.mIBinder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
