// ITestPluginBinder.aidl
package com.reginald.pluginm.demo.pluginsharelib;

// Declare any non-default types here with import statements
import com.reginald.pluginm.demo.pluginsharelib.PluginItem;

interface ITestPluginBinder {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    String basicTypes(in PluginItem pluginItem);
}
