package com.reginald.pluginm.utils;

import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lxy on 17-9-22.
 */

public class ConfigUtils {
    public static final String KEY_INVOKER = "pluginm_invoker";
    public static final String KEY_INVOKER_SERVICE = "service";
    public static final String KEY_INVOKER_CLASS = "class";
    public static final String KEY_INVOKER_PROCESS = "process";

    public static Map<String, Map<String, String>> parseInvokerConfig(Bundle metaData) {
        if (metaData != null) {
            String configJson = metaData.getString(KEY_INVOKER);
            if (!TextUtils.isEmpty(configJson)) {
                JSONArray jsonArray = null;
                try {
                    jsonArray = new JSONArray(configJson);
                    int length = jsonArray.length();
                    Map<String, Map<String, String>> map = new HashMap<>(1);
                    for (int i = 0; i < length; i++) {
                        JSONObject item = jsonArray.optJSONObject(i);
                        if (item != null) {
                            String serviceName = item.optString(KEY_INVOKER_SERVICE);
                            String className = item.optString(KEY_INVOKER_CLASS);
                            String processName = item.optString(KEY_INVOKER_PROCESS);
                            if (TextUtils.isEmpty(serviceName) || TextUtils.isEmpty(className)) {
                                throw new IllegalStateException(String.format("Error config %s. 'service' and 'class' MUST be provided!",
                                        configJson));
                            }
                            Map<String, String> config = new HashMap<>(2);
                            config.put(KEY_INVOKER_CLASS, className);
                            config.put(KEY_INVOKER_PROCESS, processName);

                            if (map.containsKey(serviceName)) {
                                throw new IllegalStateException(String.format("Error config %s. duplicated service %s!",
                                        configJson, serviceName));
                            }

                            map.put(serviceName, config);
                        }
                    }
                    return map;
                } catch (JSONException e) {
                    throw new IllegalStateException(String.format("Error config json %s!",
                            configJson));
                }
            }
        }

        return Collections.EMPTY_MAP;
    }
}
