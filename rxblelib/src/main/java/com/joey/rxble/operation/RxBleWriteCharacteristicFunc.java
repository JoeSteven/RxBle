package com.joey.rxble.operation;

import android.util.Log;

import com.joey.rxble.RxBleOperator;
import com.polidea.rxandroidble2.RxBleConnection;

import java.util.UUID;

import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * Description:
 * author:Joey
 * date:2018/8/8
 */
public class RxBleWriteCharacteristicFunc implements Function<RxBleConnection, ObservableSource<byte[]>>{

    private String macAddress;
    private UUID uuid;
    private RxBleOperator operator;
    private byte[] bytes;

    public RxBleWriteCharacteristicFunc(String macAddress, UUID uuid, byte[] bytes,RxBleOperator operator) {
        this.macAddress = macAddress;
        this.uuid = uuid;
        this.operator = operator;
        this.bytes = bytes;
    }

    @Override
    public ObservableSource<byte[]> apply(RxBleConnection rxBleConnection) throws Exception {
        Log.d("RxBleDemo", "flat map return:" + System.currentTimeMillis());
        return operator.writeCharacteristic(macAddress, uuid, bytes).toObservable();
    }
}
