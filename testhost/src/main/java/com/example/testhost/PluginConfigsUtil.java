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
        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.apkPath = "testplugin-debug.apk";
        pluginConfig.packageName = "com.example.testplugin";
        pluginConfig.desc = "plugin test apk";
        pluginConfigs.add(pluginConfig);

        return pluginConfigs;
    }

}
