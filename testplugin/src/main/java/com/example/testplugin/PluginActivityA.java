package com.example.testplugin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.reginald.pluginm.pluginbase.BasePluginActivity;

public class PluginActivityA extends BasePluginActivity {

    private static final String TAG = "PluginActivityA";

    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);

        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("start activity main");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginMainActivity.class.getName());
                startActivity(intent);
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("start service");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginService.class.getName());
                startService(intent);
            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("kill process");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.os.Process.killProcess(Process.myPid());
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
