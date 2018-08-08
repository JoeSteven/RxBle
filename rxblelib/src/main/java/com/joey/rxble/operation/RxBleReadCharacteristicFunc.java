package com.joey.rxble.operation;

import android.util.Log;

import com.joey.rxble.RxBleOperator;
import com.polidea.rxandroidble2.RxBleConnection;

import java.util.UUID;

import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;

/**
 * Description:
 * author:Joey
 * date:2018/8/8
 */
public class RxBleReadCharacteristicFunc implements Function<RxBleConnection, ObservableSource<byte[]>>{

    private String macAddress;
    private UUID uuid;
    private RxBleOperator operator;

    public RxBleReadCharacteristicFunc(String macAddress, UUID uuid, RxBleOperator operator) {
        this.macAddress = macAddress;
        this.uuid = uuid;
        this.operator = operator;
    }

    @Override
    public ObservableSource<byte[]> apply(RxBleConnection rxBleConnection) throws Exception {
        Log.d("RxBleDemo", "flat map return:" + System.currentTimeMillis());
        return operator.readCharacteristic(macAddress, uuid).toObservable();
    }
}
