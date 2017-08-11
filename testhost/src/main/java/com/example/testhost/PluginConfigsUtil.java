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


        return pluginConfigs;
    }

}
