package com.example.testhost;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.multidexmodeplugin.DexClassLoaderPluginManager;
import com.example.multidexmodeplugin.MultiDexPluginManager;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;

import java.lang.reflect.Method;

public class HostMainActivity extends AppCompatActivity {

    static final String TAG = "HostMainActivity";

    private TextView mLoadModeText;
    private Button mBtn;

    private DexClassLoaderPluginManager mDexClassLoaderPluginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_main);
        initViews();

//        testMultiDexModePlugin();
        testDexCLassLoaderModePlugin(true);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.cancel();
        Log.d(TAG, "AnimatorSet.class = " + AnimatorSet.class);
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());
    }

    private void initViews() {
        mBtn = (Button) findViewById(R.id.btn_start_act);
        mLoadModeText = (TextView) findViewById(R.id.plugin_load_mode);
    }

    private void testMultiDexModePlugin() {
        mLoadModeText.setText("loadmode = MultiDex");
        MultiDexPluginManager.install(getApplicationContext(), "testplugin-debug.apk");
        try {
            Class<?> clazz = Class.forName("com.example.testplugin.TestUtils");

            Object testUtilsObj = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("test");
            Log.d(TAG, "testPlugin success! \n testUtilsObj.test() = " + method.invoke(testUtilsObj));


            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Plugin activity must be declared in manifest
                    Intent pluginIntent = new Intent();
                    pluginIntent.setClassName(HostMainActivity.this, "com.example.testplugin.PluginMainActivity");
                    startActivity(pluginIntent);
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testDexCLassLoaderModePlugin(boolean isStandAlone) {
        mDexClassLoaderPluginManager = DexClassLoaderPluginManager.getInstance(getApplicationContext());

        mLoadModeText.setText("loadmode = DexCLassLoader");
        mDexClassLoaderPluginManager.install("testplugin-debug.apk", isStandAlone);
        try {
            Class<?> clazz = mDexClassLoaderPluginManager.loadPluginClass("com.example.testplugin.TestUtils");

            Object testUtilsObj = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("test");
            Log.d(TAG, "testPlugin success! \n testUtilsObj.test() = " + method.invoke(testUtilsObj));

            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent pluginIntent = new Intent();
//                    pluginIntent.setClassName(HostMainActivity.this, "com.example.testplugin.PluginMainActivity");
//                    startActivity(pluginIntent);
                    Intent intent = new Intent();
                    intent.setClassName("com.example.testplugin", "com.example.testplugin.PluginMainActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mDexClassLoaderPluginManager.startPluginActivity(intent);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
