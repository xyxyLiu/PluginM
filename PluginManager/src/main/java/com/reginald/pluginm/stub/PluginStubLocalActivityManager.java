package com.reginald.pluginm.stub;

import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.pluginapi.PluginHelper;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.utils.Logger;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.view.Window;

/**
 * Created by lxy on 18-6-20.
 */

public class PluginStubLocalActivityManager extends LocalActivityManager {
    private static final String TAG = "PluginStubLocalActivityManager";
    private final PluginManager mPluginManager;

    /**
     * Create a new LocalActivityManager for holding activities running within
     * the given <var>parent</var>.
     *
     * @param parent     the host of the embedded activities
     * @param singleMode True if the LocalActivityManger should keep a maximum
     */
    public PluginStubLocalActivityManager(Activity parent, boolean singleMode) {
        super(parent, singleMode);
        mPluginManager = PluginManager.getInstance(PluginHelper.getHostContext(parent));
    }

    public PluginStubLocalActivityManager(LocalActivityManager base) throws IllegalAccessException {
        this((Activity)FieldUtils.readField(base, "mParent"), (Boolean) FieldUtils.readField(base, "mSingleMode"));
    }

    @Override
    public Window startActivity(String id, Intent intent) {
        Logger.d(TAG, "startActivity " + intent);
        Intent resolvedIntent = mPluginManager.getPluginActivityIntent(intent);

        if (resolvedIntent == null) {
            resolvedIntent = intent;
        }

        return super.startActivity(id, resolvedIntent);
    }
}
