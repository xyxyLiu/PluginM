// IPluginServiceStubBinder.aidl
package com.example.multidexmodeplugin;

// Declare any non-default types here with import statements

interface IPluginServiceStubBinder {
//    boolean needConnect();
    ComponentName getComponentName();
    IBinder getBinder();
}
