package com.example.testplugin;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.multidexmodeplugin.pluginbase.BasePluginActivity;
import com.nineoldandroids.animation.AnimatorSet;

public class PluginMainActivity extends BasePluginActivity {

    static final String TAG = "PluginMainActivity";

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
    }
}
