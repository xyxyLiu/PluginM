package com.example.testplugin;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
    private Button mBtn4;
    private Button mBtn5;
    private Button mBtn6;

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected() ComponentName = " + name + ", IBinder = " + service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected() ComponentName = " + name);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);

        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("start plugin activity main");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginMainActivity.class.getName());
                startActivity(intent);
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("start plugin service");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginService.class.getName());
                startService(intent);
            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("stop plugin service");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginService.class.getName());
                stopService(intent);
            }
        });

        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn4.setText("bind plugin service");
        mBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginService.class.getName());
                bindService(intent, mConn, BIND_AUTO_CREATE);
            }
        });

        mBtn5 = (Button) findViewById(R.id.btn5);
        mBtn5.setText("unbind plugin service");
        mBtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(mConn);
            }
        });

        mBtn6 = (Button) findViewById(R.id.btn6);
        mBtn6.setText("kill process");
        mBtn6.setOnClickListener(new View.OnClickListener() {
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
