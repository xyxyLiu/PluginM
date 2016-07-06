package com.example.testplugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.reginald.pluginm.pluginbase.BasePluginActivity;
import com.nineoldandroids.animation.AnimatorSet;

public class PluginMainActivity extends BasePluginActivity {

    private static final String TAG = "PluginMainActivity";

    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;

    public static final String BROADCAST_ACTION_1 = "plugin_broadcast_test_1";
    public static final String BROADCAST_ACTION_2 = "plugin_broadcast_test_2";

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Plugin BroadcastReceiver.onReceive() context = " + context + " ,intent = " + intent);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_main);


        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.cancel();

        Log.d(TAG, "AnimatorSet.class = " + AnimatorSet.class);
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());


        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("start plugin activity A");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginActivityA.class.getName());
                startActivity(intent);
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("start host activity");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.HOST_PACKAGE_NAME, "com.example.testhost.HostMainActivity");
                startActivity(intent);
            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("start plugin activity with action");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("action.com.example.testplugin.testA");
                startActivity(intent);
            }
        });

        broadcastTest();
        showClassloader();
    }

    private void broadcastTest() {
        registerReceiver(mBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_1));
        registerReceiver(mBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_2));
    }

    private void showClassloader() {
        ClassLoader classLoader = getClassLoader();
        Log.d(TAG, "classloader = " + classLoader);
        Log.d(TAG, "parent classloader = " + classLoader.getParent());
    }
}
