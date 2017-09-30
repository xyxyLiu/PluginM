package com.example.testhost;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.PluginM;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lxy on 17-8-24.
 */

public class DemoActivity extends Activity {

    private static final String TAG = "DemoActivity";
    private static final String PLUGINS_PATH = Environment.getExternalStorageDirectory().getPath() + "/PluginM/";

    private ListView mListView;
    private PluginAdapter mAapter;
    private Button mSelectBtn;
    private Button mTestDemoBtn;
    private ProgressDialog mLoadingDlg = null;
    private File mPluginDir = new File(PLUGINS_PATH);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_layout);

        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mListView = (ListView) findViewById(R.id.plugin_list);
        mAapter = new PluginAdapter();
        mListView.setAdapter(mAapter);

        mSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile(mPluginDir.getAbsolutePath());
            }
        });

        mTestDemoBtn = (Button) findViewById(R.id.test_demo_btn);
        mTestDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginInfo pluginInfo = PluginM.getInstalledPlugin(HostTestActivity.sPluginPkgName);
                if (pluginInfo != null) {
                    Intent intent = new Intent(DemoActivity.this, HostTestActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(DemoActivity.this, "还未安装插件" +
                            HostTestActivity.sPluginPkgName + "！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        refreshData();
    }

    private void refreshData() {
        mAapter.setData(PluginM.getAllInstalledPlugins());
    }


    private void chooseFile(String targetDir) {
        final List<String> apks = getAllPlugins(targetDir);
        if (apks.isEmpty()) {
            Toast.makeText(DemoActivity.this, "请将apk放入" + mPluginDir.getAbsolutePath() + "中", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择一个apk");
        List<String> pkgs = new ArrayList<>();
        for (String apk : apks) {
            PackageInfo pkgInfo = getPackageManager().getPackageArchiveInfo(apk, 0);
            if (pkgInfo != null && !TextUtils.isEmpty(pkgInfo.packageName)) {
                pkgs.add(pkgInfo.packageName);
            }
        }
        builder.setItems(pkgs.toArray(new String[pkgs.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                install(apks.get(which));
            }
        });
        builder.show();
    }

    private void install(final String apkPath) {
        Log.d(TAG, "start install " + apkPath);
        showLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PluginInfo pluginInfo = PluginM.install(apkPath);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoading(false);
                        if (pluginInfo != null) {
                            refreshData();
                            Toast.makeText(DemoActivity.this, pluginInfo.packageName + "install success！",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void showLoading(boolean isShow) {
        if (mLoadingDlg != null && mLoadingDlg.isShowing()) {
            mLoadingDlg.dismiss();
            mLoadingDlg = null;
        }

        if (isShow) {
            mLoadingDlg = new ProgressDialog(this);
            mLoadingDlg.setCancelable(false);
            mLoadingDlg.setMessage("安装中...");
            mLoadingDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mLoadingDlg.show();
        }
    }

    private List<String> getAllPlugins(String targetDirPath) {
        List<String> apkList = new ArrayList<>();
        File targetDir = new File(targetDirPath);
        if (targetDir.exists()) {
            File[] apks = targetDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".apk");
                }
            });
            if (apks != null) {
                for (File file : apks) {
                    apkList.add(file.getAbsolutePath());
                }
            }
        }

        return apkList;
    }


    private class PluginAdapter extends BaseAdapter {

        private List<PluginInfo> mDatas = new ArrayList<>();

        public PluginAdapter() {

        }

        public void setData(List<PluginInfo> datas) {
            mDatas.clear();
            if (datas != null) {
                mDatas.addAll(datas);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mDatas.size();
        }

        @Override
        public Object getItem(int position) {
            return mDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView;
            if (convertView != null) {
                itemView = convertView;
            } else {
                itemView = LayoutInflater.from(DemoActivity.this).inflate(R.layout.plugin_list_item_layout, null);
            }

            TextView textView = (TextView) itemView.findViewById(R.id.title);
            Button btn = (Button) itemView.findViewById(R.id.btn);

            final PluginInfo pluginInfo = mDatas.get(position);

            textView.setText(getPluginShowInfo(pluginInfo));
            btn.setText("load");
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent pluginIntent = new Intent();
                    pluginIntent.setPackage(pluginInfo.packageName);
                    pluginIntent.setAction(Intent.ACTION_MAIN);
                    pluginIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    pluginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Intent intent = PluginM.getPluginActivityIntent(pluginIntent);
                    DemoActivity.this.startActivity(intent);
                }
            });

            return itemView;
        }

        private String getPluginShowInfo(PluginInfo pluginInfo) {
            return String.format("pkg = %s\nversion = %s, %s\nsize = %s",
                    pluginInfo.packageName, pluginInfo.versionName, pluginInfo.versionCode, pluginInfo.fileSize);
        }
    }


}
