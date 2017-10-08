package com.reginald.pluginm;

import com.reginald.pluginm.stub.StubManager;

/**
 * Created by lxy on 17-9-21.
 */

public class PluginConfigs {
    private static final String TAG = "PluginConfigs";

    /**
     * 独立进程模式： 每个插件分配一个进程。
     */
    public static final int PROCESS_TYPE_INDEPENDENT = StubManager.PROCESS_TYPE_INDEPENDENT;

    /**
     * 单一进程模式: 所有插件都分配在一个固定的进程。
     */
    public static final int PROCESS_TYPE_SINGLE = StubManager.PROCESS_TYPE_SINGLE;

    /**
     * 双进程模式: 所有插件都分配在两个固定的进程，进程名与插件名称相同的在一个特定进程，否则在另一个特定进程。
     */
    public static final int PROCESS_TYPE_DUAL = StubManager.PROCESS_TYPE_DUAL;

    private int mProcessType = PROCESS_TYPE_INDEPENDENT;
    private boolean mUseHostLoader = true;

    public PluginConfigs() {

    }

    public PluginConfigs(PluginConfigs pluginConfigs) {
        mProcessType = pluginConfigs.getProcessType();
        mUseHostLoader = pluginConfigs.isUseHostLoader();
    }

    public int getProcessType() {
        return mProcessType;
    }

    /**
     * 设置进程模式：
     * {@see }
     * @param processType
     * @return
     */
    public PluginConfigs setProcessType(int processType) {
        mProcessType = processType;
        return this;
    }

    public boolean isUseHostLoader() {
        return mUseHostLoader;
    }

    public PluginConfigs setUseHostLoader(boolean useHostLoader) {
        mUseHostLoader = useHostLoader;
        return this;
    }

}
