package com.reginald.pluginm.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @version $Id: PackageUtils.java, v 0.1 2015年12月11日 下午4:41:10 mochuan.zhb Exp $
 * @Author Zheng Haibo
 * @Mail mochuan.zhb@alibaba-inc.com
 * @Company Alibaba Group
 * @PersonalWebsite http://www.mobctrl.net
 * @Description
 */
public class PackageUtils {

    private static final boolean DEBUG = true;
    private static final String TAG = "AssetsApkLoader";

    //从assets复制出去的apk的目标目录
    public static final String APK_DIR = "plugin_apk";

    //文件结尾过滤
    public static final String PLUGIN_ASSET = "plugin.apk";


    /**
     * 将资源文件中的apk文件拷贝到私有目录中
     * @param context
     */
    public static File copyAssetsApk(Context context, String pluginName) {

        AssetManager assetManager = context.getAssets();
        long startTime = System.currentTimeMillis();
        try {
            File dex = context.getDir(APK_DIR, Context.MODE_PRIVATE);
            dex.mkdir();
            String[] fileNames = assetManager.list("");
            for (String fileName : fileNames) {
                if (fileName.equals(pluginName)) {
                    InputStream in = null;
                    OutputStream out = null;
                    in = assetManager.open(fileName);
                    File f = new File(dex, fileName);
                    if (f.exists() && f.length() == in.available()) {
                        Logger.i(TAG, fileName + "no change");
                        return f;
                    }
                    Logger.i(TAG, fileName + " chaneged");

                    copyFile(in, f.getAbsolutePath());
                    Logger.i(TAG, fileName + " copy over");
                    return f;
                }
            }
            Logger.i(TAG, "###copyAssets time = " + (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getApkPath(Context context, String apkName) {
        return context.getDir(APK_DIR, Context.MODE_PRIVATE).getAbsolutePath() + "/" + apkName;
    }

    public static File getOrMakeDir(String root, String dir) {
        File dirFile = new File(root, dir);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        return dirFile;
    }

    public static boolean copyFile(String source, String dest) {
        try {
            return copyFile(new FileInputStream(new File(source)), dest);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean copyFile(final InputStream inputStream, String dest) {
        Logger.d(TAG, "copyFile to " + dest);
        FileOutputStream oputStream = null;
        try {
            File destFile = new File(dest);
            destFile.getParentFile().mkdirs();
            destFile.createNewFile();

            oputStream = new FileOutputStream(destFile);
            byte[] bb = new byte[48 * 1024];
            int len = 0;
            while ((len = inputStream.read(bb)) != -1) {
                oputStream.write(bb, 0, len);
            }
            oputStream.flush();
            Logger.d(TAG, "copyFile to " + dest + " success!");
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "copyFile to " + dest + " error! ", e);
            e.printStackTrace();
        } finally {
            if (oputStream != null) {
                try {
                    oputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static boolean copySo(File sourceDir, String so, String dest) {

        String soDestPath = dest + File.separator + so;

        try {


            if (Build.VERSION.SDK_INT >= 21) {
                String[] abis = Build.SUPPORTED_ABIS;
                if (abis != null) {
                    for (String abi : abis) {
                        Logger.d(TAG, "try supported abi: " + abi);
                        String name = "lib" + File.separator + abi + File.separator + so;
                        File sourceFile = new File(sourceDir, name);
                        if (sourceFile.exists()) {
                            if (copyFile(sourceFile.getAbsolutePath(), soDestPath)) {
                                Logger.d(TAG, "use " + name);
                                return true;
                            }
                            //api21 64位系统的目录可能有些不同
                            //copyFile(sourceFile.getAbsolutePath(), dest + File.separator +  name);
//							break;
                        }
                    }
                }
            } else {
                Logger.d(TAG, "supported api: " + Build.CPU_ABI + " , " + Build.CPU_ABI2);

                String name = "lib" + File.separator + Build.CPU_ABI + File.separator + so;
                File sourceFile = new File(sourceDir, name);

                if (sourceFile.exists() && copyFile(sourceFile.getAbsolutePath(), soDestPath)) {
                    Logger.d(TAG, "use " + name);
                    return true;
                }

                if (Build.CPU_ABI2 != null) {
                    name = "lib" + File.separator + Build.CPU_ABI2 + File.separator + so;
                    sourceFile = new File(sourceDir, name);

                    if (sourceFile.exists() && copyFile(sourceFile.getAbsolutePath(), soDestPath)) {
                        Logger.d(TAG, "use " + name);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        // 默认无匹配时使用第一个提供的abi
        String name = "lib" + File.separator + "armeabi" + File.separator + so;
        File sourceFile = new File(sourceDir, name);
        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                if (copyFile(file.getAbsolutePath() + File.separator + so, soDestPath)) {
                    Logger.w(TAG, "can not found matched abi for " + so + ", use " + file.getName() + " instead!");
                    return true;
                }
            }
        }

        Logger.e(TAG, "can not found matched abi for " + so);
        return false;
    }


    public static Set<String> unZipSo(File apkFile, File tempDir) {

        HashSet<String> result = null;

        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        Logger.d(TAG, "unZipSo " + tempDir.getAbsolutePath());

        ZipFile zfile = null;
        boolean isSuccess = false;
        BufferedOutputStream fos = null;
        BufferedInputStream bis = null;
        try {
            zfile = new ZipFile(apkFile);
            ZipEntry ze = null;
            Enumeration zList = zfile.entries();
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                String relativePath = ze.getName();

                if (!relativePath.startsWith("lib" + File.separator)) {
                    if (DEBUG) {
                        Logger.d(TAG, "skip " + relativePath);
                    }
                    continue;
                }

                if (ze.isDirectory()) {
                    File folder = new File(tempDir, relativePath);
                    if (DEBUG) {
                        Logger.d(TAG, "create dir " + folder.getAbsolutePath());
                    }
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }

                } else {

                    if (result == null) {
                        result = new HashSet<String>(4);
                    }

                    File targetFile = new File(tempDir, relativePath);
                    Logger.d(TAG, "unzip so " + targetFile.getAbsolutePath());
                    if (!targetFile.getParentFile().exists()) {
                        targetFile.getParentFile().mkdirs();
                    }
                    targetFile.createNewFile();

                    fos = new BufferedOutputStream(new FileOutputStream(targetFile));
                    bis = new BufferedInputStream(zfile.getInputStream(ze));
                    byte[] buffer = new byte[2048];
                    int count = -1;
                    while ((count = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                        fos.flush();
                    }
                    fos.close();
                    fos = null;
                    bis.close();
                    bis = null;

                    result.add(relativePath.substring(relativePath.lastIndexOf(File.separator) + 1));
                }
            }
            isSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (zfile != null) {
                try {
                    zfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Logger.d(TAG, "unZipSo finish. isSuccess = " + isSuccess);
        return result;
    }

    public static boolean deleteAll(File file) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles != null && childFiles.length > 0) {
                for (int i = 0; i < childFiles.length; i++) {
                    deleteAll(childFiles[i]);
                }
            }
        }
        Logger.d(TAG, "delete " + file.getAbsolutePath());
        return file.delete();
    }

}
