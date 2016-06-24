package com.example.testplugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.multidexmodeplugin.pluginbase.BasePluginActivity;
import com.nineoldandroids.animation.AnimatorSet;

public class PluginMainActivity extends BasePluginActivity {

    private static final String TAG = "PluginMainActivity";

    private Button mBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_main);
//        TextView textView = new TextView(this);
//        textView.setText("hello!!!!");
//        setContentView(textView);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.cancel();

        Log.d(TAG, "AnimatorSet.class = " + AnimatorSet.class);
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());


        mBtn = (Button) findViewById(R.id.btn);
        mBtn.setText("start activity A");
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginActivityA.class.getName());
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
