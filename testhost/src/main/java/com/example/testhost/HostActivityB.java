package com.example.testhost;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class HostActivityB extends Activity {
    private static final String TAG = "HostActivityB";


    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;
    private Button mBtn4;
    private Button mBtn5;
    private Button mBtn6;
    private Button mBtn7;
    private Button mBtn8;

    private ServiceConnection mConn1 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"onServiceConnected() mConn1 ComponentName = " + name);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"onServiceDisconnected() mConn1 ComponentName = " + name);
        }
    };

    private ServiceConnection mConn2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"onServiceConnected() mConn2 ComponentName = " + name);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"onServiceDisconnected() mConn2 ComponentName = " + name);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);


        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("bind host service");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityB.this, HostService.class);
                bindService(intent, mConn1, BIND_AUTO_CREATE);
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("unbind host service");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(mConn1);
            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("start host service");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityB.this, HostService.class);
                startService(intent);
            }
        });

        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn4.setText("stop host service");
        mBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityB.this, HostService.class);
                stopService(intent);
            }
        });

        mBtn5 = (Button) findViewById(R.id.btn5);
        mBtn5.setText("stop self host service");
        mBtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityB.this, HostService.class);
                intent.setAction("stopself");
                startService(intent);
            }
        });

        mBtn6 = (Button) findViewById(R.id.btn6);
        mBtn6.setText("bind host service 2");
        mBtn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityB.this, HostService.class);
//                intent.putExtra("asdas", "asd");
//                intent.setAction("random action " + SystemClock.currentThreadTimeMillis());
                bindService(intent, mConn2, BIND_AUTO_CREATE);
            }
        });

        mBtn7 = (Button) findViewById(R.id.btn7);
        mBtn7.setText("unbind host service 2");
        mBtn7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(mConn2);
            }
        });

        mBtn8 = (Button) findViewById(R.id.btn8);
        mBtn8.setText("start host activity A");
        mBtn8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HostActivityB.this, HostActivityA.class);
                startActivity(intent);
            }
        });

    }
}
