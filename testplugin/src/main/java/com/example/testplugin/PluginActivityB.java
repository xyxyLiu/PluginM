package com.example.testplugin;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.reginald.pluginm.pluginbase.BasePluginActivity;

public class PluginActivityB extends BasePluginActivity {

    private static final String TAG = "PluginActivityB";

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
        mBtn1.setText("query plugin providers");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver contentResolver = getContentResolver();
                Log.d(TAG, "contentResolver = " + contentResolver);
                Cursor cursor = contentResolver.query(PluginContentProvider.CONTENT_URI, null, null, null, null);
                Log.d(TAG, "cursor = " + cursor);
                if (cursor != null) {
                    cursor.moveToFirst();
                    Toast.makeText(getApplicationContext(), "cursor first value: \n" + cursor.getString(0), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtn10 = (Button) findViewById(R.id.btn10);
        mBtn10.setText("load native library");
        mBtn10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JniUtils.loadTestJni();
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

        mWebView.loadUrl("http://www.baidu.com");
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
