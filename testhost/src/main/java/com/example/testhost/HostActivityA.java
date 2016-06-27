package com.example.testhost;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class HostActivityA extends Activity {
    private static final String TAG = "HostActivityA";


    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);


        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("bind host service");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityA.this, HostService.class);
                bindService(intent, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.d(TAG,"onServiceConnected()");
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.d(TAG,"onServiceDisconnected()");
                    }
                }, BIND_AUTO_CREATE);
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("start host service");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityA.this, HostService.class);
                startService(intent);
            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("kill background process");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityA.this, HostService.class);
                intent.setAction("kill");
                startService(intent);
            }
        });

    }
}
