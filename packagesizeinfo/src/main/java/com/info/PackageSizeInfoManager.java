package com.info;

import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PackageSizeInfoManager {
    private Context context;
    private PackageSizeInfoCallback callback;
    private Handler handler;
    private Runnable callbackRunnable;
    private PackageStats result;

    public static interface PackageSizeInfoCallback {
        void onGetPackageSizeInfoCompleted(PackageStats pStats, boolean succeeded);
    }

    public PackageSizeInfoManager(Context context) {
        this.context = context;
        handler = new Handler();

        callbackRunnable = new Runnable() {
            @Override
            public void run() {
                callback.onGetPackageSizeInfoCompleted(result, true);
            }
        };
    }

    public synchronized void cancel() {
        handler.removeCallbacks(callbackRunnable);
        callbackRunnable = null;
    }

    public void getPkgSize(String pkgName, PackageSizeInfoCallback callback) {
        this.callback = callback;

        try {
            // getPackageSizeInfo是PackageManager中的一个private方法，所以需要通过反射的机制来调用
            Method method = PackageManager.class.getMethod("getPackageSizeInfo",
                    new Class[]{String.class, IPackageStatsObserver.class});
            // 调用 getPackageSizeInfo 方法，需要两个参数：1、需要检测的应用包名；2、回调
            method.invoke(context.getPackageManager(), new Object[]{
                    pkgName,
                    new IPackageStatsObserver.Stub() {
                        @Override
                        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
                            result = pStats;
                            // 子线程中默认无法处理消息循环，自然也就不能显示Toast，所以需要手动Looper一下
                            synchronized (PackageSizeInfoManager.this) {
                                if (callbackRunnable != null)
                                    handler.post(callbackRunnable);
                            }
                        }
                    }
            });
        }catch (Exception e) {
            e.printStackTrace();
            if (callbackRunnable != null)
                callback.onGetPackageSizeInfoCompleted(null, false);

        }
    }

}
