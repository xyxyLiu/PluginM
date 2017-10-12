package com.example.testhost;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.reginald.pluginm.PluginConfigs;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.PluginM;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by lxy on 17-8-24.
 */

public class DemoActivity extends Activity {

    private static final String TAG = "DemoActivity";
    private static final String PLUGINS_PATH = Environment.getExternalStorageDirectory().getPath() + "/PluginM/";

    private TextView mConfigText;
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

        mConfigText = (TextView) findViewById(R.id.config_text);
        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mListView = (ListView) findViewById(R.id.plugin_list);
        mAapter = new PluginAdapter();
        mListView.setAdapter(mAapter);

        PluginConfigs pluginConfigs = PluginM.getConfigs();
        mConfigText.setText(pluginConfigs.toString());

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
        showLoading(true, "列表加载中 ...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<PluginInfo> pluginInfos = PluginM.getAllInstalledPlugins();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAapter.setData(pluginInfos);
                        showLoading(false, null);
                    }
                });
            }
        }).start();

    }


    private void chooseFile(String targetDir) {
        final List<String> apks = getAllPlugins(targetDir);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.format("选择一个apk(%s)", PLUGINS_PATH));
        final List<String> pkgs = new ArrayList<>();
        Iterator<String> iterator = apks.iterator();
        while (iterator.hasNext()) {
            String apk = iterator.next();
            PackageInfo pkgInfo = getPackageManager().getPackageArchiveInfo(apk, 0);
            if (pkgInfo != null && !TextUtils.isEmpty(pkgInfo.packageName)) {
                pkgs.add(pkgInfo.packageName);
            } else {
                Toast.makeText(DemoActivity.this, "apk解析错误: " + apk, Toast.LENGTH_SHORT).show();
                iterator.remove();
            }
        }

        if (apks.isEmpty()) {
            Toast.makeText(DemoActivity.this, "未找到合法apk, 请将apk放入" + mPluginDir.getAbsolutePath() + "中", Toast.LENGTH_SHORT).show();
            return;
        }

        builder.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return pkgs.size();
            }

            @Override
            public Object getItem(int position) {
                return pkgs.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View itemView;
                if (convertView != null) {
                    itemView = convertView;
                } else {
                    itemView = LayoutInflater.from(DemoActivity.this).inflate(R.layout.apk_list_item_layout, null);
                }

                TextView textView = (TextView) itemView.findViewById(R.id.title);
                Button installBtn = (Button) itemView.findViewById(R.id.install_btn);
                Button deleteBtn = (Button) itemView.findViewById(R.id.delete_btn);
                final String pkg = (String)getItem(position);
                textView.setText(pkg);
                installBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        install(apks.get(position));
                    }
                });
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isSuc = delete(apks.get(position));
                        if (isSuc && apks.remove(position) != null && pkgs.remove(position) != null) {
                            notifyDataSetChanged();
                        }
                    }
                });

                return itemView;
            }
        }, null);

        builder.show();
    }

    private void install(final String apkPath) {
        Log.d(TAG, "start install " + apkPath);
        showLoading(true, "安装中 ...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PluginInfo pluginInfo = PluginM.install(apkPath, true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoading(false, null);
                        if (pluginInfo != null) {
                            refreshData();
                        }
                        Toast.makeText(DemoActivity.this, "install " + (pluginInfo != null ? pluginInfo.packageName + " ok!" : "error!"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private boolean delete(final String apkPath) {
        File apkFile = new File(apkPath);
        return apkFile.delete();
    }

    private void showLoading(boolean isShow, String msg) {
        if (mLoadingDlg != null && mLoadingDlg.isShowing()) {
            mLoadingDlg.dismiss();
            mLoadingDlg = null;
        }

        if (isShow) {
            mLoadingDlg = new ProgressDialog(this);
            mLoadingDlg.setCancelable(false);
            mLoadingDlg.setMessage(msg);
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

            ImageView iconView = (ImageView) itemView.findViewById(R.id.icon);
            TextView textView = (TextView) itemView.findViewById(R.id.title);
            Button installBtn = (Button) itemView.findViewById(R.id.install_btn);
            Button uninstallBtn = (Button) itemView.findViewById(R.id.uninstall_btn);

            final PluginInfo pluginInfo = mDatas.get(position);

            iconView.setImageDrawable(getPluginIcon(pluginInfo));
            textView.setText(getPluginShowInfo(pluginInfo));
            installBtn.setText("加载");
            installBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent pluginIntent = PluginM.getPluginPackageManager(DemoActivity.this).
                            getLaunchIntentForPackage(pluginInfo.packageName);
                    if (pluginIntent != null) {
                        PluginM.startActivity(DemoActivity.this, pluginIntent);
                    } else {
                        Toast.makeText(DemoActivity.this, pluginInfo.packageName + "未找到入口Intent",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            uninstallBtn.setText("卸载");
            uninstallBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PluginInfo uninstallInfo = PluginM.uninstall(pluginInfo.packageName);
                    Toast.makeText(DemoActivity.this, pluginInfo.packageName + " uninstall " + (uninstallInfo != null ? "ok!" : "error!"),
                            Toast.LENGTH_SHORT).show();
                    refreshData();
                }
            });

            return itemView;
        }

        private String getPluginShowInfo(PluginInfo pluginInfo) {
            PackageManager pluginPM = PluginM.getPluginPackageManager(DemoActivity.this);
            String label = "";
            try {
                label = pluginPM.getApplicationInfo(pluginInfo.packageName, 0).loadLabel(pluginPM).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return String.format("%s\npkg = %s\nversion = %s, %s\nsize = %s",
                    label,pluginInfo.packageName, pluginInfo.versionName, pluginInfo.versionCode, pluginInfo.fileSize);
        }

        private Drawable getPluginIcon(PluginInfo pluginInfo) {
            PackageManager pluginPM = PluginM.getPluginPackageManager(DemoActivity.this);
            try {
                return pluginPM.getApplicationIcon(pluginInfo.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


}
