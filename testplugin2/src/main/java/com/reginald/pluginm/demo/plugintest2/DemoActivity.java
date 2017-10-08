package com.reginald.pluginm.demo.plugintest2;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.reginald.pluginm.demo.pluginsharelib.ITestPluginBinder;
import com.reginald.pluginm.demo.pluginsharelib.ITestServiceBinder;
import com.reginald.pluginm.demo.pluginsharelib.PluginItem;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.pluginapi.PluginHelper;

public class DemoActivity extends AppCompatActivity {
    private static final String TAG = "DemoActivity";

    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;
    private Button mBtn4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        setTitle("plugin action bar");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("invoke testplugin");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IInvokeResult result = PluginHelper.invoke("com.example.testplugin", "main", "start_main", null, null);
                Log.d(TAG, "invoke() main result = " + (result == null ? "null" :
                        String.format("[ resultCode = %d, result = %s ]", result.getResultCode(), result.getResult())));
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("invoke host");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IInvokeResult result = PluginHelper.invoke(PluginHelper.getHostPackageName(DemoActivity.this),
                        "main", "start_host_main", null, null);
                Log.d(TAG, "invokeHost() result = " + (result == null ? "null" :
                        String.format("[ resultCode = %d, result = %s ]", result.getResultCode(), result.getResult())));
            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("fetch host binder");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IBinder iHostBinder = PluginHelper.fetchService(PluginHelper.getHostPackageName(DemoActivity.this), "main");
                ITestServiceBinder iTestServiceBinder = ITestServiceBinder.Stub.asInterface(iHostBinder);
                if (iTestServiceBinder != null) {
                    try {
                        iTestServiceBinder.test("www.baidu.com");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "fetchService() iTestServiceBinder = " + iTestServiceBinder);
            }
        });

        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn4.setText("fetch plugin binder");
        mBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IBinder iPluginBinder = PluginHelper.fetchService("com.example.testplugin", "main");
                ITestPluginBinder iTestBinder = ITestPluginBinder.Stub.asInterface(iPluginBinder);
                if (iTestBinder != null) {
                    try {
                        String result = iTestBinder.basicTypes(new PluginItem("my plugin item", 2));
                        Toast.makeText(DemoActivity.this, "test plugin binder result = " + result, Toast.LENGTH_SHORT).show();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "fetchService() iTestBinder = " + iTestBinder);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
