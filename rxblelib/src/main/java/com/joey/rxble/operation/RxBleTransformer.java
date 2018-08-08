package com.joey.rxble.operation;

import android.util.Log;

import com.joey.rxble.RxBleOperator;
import com.polidea.rxandroidble2.NotificationSetupMode;

import java.util.UUID;

/**
 * Description:
 * author:Joey
 * date:2018/8/8
 */
public class RxBleTransformer {
    public static RxBleReadCharacteristicFunc readCharacteristic(String macAddress, UUID uuid, RxBleOperator operator){
        Log.d("RxBleDemo", "flat map single create func:" + System.currentTimeMillis());
        return new RxBleReadCharacteristicFunc(macAddress, uuid, operator);
    }

    public static RxBleNotifyCharacteristicFunc notifyCharacteristic(String macAddress, UUID uuid, NotificationSetupMode mode, RxBleOperator operator){
        Log.d("RxBleDemo", "flat map single create func:" + System.currentTimeMillis());
        return new RxBleNotifyCharacteristicFunc(macAddress, uuid, mode,operator);
    }
}
