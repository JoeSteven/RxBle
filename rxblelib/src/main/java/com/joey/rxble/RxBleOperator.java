package com.joey.rxble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import com.joey.rxble.exception.BleEnableException;
import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.Timeout;
import com.polidea.rxandroidble2.exceptions.BleAlreadyConnectedException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Description: easier way to use RxAndroidBle, require per
 * <p>
 * android.permission.ACCESS_COARSE_LOCATION
 * android.permission.ACCESS_FINE_LOCATION
 * android.permission.BLUETOOTH
 * android.permission.BLUETOOTH_ADMIN
 * <p>
 * author:Joey
 * date:2018/8/6
 */
public class RxBleOperator {
    private BluetoothAdapter mBtAdapter;
    private RxBleConnection mConnection;
    private CompositeDisposable compositeDisposable;
    private long retryTimes = 3;
    private long retryInterval = 1000;// unit ms
    private PublishSubject<Boolean> disconnectTrigger = PublishSubject.create();
    private PublishSubject<Boolean> stopScanTrigger = PublishSubject.create();


    public RxBleOperator() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        add(RxBle.registerState().subscribe(state -> {
            switch (state) {
                case BLUETOOTH_NOT_AVAILABLE:
                case LOCATION_PERMISSION_NOT_GRANTED:
                case BLUETOOTH_NOT_ENABLED:
                    RxBleLog.d("state has changed:%s", state);
                    disconnect();
            }
        }));
    }

    public RxBleOperator setConnectRetryTimes(long retryTimes) {
        this.retryTimes = retryTimes;
        return this;
    }

    public RxBleOperator setRetryInterval(long interval) {
        this.retryInterval = interval;
        return this;
    }

    /**
     * enable bluetooth
     */
    @SuppressLint("MissingPermission")
    public Observable<RxBleClient> enable() {
        return Observable.create((ObservableOnSubscribe<RxBleClient>) emitter -> {
            if (!RxBle.hasPermission()) {
                boolean supportRequest = RxBle.requestPermission(new RxBle.PermissionListener() {
                    @Override
                    public void onGranted() {
                        if (mBtAdapter.enable()) {
                            emitter.onNext(RxBle.client());
                            emitter.onComplete();
                        } else {
                            emitter.onError(new BleEnableException(BleEnableException.ENABLE_FAILED, "enable bluetooth failed!"));
                        }
                    }

                    @Override
                    public void onDenied() {
                        emitter.onError(new BleEnableException(BleEnableException.PERMISSION_DENIED, "permission denied!"));
                    }
                });
                if (supportRequest) {
                    RxBleLog.d("request permissions for bluetooth");
                    return;
                }
            }
            if (RxBle.isEnable() || mBtAdapter.enable()) {
                RxBleLog.d( "has permission, try to enable bluetooth");
                emitter.onNext(RxBle.client());
                emitter.onComplete();
            } else {
                emitter.onError(new BleEnableException(BleEnableException.ENABLE_FAILED, "enable bluetooth failed!"));
            }
        })
                .doOnNext(client -> RxBleLog.d("enable bluetooth success"))
                .doOnError(throwable -> RxBleLog.e("enable bluetooth failed:%s", throwable.toString()));
    }

    @SuppressLint("MissingPermission")
    public void disable() {
        if (mBtAdapter.isEnabled()) {
            mBtAdapter.disable();
        }
        RxBleLog.d("disable bluetooth");
    }

    /**
     * scan ble devices
     */
    public Observable<ScanResult> scan(ScanFilter... scanFilters) {
        return scan(new ScanSettings.Builder().build(), scanFilters);
    }

    /**
     * scan ble devices
     *
     * @param scanSettings settings
     * @param scanFilter   scan filter
     */
    public Observable<ScanResult> scan(@NonNull final ScanSettings scanSettings, final ScanFilter... scanFilter) {
        stopScan();
        Observable<ScanResult> scanResultObservable;
        if (RxBle.isEnable()) {
            scanResultObservable = RxBle.client().scanBleDevices(scanSettings, scanFilter);
        } else {
            scanResultObservable = enable()
                    .flatMap(rxBleClient -> RxBle.client().observeStateChanges()
                            .filter(state -> state == RxBleClient.State.READY)
                            .flatMapSingle((Function<RxBleClient.State, SingleSource<RxBleClient>>) state -> Single.just(RxBle.client()))
                            .flatMap((Function<RxBleClient, ObservableSource<ScanResult>>) client -> client.scanBleDevices(scanSettings, scanFilter)));
        }
        return scanResultObservable.subscribeOn(Schedulers.io())
                .takeUntil(stopScanTrigger)
                .doOnNext(result -> RxBleLog.d( "Scan device:%s", result.getBleDevice()))
                .doOnError(result -> RxBleLog.e( "Scan device failed:%s", result.toString()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    /**
     * connect to a devices
     */
    public Observable<RxBleConnection> connect(String macAddress) {
        return connect(RxBle.client().getBleDevice(macAddress));
    }

    /**
     * connect to a devices
     */
    public Observable<RxBleConnection> connect(RxBleDevice device) {
        return connect(device, false, null);
    }

    /**
     * connect to a devices
     */
    public Observable<RxBleConnection> connect(String macAddress, boolean autoConnect, Timeout timeOut) {
        return connect(RxBle.client().getBleDevice(macAddress), autoConnect, timeOut);
    }

    /**
     * connect to ble device
     *
     * @param rxBleDevice device
     */
    public Observable<RxBleConnection> connect(final RxBleDevice rxBleDevice, final boolean autoConnect, final Timeout timeOut) {
        return Observable.just("connect")
                .flatMap((Function<String, ObservableSource<RxBleConnection>>) s -> wrapConnect(rxBleDevice, autoConnect, timeOut))
                .retry(retryTimes, throwable -> {
                    boolean retry = (throwable instanceof BleDisconnectedException
                            || (throwable instanceof BleAlreadyConnectedException && mConnection == null)) && RxBle.isEnable();
                    if (retry) {
                        RxBleLog.e("reconnect caused by:%s", throwable.toString());
                        Thread.sleep(retryInterval);
                    }
                    return retry;
                })
                .onErrorResumeNext(throwable -> {
                    if (throwable instanceof BleAlreadyConnectedException && mConnection != null) {
                        RxBleLog.e("reuse connection :%s", mConnection.toString());
                        return Observable.just(mConnection);
                    }
                    return Observable.error(throwable);
                })
                .doOnNext(rxBleConnection -> {
                    RxBleLog.d( "connect success:%s", rxBleDevice.getMacAddress());
                    mConnection = rxBleConnection;// hold connection
                })
                .doOnError(throwable -> RxBleLog.e("connect to %s failed, caused by:%s", rxBleDevice.getMacAddress(), throwable.toString()))
                .takeUntil(disconnectTrigger)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<RxBleConnection> wrapConnect(final RxBleDevice rxBleDevice, final boolean autoConnect, final Timeout timeOut) {
        RxBleLog.d( "connect to %s", rxBleDevice.getMacAddress());
        Observable<RxBleConnection> connectionObservable;
        if (mConnection != null) {
            connectionObservable = Observable.just(mConnection);
        } else {
            connectionObservable = timeOut != null ? rxBleDevice.establishConnection(autoConnect, timeOut)
                    : rxBleDevice.establishConnection(autoConnect);
        }
        Observable<RxBleConnection> finalObservable;
        if (RxBle.isEnable()) {
            finalObservable = connectionObservable;
        } else {
            finalObservable = enable()
                    .flatMap(rxBleClient -> RxBle.client().observeStateChanges()
                            .filter(state -> {
                                RxBleLog.d( "bluetooth state when connect:" + state);
                                return state == RxBleClient.State.READY;
                            })
                            .flatMapSingle((Function<RxBleClient.State, SingleSource<RxBleClient>>) state -> Single.just(RxBle.client()))
                            .flatMap((Function<RxBleClient, ObservableSource<RxBleConnection>>) client -> connectionObservable));
        }
        return finalObservable;
    }

    /**
     * read characteristic
     *
     * @param macAddress device mac address
     * @return
     */
    public Single<byte[]> readCharacteristic(String macAddress, final UUID characteristicUUID) {

        return readOrWrite(macAddress, rxBleConnection -> {
            RxBleLog.d("attempt to read Characteristic:%s", characteristicUUID);
            return rxBleConnection.readCharacteristic(characteristicUUID);
        });
    }

    /**
     * read characteristic
     *
     * @param macAddress device mac address
     * @return
     */
    public Single<byte[]> readDescriptor(String macAddress, final BluetoothGattDescriptor descriptor) {
        return readOrWrite(macAddress, rxBleConnection -> {
            RxBleLog.d( "attempt to read Descriptor:%s", descriptor.getUuid());
            return rxBleConnection.readDescriptor(descriptor);
        });
    }

    /**
     * write characteristic
     *
     * @param macAddress
     */
    public Single<byte[]> writeCharacteristic(String macAddress, final UUID characteristicUUID, final byte[] bytes) {
        return readOrWrite(macAddress, rxBleConnection -> {
            RxBleLog.d( "attempt to write Characteristic:%s", characteristicUUID);
            return rxBleConnection.writeCharacteristic(characteristicUUID, bytes);
        });
    }

    /**
     * write characteristic,
     *
     * @return this is a pointless value, because write descriptor doesn't have return value
     */
    public Single<byte[]> writeDescriptor(String macAddress, final BluetoothGattDescriptor descriptor, final byte[] bytes) {
        return readOrWrite(macAddress, rxBleConnection -> {
            RxBleLog.d( "attempt to write Descriptor:%s", descriptor.getUuid());
            return rxBleConnection.writeDescriptor(descriptor, bytes).toSingle(() -> new byte[0]);
        });
    }

    /**
     * set up indicate
     */
    public Observable<Observable<byte[]>> setUpIndication(String macAddress, final UUID characteristicUUID, final NotificationSetupMode mode) {
        return setUp(macAddress, rxBleConnection -> rxBleConnection.setupIndication(characteristicUUID, mode));
    }

    /**
     * set up notify
     */
    public Observable<Observable<byte[]>> setupNotification(String macAddress, final UUID characteristicUUID, final NotificationSetupMode mode) {
        return setUp(macAddress, rxBleConnection -> rxBleConnection.setupNotification(characteristicUUID, mode));
    }

    /**
     * stop scan
     */
    public void stopScan() {
        RxBleLog.d("stop scan");
        stopScanTrigger.onNext(true);
    }

    /**
     * disconnect
     */
    public void disconnect() {
        RxBleLog.d("disconnect device");
        disconnectTrigger.onNext(true);
        mConnection = null;
    }

    /**
     * add disposable to release
     *
     * @param disposable
     */
    public void add(Disposable disposable) {
        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(disposable);
    }

    public boolean dispose(Disposable disposable) {
        return compositeDisposable != null && disposable != null && compositeDisposable.remove(disposable);
    }

    /**
     * release this operator after use
     */
    public void release() {
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
        disconnect();
        stopScan();
        RxBleLog.d("release");
    }


    private Observable<Observable<byte[]>> setUp(String macAddress, Function<RxBleConnection, Observable<Observable<byte[]>>> function) {

        return Observable.create((ObservableOnSubscribe<RxBleConnection>) emitter -> {
            if (mConnection == null) {
                emitter.onError(new BleDisconnectedException(macAddress));
            } else {
                emitter.onNext(mConnection);
                emitter.onComplete();
            }
        }).flatMap(function)
                .doOnNext(observable -> RxBleLog.d("set up Indicate or Notification success"))
                .doOnError(throwable -> {
                    RxBleLog.e("set up Indicate or Notification failed %s", throwable.toString());
                    if (throwable instanceof BleDisconnectedException) mConnection = null;
                })
                .takeUntil(disconnectTrigger)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Single<byte[]> readOrWrite(final String macAddress, final Function<RxBleConnection, SingleSource<byte[]>> function) {
        AtomicReference<Disposable> readDisposable = new AtomicReference<>();
        return Observable.create((ObservableOnSubscribe<RxBleConnection>) emitter -> {
            if (mConnection == null) {
                emitter.onError(new BleDisconnectedException(macAddress));
            } else {
                emitter.onNext(mConnection);
                emitter.onComplete();
            }
        }).flatMapSingle(function)
                .doOnNext(bytes -> RxBleLog.d("read or write success %s"))
                .doOnError(throwable -> {
                    RxBleLog.e("read or write failed %s", throwable.toString());
                    if (throwable instanceof BleDisconnectedException) mConnection = null;
                })
                .singleOrError()
                .doOnSubscribe(readDisposable::set)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterSuccess(bytes -> readDisposable.get().dispose());
    }

}
