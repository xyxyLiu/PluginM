package com.example.testhost;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.PluginManager;
import com.reginald.pluginm.PluginManagerNative;
import com.reginald.pluginm.MultiDexPluginManager;
import com.nineoldandroids.animation.AnimatorSet;

import java.lang.reflect.Method;

public class HostMainActivity extends AppCompatActivity {

    static final String TAG = "HostMainActivity";

    private TextView mLoadModeText;
    private Button mBtn1;
    private Button mBtn1_1;
    private Button mBtn1_2;
    private Button mBtn1_3;

    private Button mBtn2;
    private Button mBtn3;
    private Button mBtn4;

    private PluginManagerNative mPluginManagerNative;

    public static final String BROADCAST_ACTION_1 = "host_broadcast_test_1";
    public static final String BROADCAST_ACTION_2 = "host_broadcast_test_2";

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver.onReceive() intent = " + intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_main);
        initViews();

//        testMultiDexModePlugin();
        testDexCLassLoaderModePlugin();

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.cancel();
        Log.d(TAG, "AnimatorSet.class = " + AnimatorSet.class);
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());

        broadcastTest();
    }

    private void broadcastTest() {
        registerReceiver(mBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_1));
        registerReceiver(mBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_2));
    }

    private void initViews() {
        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn1_1 = (Button) findViewById(R.id.btn1_1);
        mBtn1_2 = (Button) findViewById(R.id.btn1_2);
        mBtn1_3 = (Button) findViewById(R.id.btn1_3);
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


            mBtn1.setOnClickListener(new View.OnClickListener() {
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

    private void testDexCLassLoaderModePlugin() {
        mPluginManagerNative = PluginManagerNative.getInstance(getApplicationContext());
//
//        mLoadModeText.setText("loadmode = DexCLassLoader");
        String pluginPackageName = "com.example.testplugin";
        PluginInfo pluginInfo = mPluginManagerNative.install(pluginPackageName);
//
        Toast.makeText(this, "plugin " + pluginPackageName + " loadPlugin " + (pluginInfo != null ? "ok!":"error!"), Toast.LENGTH_SHORT).show();

        try {
//            Class<?> clazz = mPluginManagerNative.loadPluginClass("com.example.testplugin", "com.example.testplugin.TestUtils");
//
//            Object testUtilsObj = clazz.newInstance();
//            Method method = clazz.getDeclaredMethod("test");
//            Log.d(TAG, "testPlugin success! \n testUtilsObj.test() = " + method.invoke(testUtilsObj));

            mBtn1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent pluginIntent = new Intent();
//                    pluginIntent.setClassName("com.example.testplugin", "com.example.testplugin.PluginMainActivity");
                    pluginIntent.setPackage("com.example.testplugin");
                    pluginIntent.setAction(Intent.ACTION_MAIN);
                    pluginIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    pluginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Intent intent = mPluginManagerNative.getPluginActivityIntent(pluginIntent);
                    HostMainActivity.this.startActivity(intent);
                }
            });

            mBtn1_1.setText("start plugin activity with action");
            mBtn1_1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent pluginIntent = new Intent("action.com.example.testplugin.testA");
                    pluginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Intent intent = mPluginManagerNative.getPluginActivityIntent(pluginIntent);
                    HostMainActivity.this.startActivity(intent);
                }
            });

            mBtn1_2.setText("install & start wifimgr");
            mBtn1_2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent pluginIntent = new Intent("plugin_broadcast_test_2");
//                    sendBroadcast(pluginIntent);
//                    Intent hostIntent = new Intent("host_broadcast_test");
//                    sendBroadcast(hostIntent);

                    String wifiPluginPkg =  "com.youba.WeatherForecast"; //"com.dianxinos.optimizer.plugin.wifimgr"; //
                    final PluginInfo installedPluginInfo = mPluginManagerNative.install(wifiPluginPkg);
                    Toast.makeText(HostMainActivity.this, wifiPluginPkg + " install " + (installedPluginInfo != null ? "ok!":"error!"),
                            Toast.LENGTH_SHORT).show();

                    if (installedPluginInfo == null) {
                        return;
                    }

                    Intent pluginIntent = new Intent();
                    pluginIntent.setPackage(wifiPluginPkg);
                    pluginIntent.setAction(Intent.ACTION_MAIN);
                    pluginIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    pluginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Intent intent = mPluginManagerNative.getPluginActivityIntent(pluginIntent);
                    HostMainActivity.this.startActivity(intent);
                }
            });

            mBtn1_3.setText("send plugin static broadcast");
            mBtn1_3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent pluginIntent = new Intent("action.plugin.broadcast.test2");
                    sendBroadcast(pluginIntent);
                }
            });

            mBtn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClassName(HostMainActivity.this, "com.example.testhost.HostActivityA");
                    startActivity(intent);
                }
            });

            mBtn3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClassName(HostMainActivity.this, "com.example.testhost.HostActivityB");
                    startActivity(intent);
                }
            });

            mBtn4.setText("bind host service");
            mBtn4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HostMainActivity.this, HostService.class);
                    bindService(intent, new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            Log.d(TAG,"onServiceConnected() " + this);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            Log.d(TAG,"onServiceDisconnected()" + this);
                        }
                    }, BIND_AUTO_CREATE);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
