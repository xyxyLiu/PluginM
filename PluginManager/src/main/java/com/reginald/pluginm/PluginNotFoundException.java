package com.reginald.pluginm;

/**
 * Created by lxy on 17-9-30.
 */

public class PluginNotFoundException extends RuntimeException {
    public PluginNotFoundException()
    {
    }

    public PluginNotFoundException(String message)
    {
        super(message);
    }
}
