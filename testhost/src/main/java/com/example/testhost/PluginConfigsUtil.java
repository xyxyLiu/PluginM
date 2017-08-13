package com.example.testhost;

import com.reginald.pluginm.PluginConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lxy on 16-6-28.
 */
public class PluginConfigsUtil {


    public static List<PluginConfig> getPluginConfigs() {
        List<PluginConfig> pluginConfigs = new ArrayList<>();


        PluginConfig pluginConfig1 = new PluginConfig();
        pluginConfig1.apkPath = "testplugin-debug.apk";
        pluginConfig1.packageName = "com.example.testplugin";
        pluginConfig1.desc = "plugin test apk";
        pluginConfigs.add(pluginConfig1);

        PluginConfig pluginConfig2 = new PluginConfig();
        pluginConfig2.apkPath = "wifimgr-plugin-test.apk";
        pluginConfig2.packageName = "com.dianxinos.optimizer.plugin.wifimgr";
        pluginConfig2.desc = "plugin wifimgr apk";
        pluginConfigs.add(pluginConfig2);

        PluginConfig pluginConfig3 = new PluginConfig();
        pluginConfig3.apkPath = "153e48737f5602ae4d15cbcb2fa46b78.apk";
        pluginConfig3.packageName = "com.youba.WeatherForecast";
        pluginConfig3.desc = "com.youba.WeatherForecast";
        pluginConfigs.add(pluginConfig3);


        return pluginConfigs;
    }

}
