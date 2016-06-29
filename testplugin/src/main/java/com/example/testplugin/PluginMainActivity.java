package com.example.testplugin;

import android.content.Intent;
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

        showClassloader();
    }

    private void showClassloader() {
        ClassLoader classLoader = getClassLoader();
        Log.d(TAG, "classloader = " + classLoader);
        Log.d(TAG, "parent classloader = " + classLoader.getParent());
    }
}
