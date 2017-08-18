package com.reginald.pluginm.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by lxy on 17-8-18.
 */

public class ThreadUtils {
    public static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    public static void ensureRunOnMainThread(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            final Object lock = new Object();
            final AtomicBoolean mIsRunning = new AtomicBoolean(false);
            sMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        mIsRunning.set(true);
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }

                }
            });
            if (!mIsRunning.get()) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
