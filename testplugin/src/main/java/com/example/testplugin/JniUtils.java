package com.example.testplugin;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by lxy on 16-7-13.
 */
public class JniUtils {
    private static final String TAG = "JniUtils";

    public static void loadNativeLib(Context context, String libName) {
        try {
            Log.d(TAG, "System.loadLibrary() test: " + libName);
            System.loadLibrary(libName);
            Log.d(TAG, "native lib load ok! " + libName);

            String assetsLibName = "libTest.so";
            File desFile = copy(context, assetsLibName);
            Log.d(TAG, "System.load() desFile = " + desFile.getAbsolutePath());
            if (!desFile.exists()) {
                Log.e(TAG, "copy assets native lib error! " + assetsLibName);
            } else {
                System.load(desFile.getAbsolutePath());
            }
        } catch (Throwable t) {
            Log.e(TAG, "native lib load error! " + libName + " detail: " + t);
            t.printStackTrace();
        }
    }

    public static void loadTestJni(Context context) {
        loadNativeLib(context, "testjni");
        int result = getsum(11, 22);
        Log.d(TAG, " testjni getsum() = " + result);
    }

    public static File copy(Context context, String filename) throws IOException {
        File desDir = context.getDir("loadtest", MODE_PRIVATE);

        InputStream source = context.getAssets().open(new File(filename).getPath());
        File destinationFile = new File(desDir, filename);
        destinationFile.getParentFile().mkdirs();
        OutputStream destination = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[1024];
        int nread;

        while ((nread = source.read(buffer)) != -1) {
            if (nread == 0) {
                nread = source.read();
                if (nread < 0)
                    break;
                destination.write(nread);
                continue;
            }
            destination.write(buffer, 0, nread);
        }
        destination.close();

        return destinationFile;
    }


    public native static int getsum(int a, int b);
}
