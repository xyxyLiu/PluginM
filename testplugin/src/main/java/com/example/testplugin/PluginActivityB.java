package com.example.testplugin;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.pluginapi.PluginHelper;

public class PluginActivityB extends Activity {

    private static final String TAG = "PluginActivityB";
    public static final String HOST_PROVIDER_AUTHORITY = "com.example.testhost.provider";
    public static final Uri HOST_CONTENT_URI = Uri.parse("content://" + HOST_PROVIDER_AUTHORITY + "/pluginfirst");

    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;
    private Button mBtn4;
    private Button mBtn5;
    private Button mBtn6;
    private Button mBtn7;
    private Button mBtn8;
    private Button mBtn9;
    private Button mBtn10;
    private Button mBtn11;
    private Button mBtn12;
    private Button mBtn13;

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_activity_b);
        testProviders();
    }

    private void testProviders() {
        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("query plugin back providers");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver contentResolver = getContentResolver();
                Log.d(TAG, "plugin contentResolver = " + contentResolver);
                Cursor cursor = contentResolver.query(PluginBackContentProvider.CONTENT_URI, null, null, null, null);
                Log.d(TAG, "plugin cursor = " + cursor);
                if (cursor != null) {
                    cursor.moveToFirst();
                    Toast.makeText(getApplicationContext(), "plugin cursor first value: \n" + cursor.getString(0), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("query host providers");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver contentResolver = getContentResolver();
                Log.d(TAG, "host contentResolver = " + contentResolver);
                Cursor cursor = contentResolver.query(HOST_CONTENT_URI, null, null, null, null);
                Log.d(TAG, "host cursor = " + cursor);
                if (cursor != null) {
                    cursor.moveToFirst();
                    Toast.makeText(getApplicationContext(), "host cursor first value: \n" + cursor.getString(0), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("test PendingIntent notification");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PluginActivityB.this, PluginActivityA.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(PluginActivityB.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new NotificationCompat.Builder(PluginActivityB.this)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setTicker("plugin notification test")
                        .setColor(getResources().getColor(R.color.notification_color))
                        .setContentTitle("launch " + intent.getComponent().getShortClassName())
                        .setContentText("go go go")
                        .build();
                notification.contentIntent = pendingIntent;
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(PluginActivityB.this);
                notificationManager.notify(101, notification);
            }
        });

        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn4.setText("test PackageManager");
        mBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PackageManager pm = getPackageManager();
                Log.d(TAG, "testPackageManager pm = " + pm);
                try {
                    Log.d(TAG, "testPackageManager pm.getPackageInfo() = " + pm.getPackageInfo(getPackageName(), 0));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    Log.d(TAG, "testPackageManager pm.getApplicationInfo() = " + pm.getApplicationInfo(getPackageName(), 0));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "testPackageManager pm.getInstallerPackageName() = " + pm.getInstallerPackageName(getPackageName()));
                Log.d(TAG, "testPackageManager pm.queryIntentActivities() = " +
                        pm.queryIntentActivities(new Intent(PluginActivityB.this, PluginActivityA.class), 0));
                Log.d(TAG, "testPackageManager pm.queryIntentActivities() action= " +
                        pm.queryIntentActivities(new Intent("action.com.example.testplugin.testA"), 0));
                Log.d(TAG, "testPackageManager pm.getText() = " +
                        pm.getText(getPackageName(), R.string.app_name, null));
            }
        });

        mBtn5 = (Button) findViewById(R.id.btn5);
        mBtn5.setText("test PluginInvoker");
        mBtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "testPackageManager PluginHelper.getPluginPackageName() = " +
                        PluginHelper.getPluginPackageName(PluginActivityB.this));
                Log.d(TAG, "testPackageManager PluginHelper.getHostPackageName() = " +
                        PluginHelper.getHostPackageName(PluginActivityB.this));
                Log.d(TAG, "testPackageManager PluginHelper.getHostContext() = " +
                        PluginHelper.getHostContext(PluginActivityB.this));
                Log.d(TAG, "testPackageManager PluginHelper.invokePlugin() = " +
                        PluginHelper.invoke(PluginHelper.getPluginPackageName(PluginActivityB.this), "main", "test", "paramstest", new IInvokeCallback() {
                            @Override
                            public IInvokeResult onCallback(String params) {
                                Log.d(TAG, "testPackageManager onCallback() params = " + params);
                                return IInvokeResult.INVOKERESULT_VOID_OK;
                            }
                        }));
            }
        });

        mBtn10 = (Button) findViewById(R.id.btn10);
        mBtn10.setText("load native library");
        mBtn10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JniUtils.loadTestJni(PluginActivityB.this);
                    Toast.makeText(PluginActivityB.this, "load library ok!", Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Log.e(TAG, "loadLibrary() error " + t);
                }

            }
        });


        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }
        });

        mWebView.loadUrl("https://github.com/xyxyLiu");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();// 返回前一个页面
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
