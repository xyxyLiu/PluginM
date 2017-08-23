package com.example.testhost;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lxy on 16-6-28.
 */
public class PluginConfigsUtil {


    public static Map<String, PluginConfig> getPluginConfigs() {
        Map<String, PluginConfig> pluginConfigsMap = new HashMap<>();


        PluginConfig pluginConfig1 = new PluginConfig();
        pluginConfig1.apkPath = "testplugin-debug.apk";
        pluginConfig1.packageName = "com.example.testplugin";
        pluginConfig1.desc = "plugin test apk";
        pluginConfigsMap.put(pluginConfig1.packageName, pluginConfig1);

        PluginConfig pluginConfig2 = new PluginConfig();
        pluginConfig2.apkPath = "wifimgr-plugin-test.apk";
        pluginConfig2.packageName = "com.dianxinos.optimizer.plugin.wifimgr";
        pluginConfig2.desc = "plugin wifimgr apk";
        pluginConfigsMap.put(pluginConfig2.packageName, pluginConfig2);

        PluginConfig pluginConfig3 = new PluginConfig();
        pluginConfig3.apkPath = "153e48737f5602ae4d15cbcb2fa46b78.apk";
        pluginConfig3.packageName = "com.youba.WeatherForecast";
        pluginConfig3.desc = "com.youba.WeatherForecast";
        pluginConfigsMap.put(pluginConfig3.packageName, pluginConfig3);

        PluginConfig pluginConfig4 = new PluginConfig();
        pluginConfig4.apkPath = "8d12e7277f64db33a6649883e02d04dc.apk";
        pluginConfig4.packageName = "com.wole56.ishow";
        pluginConfig4.desc = "com.wole56.ishow";
        pluginConfigsMap.put(pluginConfig4.packageName, pluginConfig4);

        return pluginConfigsMap;
    }

}
