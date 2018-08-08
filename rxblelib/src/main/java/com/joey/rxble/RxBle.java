package com.joey.rxble;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.support.annotation.MainThread;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.internal.RxBleLog;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;


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
    private static PublishSubject<RxBleClient.State> sStatePublishSubject;

    public static void init(Context context) {
        RxJavaPlugins.setErrorHandler(throwable -> {
            Log.e("RxBleDemo", "don't crash:" + throwable.toString());
        });
        if (sClient == null) {
            sClient = RxBleClient.create(context);
        }
        observeState();
    }

    private static void observeState() {
        client().observeStateChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(RxBle::dispatchState);
    }

    private static void dispatchState(RxBleClient.State state) {
        if (sStatePublishSubject != null && sStatePublishSubject.hasObservers()) {
            sStatePublishSubject.onNext(state);
        }
    }

    /**
     * register observer to observe bluetooth state change
     */
    @MainThread
    public static PublishSubject<RxBleClient.State> registerState() {
        if (sStatePublishSubject == null) sStatePublishSubject = PublishSubject.create();
        return sStatePublishSubject;
    }

    public static void enableLog(boolean enable) {
        if (enable) {
            RxBleLog.setLogLevel(RxBleLog.VERBOSE);
        } else {
            RxBleLog.setLogLevel(RxBleLog.NONE);
        }
    }

    /**
     * mark bluetooth state when application start
     */
    @SuppressLint("MissingPermission")
    public static void markState() {
        initEnable = isEnable();
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

    /**
     * only use this method when RxBleOperator can't meet the requirements
     */
    public static RxBleClient client() {
        if (sClient == null) throw new IllegalStateException("invoke init before use!");
        return sClient;
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


    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE
                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

}
