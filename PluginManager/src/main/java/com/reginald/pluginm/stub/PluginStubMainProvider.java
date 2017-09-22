package com.reginald.pluginm.stub;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.IContentProvider;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.common.ContentProviderCompat;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.comm.PluginCommService;
import com.reginald.pluginm.core.PluginClient;
import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.utils.BinderParcelable;
import com.reginald.pluginm.utils.Logger;
import com.reginald.pluginm.utils.ThreadUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lxy on 16-10-24.
 */
public class PluginStubMainProvider extends ContentProvider {
    private static final String TAG = "PluginStubMainProvider";
    private static final String EXTRA_BINDER = "extra.binder";
    public static final String METHOD_GET_PROVIDER = "method.get_provider";
    public static final String METHOD_GET_CLIENT = "method.get_client";

    private static PluginStubMainProvider sInstance;
    private final Map<String, ContentProvider> mContentProviderMap = new HashMap<>();

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
        Logger.d(TAG, "onCreate() ");
        sInstance = this;
        return true;
    }

    public static void loadProviders(PluginInfo pluginInfo, List<ProviderInfo> targetProviderInfos) {
        Logger.d(TAG, "installProviders() sInstance = " + sInstance);
        if (sInstance == null) {
            throw new RuntimeException("CAN NOT found running stub provider!");
        }
        if (targetProviderInfos != null) {
            sInstance.installProviders(pluginInfo, targetProviderInfos);
        }
    }


    public
    @Nullable
    Bundle call(@NonNull String method, @Nullable String arg,
            @Nullable Bundle extras) {
        Logger.d(TAG, "call() method =" + method + ", arg=" + arg);
        if (METHOD_GET_PROVIDER.equals(method)) {
            final ProviderInfo providerInfo = extras.getParcelable(PluginManager.EXTRA_INTENT_TARGET_PROVIDERINFO);
            final Bundle resultBundle = new Bundle();

            ThreadUtils.ensureRunOnMainThread(new Runnable() {
                @Override
                public void run() {
                    PluginInfo loadedPluginInfo = PluginManager.getInstance(getContext()).loadPlugin(providerInfo.packageName);

                    if (loadedPluginInfo == null) {
                        return;
                    }

                    try {
                        IContentProvider iContentProvider = getIContentProvider(loadedPluginInfo, providerInfo);
                        Logger.d(TAG, "call() iContentProvider = " + iContentProvider);
                        if (iContentProvider != null) {
                            BinderParcelable binderParcelable = new BinderParcelable(iContentProvider.asBinder());
                            resultBundle.putParcelable(EXTRA_BINDER, binderParcelable);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            return resultBundle.isEmpty() ? null : resultBundle;
        } else if (METHOD_GET_CLIENT.equals(method)) {
            final Bundle resultBundle = new Bundle();
            BinderParcelable binderParcelable = new BinderParcelable(PluginClient.getInstance(getContext()));
            resultBundle.putParcelable(EXTRA_BINDER, binderParcelable);
            return resultBundle;
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
            ContentProvider contentProvider;
            synchronized (mContentProviderMap) {
                contentProvider = mContentProviderMap.get(providerInfo.name);
            }
            if (contentProvider != null) {
                return ContentProviderCompat.getIContentProvider(contentProvider);
            } else {
                throw new RuntimeException("CAN NOT get IContentProvider for " + providerInfo);
            }
        }
        return null;
    }

    private void installProviders(PluginInfo pluginInfo, List<ProviderInfo> providerInfos) {
        if (providerInfos != null) {
            for (ProviderInfo providerInfo : providerInfos) {
                try {
                    Logger.d(TAG, "installProviders() providerInfo = " + providerInfo);
                    Class clazz = pluginInfo.classLoader.loadClass(providerInfo.name);
                    ContentProvider contentProvider = (ContentProvider) clazz.newInstance();
                    contentProvider.attachInfo(pluginInfo.baseContext, providerInfo);
                    synchronized (mContentProviderMap) {
                        mContentProviderMap.put(providerInfo.name, contentProvider);
                    }
                    Logger.d(TAG, "installProviders() providerInfo ok!");
                } catch (Exception e) {
                    Logger.e(TAG, "installProviders() error! providerInfo = " + providerInfo, e);
                }
            }
        }
    }

    public static IContentProvider parseIContentProvider(Bundle bundle) {
        IBinder iBinder = parseBinderParacelable(bundle);

        if (iBinder != null) {
            try {
                return (IContentProvider) MethodUtils.invokeStaticMethod(sContentProviderNativeClass, "asInterface", iBinder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static IBinder parseBinderParacelable(Bundle bundle) {
        if (bundle != null) {
            bundle.setClassLoader(BinderParcelable.class.getClassLoader());
            BinderParcelable binderParcelable = bundle.getParcelable(EXTRA_BINDER);
            try {
                return binderParcelable.iBinder;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
