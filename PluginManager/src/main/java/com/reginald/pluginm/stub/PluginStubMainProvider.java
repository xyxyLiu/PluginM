package com.reginald.pluginm.stub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.common.ContentProviderCompat;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.utils.BinderParcelable;
import com.reginald.pluginm.utils.CommonUtils;
import com.reginald.pluginm.utils.Logger;

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

/**
 * Created by lxy on 16-10-24.
 */
public class PluginStubMainProvider extends ContentProvider {
    private static final String TAG = "PluginStubMainProvider";
    private static final String EXTRA_BINDER = "extra.binder";
    public static final String METHOD_GET_PROVIDER = "method.get_provider";
    public static final String METHOD_START_PROCESS = "method.start_process";

    private static PluginStubMainProvider sInstance;
    private final Map<String, ContentProvider> mContentProviderMap = new HashMap<>();
    private ProviderInfo mStubInfo;

    private static Class sContentProviderNativeClass;

    static {
        try {
            sContentProviderNativeClass = Class.forName("android.content.ContentProviderNative");
        } catch (Exception e) {
            Logger.e(TAG, "find android.content.ContentProviderNative error!", e);
        }
    }

    @Override
    public boolean onCreate() {
        Logger.d(TAG, "onCreate() ");
        sInstance = this;
        mStubInfo = CommonUtils.getProviderInfo(this);
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

            PluginInfo loadedPluginInfo = PluginManager.getInstance().loadPlugin(providerInfo);

            if (loadedPluginInfo == null) {
                return null;
            }

            try {
                IContentProvider iContentProvider = getIContentProvider(loadedPluginInfo, providerInfo);
                Logger.d(TAG, "call() iContentProvider = " + iContentProvider);
                if (iContentProvider != null) {
                    BinderParcelable binderParcelable = new BinderParcelable(iContentProvider.asBinder());
                    resultBundle.putParcelable(EXTRA_BINDER, binderParcelable);
                    return resultBundle;
                }
            } catch (Exception e) {
                Logger.e(TAG, "found provider error!", e);
            }

            return null;
        } else if (METHOD_START_PROCESS.equals(method)) {
            // return an empty bundle
            return new Bundle();
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

            if (contentProvider == null) {
                contentProvider = installProvider(pluginInfo, providerInfo);
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
                    installProvider(pluginInfo, providerInfo);
                    Logger.d(TAG, "installProviders() providerInfo ok!");
                } catch (Exception e) {
                    Logger.e(TAG, "installProviders() error! providerInfo = " + providerInfo, e);
                }
            }
        }
    }

    private ContentProvider installProvider(PluginInfo pluginInfo, ProviderInfo providerInfo) {
        try {
            Logger.d(TAG, "installProvider() providerInfo = " + providerInfo);
            Class clazz = pluginInfo.classLoader.loadClass(providerInfo.name);
            ContentProvider contentProvider = (ContentProvider) clazz.newInstance();
            contentProvider.attachInfo(pluginInfo.baseContext, providerInfo);

            synchronized (mContentProviderMap) {
                ContentProvider cp = mContentProviderMap.get(providerInfo.name);
                if (cp != null) {
                    Logger.d(TAG, "installProvider() lose race!");
                    contentProvider = cp;
                } else {
                    mContentProviderMap.put(providerInfo.name, contentProvider);
                    PluginManager.getInstance().callProviderOnCreate(contentProvider, mStubInfo, providerInfo);
                }
            }

            Logger.d(TAG, "installProvider() providerInfo -> " + contentProvider);
            return contentProvider;
        } catch (Exception e) {
            Logger.e(TAG, "installProvider() error! providerInfo = " + providerInfo, e);
        }

        return null;
    }

    public static IContentProvider parseIContentProvider(Bundle bundle) {
        IBinder iBinder = parseBinderParacelable(bundle);

        if (iBinder != null) {
            try {
                return (IContentProvider) MethodUtils.invokeStaticMethod(sContentProviderNativeClass, "asInterface", iBinder);
            } catch (Exception e) {
                Logger.e(TAG, "parseIContentProvider() error!", e);
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
                Logger.e(TAG, "parseBinderParacelable() error!", e);
            }
        }

        return null;
    }
}
