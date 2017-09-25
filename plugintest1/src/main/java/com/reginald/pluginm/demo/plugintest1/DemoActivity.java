package com.reginald.pluginm.demo.plugintest1;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.pluginapi.PluginHelper;

public class DemoActivity extends AppCompatActivity {
    private static final String TAG = "DemoActivity";

    private Button mBtn1;
    private Button mBtn2;

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
                IInvokeResult result = PluginHelper.invokePlugin("com.example.testplugin", "main", "start_main", null, null);
                Log.d(TAG, "invokePlugin() main result = " + (result == null ? "null" :
                        String.format("[ resultCode = %d, result = %s ]", result.getResultCode(), result.getResult())));

                result = PluginHelper.invokePlugin("com.example.testplugin", "main_remote", "start_main", null, null);
                Log.d(TAG, "invokePlugin() main_remote result = " + (result == null ? "null" :
                        String.format("[ resultCode = %d, result = %s ]", result.getResultCode(), result.getResult())));
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("invoke host");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IInvokeResult result = PluginHelper.invokeHost("main", "start_host_main", null, null);
                Log.d(TAG, "invokeHost() result = " + (result == null ? "null" :
                        String.format("[ resultCode = %d, result = %s ]", result.getResultCode(), result.getResult())));
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
