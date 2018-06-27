package com.example.testhost;

import com.reginald.pluginm.PluginConfigs;
import com.reginald.pluginm.PluginM;

import android.app.Application;
import android.content.Context;

/**
 * Created by lxy on 16-6-22.
 */
public class HostApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        PluginM.onAttachBaseContext(this, new PluginConfigs()
                // 插件进程模式, 默认为 PluginConfigs.PROCESS_TYPE_INDEPENDENT
                .setProcessType(PluginConfigs.PROCESS_TYPE_COMPLETE)
                // 如果插件中使用插件ClassLoader加载未成功时，是否允许宿主尝试加载。默认为true
                .setUseHostLoader(true)
                // 是否开启签名检测，默认为false
                //.setSignatureCheckEnabled(true)
                // 如果开启签名检测，请填写合法的签名信息. 默认为空
                //.addSignatures(MaybeYourHostSignatures)
                );
    }
}
