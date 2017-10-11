package com.reginald.pluginm.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PackageUtils {

    private static final boolean DEBUG = true;
    private static final String TAG = "PackageUtils";

    public static final String PLUGIN_ROOT = "pluginm";
    public static final String PLUGIN_APK_FOLDER_NAME = "apk";
    public static final String PLUGIN_APK_FILE_NAME = "base.apk";
    public static final String PLUGIN_DEX_FOLDER_NAME = "dexes";
    public static final String PLUGIN_LIB_FOLDER_NAME = "lib";
    public static final String PLUGIN_SIGNATURE_FOLDER_NAME = "signatures";
    public static final String PLUGIN_SIGNATURE_FILE_PREFIX = "sig_";

    public static File getPluginRootDir(Context context) {
        return context.getDir(PLUGIN_ROOT, Context.MODE_PRIVATE);
    }

    public static File getPluginDir(Context context, String packageName) {
        return new File(getPluginRootDir(context), packageName);
    }

    public static List<File> getSignatureFiles(Context context, String packageName) {
        File sigFile = new File(makePluginDir(context, packageName), PLUGIN_SIGNATURE_FOLDER_NAME);
        if (sigFile.exists()) {
            List<File> sigFiles = new ArrayList<>();
            for (File file : sigFile.listFiles()) {
                if (file.getName().startsWith(PLUGIN_SIGNATURE_FILE_PREFIX)) {
                    sigFiles.add(file);
                }
            }
            return sigFiles;
        }
        return Collections.emptyList();
    }

    public static File makePluginDir(Context context, String packageName) {
        return getOrMakeDir(getPluginRootDir(context), packageName);
    }

    public static File makePluginApkDir(Context context, String packageName) {
        return getOrMakeDir(makePluginDir(context, packageName), PLUGIN_APK_FOLDER_NAME);
    }

    public static File makePluginDexDir(Context context, String packageName) {
        return getOrMakeDir(makePluginDir(context, packageName), PLUGIN_DEX_FOLDER_NAME);
    }

    public static File makePluginLibDir(Context context, String packageName) {
        return getOrMakeDir(makePluginDir(context, packageName), PLUGIN_LIB_FOLDER_NAME);
    }

    public static File makePluginSignatureDir(Context context, String packageName) {
        return getOrMakeDir(makePluginDir(context, packageName), PLUGIN_SIGNATURE_FOLDER_NAME);
    }

    public static File getOrMakeDir(File root, String dir) {
        File dirFile = new File(root, dir);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        return dirFile;
    }

    public static boolean writeToFile(File file, byte[] data) {
        FileOutputStream fou = null;
        try {
            fou = new FileOutputStream(file);
            fou.write(data);
            return true;
        } catch (IOException e) {
            Logger.e(TAG, "writeToFile() error!", e);
        } finally{
            if (fou != null) {
                try {
                    fou.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    public static byte[] readFromFile(File file) {
        FileInputStream fin = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            fin = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = fin.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            byte[] data = out.toByteArray();
            out.close();
            return data;
        } catch (IOException e) {
            Logger.e(TAG, "readFromFile() error!", e);
        }  finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    public static boolean copyFile(String source, String dest) {
        try {
            return copyFile(new FileInputStream(new File(source)), dest);
        } catch (FileNotFoundException e) {
            Logger.e(TAG, "copyFile() error!", e);
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
            Logger.e(TAG, "copyFile() error!", e);
        } finally {
            if (oputStream != null) {
                try {
                    oputStream.close();
                } catch (IOException e) {
                    Logger.e(TAG, "copyFile() oputStream close error!", e);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Logger.e(TAG, "copyFile() inputStream close error!", e);
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
            Logger.e(TAG, "copySo() error!", e);
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
            Logger.e(TAG, "unZipSo() error!", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Logger.e(TAG, "unZipSo() error!", e);
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    Logger.e(TAG, "unZipSo() error!", e);
                }
            }
            if (zfile != null) {
                try {
                    zfile.close();
                } catch (IOException e) {
                    Logger.e(TAG, "unZipSo() error!", e);
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

    public static boolean saveSignatures(Context context, PackageInfo pkgInfo) {
        File sigDir = makePluginSignatureDir(context, pkgInfo.packageName);
        Signature[] signatures = pkgInfo.signatures;
        if (signatures == null || signatures.length <= 0) {
            return false;
        }

        for (int i = 0; i < signatures.length; i++) {
            Signature signature = signatures[i];
            File sigFile = new File(sigDir, PLUGIN_SIGNATURE_FILE_PREFIX + i);
            if (!writeToFile(sigFile, signature.toByteArray())) {
                Logger.e(TAG, "saveSignatures() error!");
                deleteAll(sigDir);
                return false;
            }
        }

        Logger.d(TAG, String.format("saveSignatures() save %d signature(s) for %s!",
                signatures.length, pkgInfo.packageName));
        return true;
    }

    public static Signature[] readSignatures(Context context, String packageName) {
        List<File> sigFiles = getSignatureFiles(context, packageName);
        if (sigFiles.isEmpty()) {
            return null;
        }

        List<Signature> signatures = new ArrayList<>();
        for (File sigFile : sigFiles) {
            byte[] sigData = readFromFile(sigFile);
            if (sigData == null) {
                Logger.e(TAG, "saveSignatures() error in reading " + sigFile.getAbsolutePath());
                return null;
            }
            Signature signature = new Signature(sigData);
            signatures.add(signature);
        }

        Logger.d(TAG, String.format("writeSignatures() save %d signature(s) for %s!",
                signatures.size(), packageName));
        return signatures.toArray(new Signature[signatures.size()]);
    }

    public static boolean checkSignatures(Signature[] signatures, Signature[] checkers) {
        if (signatures == null || checkers == null) {
            Logger.e(TAG, "checkSignatures() null input!");
            return false;
        }

        for (Signature signature : signatures) {
            boolean match = false;
            for (Signature check : checkers) {
                if (signature.equals(check)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                Logger.e(TAG, String.format("checkSignatures() signature %s is not approved in %s !",
                        signature, Arrays.asList(checkers)));
                return false;
            }
        }

        Logger.d(TAG, String.format("checkSignatures() signature(s) %s are approved!",
                Arrays.asList(signatures)));
        return true;
    }

}
