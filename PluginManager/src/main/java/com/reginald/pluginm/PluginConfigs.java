package com.reginald.pluginm;

import android.content.pm.Signature;

import com.reginald.pluginm.stub.StubManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * 完整进程模式: 所有插件都完全拥有全部的进程，进程名与插件声明的进程名称一致。（适合加载第三方独立apk）
     */
    public static final int PROCESS_TYPE_COMPLETE = StubManager.PROCESS_TYPE_COMPLETE;

    private int mProcessType = PROCESS_TYPE_INDEPENDENT;
    private boolean mUseHostLoader = true;
    private boolean mHostContextHook = true;
    private boolean mSystemServicesHook = false;
    private final Set<Signature> mSignatures = new HashSet<>();
    private boolean mSignatureCheckEnabled = false;

    public PluginConfigs() {

    }

    public PluginConfigs(PluginConfigs pluginConfigs) {
        mProcessType = pluginConfigs.getProcessType();
        mUseHostLoader = pluginConfigs.isUseHostLoader();
        mHostContextHook = pluginConfigs.isHostContextHook();
        mSystemServicesHook = pluginConfigs.isSystemServicesHook();
        mSignatureCheckEnabled = pluginConfigs.isSignatureCheckEnabled();
        mSignatures.addAll(pluginConfigs.getSignatures());
    }

    public int getProcessType() {
        return mProcessType;
    }

    /**
     * 设置进程模式：
     * {@see {@link #PROCESS_TYPE_INDEPENDENT},
     * {@link #PROCESS_TYPE_SINGLE},
     * {@link #PROCESS_TYPE_DUAL},
     * {@link #PROCESS_TYPE_COMPLETE}}
     * @param processType 进程模式
     * @return
     */
    public PluginConfigs setProcessType(int processType) {
        mProcessType = processType;
        return this;
    }

    public boolean isUseHostLoader() {
        return mUseHostLoader;
    }

    /**
     * 设置是否插件在无法查找到类时使用宿主的classloader继续查找
     * @param useHostLoader
     * @return
     */
    public PluginConfigs setUseHostLoader(boolean useHostLoader) {
        mUseHostLoader = useHostLoader;
        return this;
    }

    public boolean isHostContextHook() {
        return mHostContextHook;
    }

    /**
     * 设置是否对宿主的context进行hook
     * @param hook
     * @return
     */
    public PluginConfigs setHostContextHook(boolean hook) {
        mHostContextHook = hook;
        return this;
    }

    public boolean isSystemServicesHook() {
        return mSystemServicesHook;
    }

    /**
     * 设置是否对除了IActivityManager以外的系统service进行hook, 稳定性可能会降低
     * @param hook
     * @return
     */
    public PluginConfigs setSystemServicesHook(boolean hook) {
        mSystemServicesHook = hook;
        return this;
    }

    public boolean isSignatureCheckEnabled() {
        return mSignatureCheckEnabled;
    }

    /**
     * 设置是否进行插件签名校验
     * @param isEnabled
     * @return
     */
    public PluginConfigs setSignatureCheckEnabled(boolean isEnabled) {
        mSignatureCheckEnabled = isEnabled;
        return this;
    }

    public Set<Signature> getSignatures() {
        return Collections.unmodifiableSet(mSignatures);
    }

    /**
     * 添加插件签名
     * @param signatures 插件签名
     * @return
     */
    public PluginConfigs addSignatures(Signature... signatures) {
        if (signatures != null && signatures.length > 0) {
            Collections.addAll(mSignatures, signatures);
        }
        return this;
    }

    public String toString() {
        String processType = "UNKNOWN";
        switch (mProcessType) {
            case PROCESS_TYPE_INDEPENDENT:
                processType = "INDEPENDENT";
                break;
            case PROCESS_TYPE_SINGLE:
                processType = "SINGLE";
                break;
            case PROCESS_TYPE_DUAL:
                processType = "DUAL";
                break;
            case PROCESS_TYPE_COMPLETE:
                processType = "COMPLETE";
                break;
        }
        return String.format(" PluginConfig[ mProcessType = %s, mUseHostLoader = %b, mHostContextHook = %b, "
                        + "mSystemServicesHook = %b, mSignatureCheckEnabled = %b, mSignatures size = %d ]",
                processType, mUseHostLoader, mHostContextHook, mSystemServicesHook, mSignatureCheckEnabled,
                mSignatures.size());
    }
}
