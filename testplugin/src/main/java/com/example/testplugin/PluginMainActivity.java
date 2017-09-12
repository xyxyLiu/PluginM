package com.example.testplugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nineoldandroids.animation.AnimatorSet;

import java.util.ArrayList;
import java.util.List;

public class PluginMainActivity extends Activity {

    private static final String TAG = "PluginMainActivity";

    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;
    private Button mBtn4;
    private Button mBtn5;

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

        Log.d(TAG, "onCreate() getBaseContext() = " + getBaseContext());
        Log.d(TAG, "onCreate() getPackageName() = " + getPackageName());
        Log.d(TAG, "onCreate() getResource() = " + getResources());
        Log.d(TAG, "onCreate() getClassLoader() = " + getClassLoader());
        Log.d(TAG, "onCreate() getClass().getClassLoader() = " + getClass().getClassLoader());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.cancel();

        Log.d(TAG, "AnimatorSet.class = " + AnimatorSet.class);
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());
        Log.d(TAG, "AnimatorSet.class.hashCode() = " + AnimatorSet.class.hashCode());


        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("start plugin activity A with plugin extras");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PluginMainActivity.this, PluginActivityA.class);
                intent.putExtra("plugin_obj", new PluginObject("lavazza"));
                intent.setData(PluginContentProvider.CONTENT_URI);
                startActivity(intent);
            }
        });

        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn4.setText("start plugin activity B");
        mBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PluginMainActivity.this, PluginActivityB.class);
                startActivity(intent);
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("start host activity with action");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("action.com.example.testplugin.DemoActivity");
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

        mBtn5 = (Button) findViewById(R.id.btn5);
        mBtn5.setText("launch other plugins");
        mBtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchOtherPlugins();
            }
        });

        broadcastTest();
        showClassloader();

        //test plugin class:
        CustomClassA objA = new CustomClassA(1, "Hi");
        Log.d(TAG, "test CustomClassA " + objA);
    }

    private void launchOtherPlugins() {
        final PackageManager pm = getPackageManager();
        List<PackageInfo> pkgList = pm.getInstalledPackages(0);
        if (pkgList == null) {
            Toast.makeText(PluginMainActivity.this, "getInstalledPackages is null!", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择一个应用：");
        final List<String> pkgs = new ArrayList<>();
        for (PackageInfo pkgInfo : pkgList) {
            if (pkgInfo != null && !TextUtils.isEmpty(pkgInfo.packageName)) {
                pkgs.add(pkgInfo.packageName);
            }
        }
        builder.setItems(pkgs.toArray(new String[pkgs.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pkg = pkgs.get(which);
                Intent intent = pm.getLaunchIntentForPackage(pkg);
                if (intent == null) {
                    Toast.makeText(PluginMainActivity.this, "NO launch intent found for " + pkg, Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(intent);
            }
        });
        builder.show();
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
