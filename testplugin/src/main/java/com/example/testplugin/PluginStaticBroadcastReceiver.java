package com.example.testplugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PluginStaticBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "StaticBroadcastReceiver";

    public PluginStaticBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Plugin static BroadcastReceiver.onReceive() context = " + context + " ,intent = " + intent);
    }
}
