package com.joey.rxble.operation;

import android.util.Log;

import com.joey.rxble.RxBleOperator;
import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.RxBleConnection;

import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * Description:
 * author:Joey
 * date:2018/8/8
 */
public class RxBleNotifyCharacteristicFunc implements Function<RxBleConnection, Observable<byte[]>>{

    private String macAddress;
    private UUID uuid;
    private RxBleOperator operator;
    private NotificationSetupMode mode;

    public RxBleNotifyCharacteristicFunc(String macAddress, UUID uuid, NotificationSetupMode mode, RxBleOperator operator) {
        this.macAddress = macAddress;
        this.uuid = uuid;
        this.operator = operator;
        this.mode = mode;
    }

    @Override
    public Observable<byte[]> apply(RxBleConnection rxBleConnection) throws Exception {
        Log.d("RxBleDemo", "flat map return:" + System.currentTimeMillis());
        return operator.setupNotification(macAddress, uuid, mode)
                .doOnNext(observable -> Log.d("RxBle", "register notification success"))
                .flatMap(observable -> observable);
    }
}
