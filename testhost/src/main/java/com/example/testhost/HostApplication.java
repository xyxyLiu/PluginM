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
                // 插件进程模式, 默认为 PluginConfigs.PROCESS_TYPE_INDEPENDENT。
                .setProcessType(Prefs.Config.getProcessType(this, PluginConfigs.PROCESS_TYPE_COMPLETE))
                // 如果插件中使用插件ClassLoader加载未成功时，是否允许宿主尝试加载。默认为true
                .setUseHostLoader(Prefs.Config.getUseHostClassloader(this, true))
                // 是否在插件进程中对宿主的Context进行hook。默认为true。(部分功能需要开启此选项，例如在webview中加载asset资源...)
                .setHostContextHook(Prefs.Config.getHookHostContext(this, true))
                // 是否在插件进程中对所有系统binder服务进行hook。默认为false。（由于系统不知道插件的存在，所以要在所有插件与系统服务交互
                // 的过程中替换插件的信息为宿主信息，例如包名信息，component信息等。。。)
                .setSystemServicesHook(Prefs.Config.getHookSystemService(this, true))
                // 是否开启签名检测，默认为false
                //.setSignatureCheckEnabled(true)
                // 如果开启签名检测，请填写合法的签名信息. 默认为空
                //.addSignatures(MaybeYourHostSignatures)
                );
    }
}
