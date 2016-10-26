package com.example.testplugin;

import android.util.Log;

/**
 * Created by lxy on 16-7-13.
 */
public class JniUtils {
    private static final String TAG = "JniUtils";

    public static void loadNativeLib(String libName) {
        try {
            System.loadLibrary(libName);
            Log.d(TAG, "native lib load ok! " + libName);
        } catch (Throwable t) {
            Log.e(TAG, "native lib load error! " + libName + " detail: " + t);
            t.printStackTrace();
        }
    }

    public static void loadTestJni() {
        loadNativeLib("testjni");
        int result = getsum(11, 22);
        Log.d(TAG, " testjni getsum() = " + result);
    }

    public native static int getsum(int a, int b);
}
