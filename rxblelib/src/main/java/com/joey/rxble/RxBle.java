package com.joey.rxble;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.polidea.rxandroidble2.RxBleClient;


/**
 * Description: Use this Class to create RxBleOperator and restore state
 * <p>
 * android.permission.ACCESS_COARSE_LOCATION
 * android.permission.ACCESS_FINE_LOCATION
 * android.permission.BLUETOOTH
 * android.permission.BLUETOOTH_ADMIN
 * <p>
 * author:Joey
 * date:2018/8/3
 */
public class RxBle {
    private static boolean initEnable;
    private static RxBleClient sClient;

    public static void init(Context context) {
        if (sClient == null) {
            sClient = RxBleClient.create(context);
        }
    }

    @SuppressLint("MissingPermission")
    public static void markState() {
        initEnable = isEnable();
    }

    /**
     * only use this method when RxBleOperator can't meet the requirements
     */
    public static RxBleClient client() {
        if (sClient == null) throw new IllegalStateException("invoke init before use!");
        return sClient;
    }

    /**
     * 恢复到打开这个应用前的蓝牙状态
     * revert to the original bluetooth state
     */
    @SuppressLint("MissingPermission")
    public static void restoreState() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (initEnable && !adapter.isEnabled()) {
            adapter.enable();
        } else if (!initEnable && adapter.isEnabled()) {
            adapter.disable();
        }
    }

    public static RxBleOperator create(Activity activity) {
        return new RxBleOperator(activity);
    }

    /**
     * @return is bluetooth enable
     */
    @SuppressLint("MissingPermission")
    public static boolean isEnable() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }



}
