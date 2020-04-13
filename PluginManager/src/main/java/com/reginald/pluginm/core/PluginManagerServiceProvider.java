package com.reginald.pluginm.core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.reginald.pluginm.comm.PluginCommService;
import com.reginald.pluginm.utils.BinderParcelable;
import com.reginald.pluginm.utils.Logger;

/**
 * Created by lxy on 17-8-23.
 */

public class PluginManagerServiceProvider extends ContentProvider {
    public static final String METHOD_GET_CORE_SERVICE = "method.get_core_service";
    public static final String METHOD_GET_COMM_SERVICE = "method.get_comm_service";
    public static final String KEY_SERVICE = "key.service";
    private static final String TAG = "PluginManagerServiceProvider";
    private static final String AUTH_SUFFIX = ".pluginm.provider.core";

    private static volatile Uri sUri;
    private static final Object sUriLock = new Object();

    public static Uri getUri(Context hostContext) {
        if (sUri == null) {
            synchronized (sUriLock) {
                if (sUri == null) {
                    sUri = Uri.parse("content://" + hostContext.getPackageName() + AUTH_SUFFIX);
                }
            }
        }

        return sUri;
    }

    @Override
    public boolean onCreate() {
        Logger.d(TAG, "onCreate()");
        initCoreService();
        return true;
    }

    private void initCoreService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.d(TAG, "initCoreService ...");
                long startTime = SystemClock.elapsedRealtime();
                PluginManagerService.getInstance(getContext());
                Logger.d(TAG, String.format("initCoreService finished! cost %d ms",
                        SystemClock.elapsedRealtime() - startTime));
            }
        }).start();
    }

    public
    @Nullable
    Bundle call(@NonNull String method, @Nullable String arg,
            @Nullable Bundle extras) {
        Logger.d(TAG, String.format("call method = %s, arg = %s, extras = %s", method, arg, extras));

        if (!TextUtils.isEmpty(method)) {
            if (method.equals(METHOD_GET_CORE_SERVICE)) {
                // delayed init core service
                PluginManagerService coreService = PluginManagerService.getInstance(getContext());
                BinderParcelable binderParcelable = new BinderParcelable(coreService);
                Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_SERVICE, binderParcelable);
                return bundle;
            } else if (method.equals(METHOD_GET_COMM_SERVICE)) {
                BinderParcelable binderParcelable = new BinderParcelable(PluginCommService.getInstance(getContext()));
                Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_SERVICE, binderParcelable);
                return bundle;
            } else {
                Logger.w(TAG, "call method " + method + " NOT supported!");
            }
        }
        return null;
    }


    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
