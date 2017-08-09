package com.reginald.pluginm.stub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PluginStubBroadcast extends BroadcastReceiver {

    private static final String TAG = "PluginStubBroadcast";

    public PluginStubBroadcast() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Plugin static BroadcastReceiver.onReceive() context = " + context + " ,intent = " + intent);
    }
}
