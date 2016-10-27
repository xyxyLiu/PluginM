package pluginm.reginald.com.pluginlib;

import pluginm.reginald.com.pluginlib.proxy.ProxyManager;

/**
 * Created by lxy on 16-10-27.
 */
public class PluginHelper {

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
    }

    public static String getPackageName() {
        return packageName;
    }
}
