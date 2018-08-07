package com.joey.rxble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.joey.rxble.permission.PermissionListener;
import com.joey.rxble.permission.PermissionUtil;
import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.Timeout;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.UUID;

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
    private Activity mActivity;
    private BluetoothAdapter mBtAdapter;
    private RxBleConnection mConnection;
    private Disposable scanDisposable;
    private Disposable connectDisposable;
    private CompositeDisposable compositeDisposable;

    public RxBleOperator(Activity activity) {
        mActivity = activity;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * enable bluetooth
     */
    @SuppressLint("MissingPermission")
    public Observable<RxBleClient> enable() {
        return Observable.create(emitter -> {
            if (RxBle.isEnable()) return;
            if (hasPermission() && mBtAdapter.enable()) {
                Log.d("RxBleDemo", "enable bluetooth");
                emitter.onNext(RxBle.client());
                emitter.onComplete();
                return;
            }
            PermissionUtil.requestPermission(mActivity, new PermissionListener() {

                        @Override
                        public void permissionGranted(@NonNull String[] permission) {
                            if (mBtAdapter.enable()) {
                                Log.d("RxBleDemo", "enable bluetooth");
                                emitter.onNext(RxBle.client());
                                emitter.onComplete();
                            } else {
                                emitter.onError(new BleException("enable bluetooth failed!"));
                            }
                        }

                        @Override
                        public void permissionDenied(@NonNull String[] permission) {
                            emitter.onError(new BleException("permission denied!"));
                        }
                    },
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        });
    }

    public boolean hasPermission() {
        return PermissionUtil.hasPermission(mActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @SuppressLint("MissingPermission")
    public void disable() {
        if (mBtAdapter.isEnabled()) {
            mBtAdapter.disable();
        }
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
                .doOnSubscribe(disposable -> scanDisposable = disposable)
                .doOnDispose(() -> scanDisposable = null)
                .observeOn(AndroidSchedulers.mainThread());
    }


    /**
     * connect to a devices
     */
    public Observable<RxBleConnection> connect(String macAddress) {
        return connect(RxBle.client().getBleDevice(macAddress), false, null);
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
        disconnect();
        Observable<RxBleConnection> connectionObservable;
        if (RxBle.isEnable()) {
            connectionObservable = timeOut != null ? rxBleDevice.establishConnection(autoConnect, timeOut)
                    : rxBleDevice.establishConnection(autoConnect);
        } else {
            connectionObservable = enable()
                    .flatMap(rxBleClient -> RxBle.client().observeStateChanges()
                    .filter(state -> state == RxBleClient.State.READY)
                    .flatMapSingle((Function<RxBleClient.State, SingleSource<RxBleClient>>) state -> Single.just(RxBle.client()))
                    .flatMap((Function<RxBleClient, ObservableSource<RxBleConnection>>)
                            client -> timeOut != null ? rxBleDevice.establishConnection(autoConnect, timeOut)
                            : rxBleDevice.establishConnection(autoConnect)));

        }
        return connectionObservable
                .doOnSubscribe(disposable -> connectDisposable = disposable)
                .doOnDispose(() -> {
                    mConnection = null;// release connection
                    connectDisposable = null;
                }).doOnNext(rxBleConnection -> {
                    mConnection = rxBleConnection;// hold connection
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * read characteristic
     *
     * @param macAddress device mac address
     * @return
     */
    public Single<byte[]> readCharacteristic(String macAddress, final UUID characteristicUUID) {
        return readOrWrite(macAddress, rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID));
    }

    /**
     * read characteristic
     *
     * @param macAddress device mac address
     * @return
     */
    public Single<byte[]> readDescriptor(String macAddress, final BluetoothGattDescriptor descriptor) {
        return readOrWrite(macAddress, rxBleConnection -> rxBleConnection.readDescriptor(descriptor));
    }

    /**
     * write characteristic
     *
     * @param macAddress
     */
    public Single<byte[]> writeCharacteristic(String macAddress, final UUID characteristicUUID, final byte[] bytes) {
        return readOrWrite(macAddress, rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUUID, bytes));
    }

    /**
     * write characteristic,
     *
     * @return this is a pointless value, because write descriptor doesn't have return value
     */
    public Single<byte[]> writeDescriptor(String macAddress, final BluetoothGattDescriptor descriptor, final byte[] bytes) {
        return readOrWrite(macAddress, rxBleConnection
                -> rxBleConnection.writeDescriptor(descriptor, bytes).toSingle(() -> new byte[0]));
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
        if (scanDisposable != null && !scanDisposable.isDisposed()) {
            scanDisposable.dispose();
        }
        scanDisposable = null;
    }

    /**
     * disconnect
     */
    public void disconnect() {
        if (connectDisposable != null && !scanDisposable.isDisposed()) {
            connectDisposable.dispose();
        }
        connectDisposable = null;
    }

    /**
     * add disposable to release
     *
     * @param disposable
     */
    public void collect(Disposable disposable) {
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
        mActivity = null;
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
        mConnection = null;
    }


    private Observable<Observable<byte[]>> setUp(String macAddress, Function<RxBleConnection, Observable<Observable<byte[]>>> function) {
        Observable<Observable<byte[]>> oCacheConnect;
        Observable<Observable<byte[]>> oNewConnect;

        oCacheConnect = Observable.create((ObservableOnSubscribe<RxBleConnection>) emitter -> {
            emitter.onNext(mConnection);
            emitter.onComplete();
        }).flatMap(function)
                .doOnError(throwable -> {
                    if (throwable instanceof BleDisconnectedException) ;
                });

        oNewConnect = connect(macAddress)
                .flatMap(function);
        if (mConnection != null && connectDisposable != null && !connectDisposable.isDisposed()) {
            return Observable.concat(oCacheConnect, oNewConnect)
                    .firstOrError()
                    .toObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        } else {
            return oNewConnect.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    private Single<byte[]> readOrWrite(final String macAddress, final Function<RxBleConnection, SingleSource<byte[]>> function) {
        Observable<byte[]> oCacheConnect;
        Observable<byte[]> oNewConnect;
        oCacheConnect = Observable.create((ObservableOnSubscribe<RxBleConnection>) emitter -> {
            emitter.onNext(mConnection);
            emitter.onComplete();
        }).flatMapSingle(function)
                .doOnError(throwable -> {
                    if (throwable instanceof BleDisconnectedException) mConnection = null;
                });
        oNewConnect = connect(macAddress)
                .flatMapSingle(function);
        if (mConnection != null && connectDisposable != null && !connectDisposable.isDisposed()) {
            return Observable.concat(oCacheConnect, oNewConnect)
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        } else {
            return oNewConnect.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .singleOrError();
        }
    }

}