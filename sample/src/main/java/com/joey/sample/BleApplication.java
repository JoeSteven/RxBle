package com.joey.sample;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;

import com.joey.rxble.BuildConfig;
import com.joey.rxble.RxBle;
import com.joey.sample.permission.PermissionListener;
import com.joey.sample.permission.PermissionUtil;


/**
 * Description:
 * author:Joey
 * date:2018/8/9
 */
public class BleApplication extends Application implements RxBle.PermissionRequester, Application.ActivityLifecycleCallbacks {
    private Activity topActivity;
    @Override
    public void onCreate() {
        super.onCreate();
        RxBle.init(this, this);
        RxBle.enableLog(BuildConfig.DEBUG);
        RxBle.markState();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void request(RxBle.PermissionListener listener, String... permissions) {
        if (topActivity == null) listener.onDenied();
        PermissionUtil.requestPermission(topActivity, new PermissionListener() {

                    @Override
                    public void permissionGranted(@NonNull String[] permission) {
                        listener.onGranted();
                    }

                    @Override
                    public void permissionDenied(@NonNull String[] permission) {
                        listener.onDenied();
                    }
                },
               permissions);
    }

    @Override
    public boolean hasPermission(String... permissions) {
        return PermissionUtil.hasPermission(topActivity, permissions);
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        topActivity = activity;
    }


    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity instanceof ScanActivity) {
            Log.d("RxBleDemo", "application quit");
            RxBle.restoreState();
            topActivity = null;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }
}
