package pluginm.reginald.com.pluginlib;

import android.text.TextUtils;
import android.util.Log;

import pluginm.reginald.com.pluginlib.proxy.ProxyManager;

/**
 * Created by lxy on 16-10-27.
 */
public class PluginHelper {
    private static final String TAG = "PluginHelper";
    public static IPluginManager pluginManager;
    public static String packageName;

    /**
     * called by host
     * @param iPluginManager
     * @param pkgName
     */
    public static void onInit(Object iPluginManager, String pkgName) {
        packageName = pkgName;
        pluginManager = ProxyManager.getProxy(iPluginManager, IPluginManager.class);

        Log.d(TAG, "onInit() packageName = " + packageName + " , pluginManager = " +
                (pluginManager == null ? "null" : "IPluginManager" + pluginManager.getClass().getName()));
        if (TextUtils.isEmpty(packageName) || pluginManager == null) {
            throw new RuntimeException("PluginHelper init error!");
        }
    }

    public static String getPackageName() {
        return packageName;
    }
}
