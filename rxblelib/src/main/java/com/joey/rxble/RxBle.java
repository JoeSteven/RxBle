package com.joey.rxble;


import android.Manifest;
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
    private static PermissionRequester sRequester;
    private static final String[] PERMISSION_FOR_BLUETOOTH = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN};

    public static void init(Context context, PermissionRequester requester) {
        if (sClient == null) {
            sClient = RxBleClient.create(context);
        }
        if (requester != null && sRequester == null) {
            sRequester = requester;
        }
        observeState();
    }

    @SuppressLint("CheckResult")
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
     * @return support request permission
     */
    public static boolean requestPermission(PermissionListener listener) {
        if (sRequester != null) {
            sRequester.request(listener, PERMISSION_FOR_BLUETOOTH);
            return true;
        }
        return false;
    }

    public static boolean hasPermission() {
        return sRequester == null || sRequester.hasPermission(PERMISSION_FOR_BLUETOOTH);
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


    public static RxBleOperator create() {
        return new RxBleOperator();
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

    public static boolean isCharacteristicIndicatable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
    }

    public interface PermissionRequester {
        void request(PermissionListener listener, String... permissions);

        boolean hasPermission(String... permissions);
    }

    public interface PermissionListener {
        void onGranted();

        void onDenied();
    }

}
