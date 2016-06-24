package com.example.testplugin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.multidexmodeplugin.pluginbase.BasePluginActivity;

public class PluginActivityA extends BasePluginActivity {

    private static final String TAG = "PluginActivityA";

    private Button mBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);

        mBtn = (Button) findViewById(R.id.btn);
        mBtn.setText("start activity main");
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginMainActivity.class.getName());
                startActivity(intent);
            }
        });

        showClassloader();
    }

    private void showClassloader() {
        ClassLoader classLoader = getClassLoader();
        Log.d(TAG, "classloader = " + classLoader);
        Log.d(TAG, "parent classloader = " + classLoader.getParent());
    }
}
