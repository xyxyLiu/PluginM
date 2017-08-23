// IPluginServiceStubBinder.aidl
package com.reginald.pluginm.stub;

// Declare any non-default types here with import statements

interface IPluginServiceStubBinder {
//    boolean needConnect();
    ComponentName getComponentName();
    IBinder getBinder();
}
