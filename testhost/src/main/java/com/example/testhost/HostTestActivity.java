package com.example.testhost;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.reginald.pluginm.PluginM;
import com.reginald.pluginm.pluginapi.IInvokeResult;

/**
 * Created by lxy on 17-9-29.
 */

public class HostTestActivity extends Activity {

    public static final String sPluginPkgName = "com.example.testplugin";

    private static final int REQUEST_CODE_PLUGIN = 1;

    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;
    private Button mBtn4;
    private Button mBtn5;
    private Button mBtn6;
    private Button mBtn7;
    private Button mBtn8;
    private Button mBtn9;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(HostTestActivity.this, "onServiceConnected name = " + name
                    , Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(HostTestActivity.this, "onServiceDisconnected name =" + name
                    , Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);


        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("start plugin activity");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComponentName componentName = new ComponentName(sPluginPkgName, "com.example.testplugin.PluginActivityA");
                Intent intent = new Intent();
                intent.setComponent(componentName);
                PluginM.startActivity(HostTestActivity.this, intent);

            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("start plugin service");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComponentName componentName = new ComponentName(sPluginPkgName, "com.example.testplugin.PluginService");
                Intent intent = new Intent();
                intent.setComponent(componentName);

                ComponentName serviceName = PluginM.startService(HostTestActivity.this, intent);
                Toast.makeText(HostTestActivity.this, "startService ok! componentName = " +
                        serviceName, Toast.LENGTH_SHORT).show();

            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("stop plugin service");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComponentName componentName = new ComponentName(sPluginPkgName, "com.example.testplugin.PluginService");
                Intent intent = new Intent();
                intent.setComponent(componentName);

                boolean isSuc = PluginM.stopService(HostTestActivity.this, intent);
                Toast.makeText(HostTestActivity.this, "stopService isSuc = " +
                        isSuc, Toast.LENGTH_SHORT).show();

            }
        });

        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn4.setText("bind plugin service");
        mBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComponentName componentName = new ComponentName(sPluginPkgName, "com.example.testplugin.PluginService");
                Intent intent = new Intent();
                intent.setComponent(componentName);
                boolean isSuc = PluginM.bindService(HostTestActivity.this, intent, serviceConnection,  Context.BIND_AUTO_CREATE);

                Toast.makeText(HostTestActivity.this, "bindService " + (isSuc ? "ok!" : "error!")
                        , Toast.LENGTH_SHORT).show();

            }
        });

        mBtn5 = (Button) findViewById(R.id.btn5);
        mBtn5.setText("unbind plugin service");
        mBtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginM.unbindService(HostTestActivity.this, serviceConnection);
                Toast.makeText(HostTestActivity.this, "unbindService", Toast.LENGTH_SHORT).show();
            }
        });

        mBtn6 = (Button) findViewById(R.id.btn6);
        mBtn6.setText("start plugin broadcast");
        mBtn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("action.plugin.broadcast.test1");
//                intent.setPackage(sPluginPkgName);
                sendBroadcast(intent);
            }
        });

        mBtn7 = (Button) findViewById(R.id.btn7);
        mBtn7.setText("query plugin provider");
        mBtn7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String AUTHORITY = "com.example.testplugin.testprovider.remote";
                Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/pluginfirst");
                ContentResolver pluginContentResolver = PluginM.getContentResolver(HostTestActivity.this);
                Cursor cursor = pluginContentResolver.query(CONTENT_URI, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    Toast.makeText(getApplicationContext(), "plugin cursor first value: \n" + cursor.getString(0), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtn8 = (Button) findViewById(R.id.btn8);
        mBtn8.setText("start plugin invoker");
        mBtn8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IInvokeResult result = PluginM.invoke(sPluginPkgName, "main", "start_main", null, null);
                Toast.makeText(getApplicationContext(), "invoke plugin invoker result: " +
                        (result == null ? "null" : String.format("[ resultCode = %d, result = %s ]",
                                result.getResultCode(), result.getResult())), Toast.LENGTH_SHORT).show();

            }
        });

        mBtn9 = (Button) findViewById(R.id.btn9);
        mBtn9.setText("start plugin activity for result");
        mBtn9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComponentName componentName = new ComponentName(sPluginPkgName, "com.example.testplugin.PluginActivityA");
                Intent intent = new Intent();
                intent.setComponent(componentName);
                PluginM.startActivityForResult(HostTestActivity.this, intent, REQUEST_CODE_PLUGIN);

            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Toast.makeText(this, "onActivityResult() requestCode = " + requestCode + " ,resultCode = " + resultCode,
                Toast.LENGTH_SHORT).show();
    }
}
