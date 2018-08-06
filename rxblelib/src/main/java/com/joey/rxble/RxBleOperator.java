package com.joey.rxble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

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
 *
 * android.permission.ACCESS_COARSE_LOCATION
 * android.permission.ACCESS_FINE_LOCATION
 * android.permission.BLUETOOTH
 * android.permission.BLUETOOTH_ADMIN
 *
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
            if (RxBle.isEnable()) {
                if (mBtAdapter.enable()) {
                    emitter.onNext(RxBle.client());
                    emitter.onComplete();
                } else {
                    emitter.onError(new BleException("enable bluetooth failed!"));
                }
                return;
            }

            PermissionUtil.requestPermission(mActivity, new PermissionListener() {

                        @Override
                        public void permissionGranted(@NonNull String[] permission) {
                            if (mBtAdapter.enable()) {
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
        return enable().flatMap((Function<RxBleClient, ObservableSource<ScanResult>>) rxBleClient
                -> rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> scanDisposable = disposable)
                .doOnDispose(() -> scanDisposable = null)
                .observeOn(AndroidSchedulers.mainThread()));
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
        return enable().flatMap((Function<RxBleClient, ObservableSource<RxBleConnection>>) rxBleClient
                -> timeOut != null ? rxBleDevice.establishConnection(autoConnect, timeOut)
                : rxBleDevice.establishConnection(autoConnect))
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
        if (dispose(scanDisposable) && scanDisposable != null && !scanDisposable.isDisposed()) {
            scanDisposable.dispose();
            scanDisposable = null;
        }
    }

    /**
     * disconnect
     */
    public void disconnect() {
        if (dispose(connectDisposable) && connectDisposable != null && !scanDisposable.isDisposed()) {
            connectDisposable.dispose();
            connectDisposable = null;
        }

    }

    /**
     * add disposable to release
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
                .doOnError(throwable -> { if (throwable instanceof BleDisconnectedException) ; });

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
                .doOnError(throwable -> { if (throwable instanceof BleDisconnectedException) mConnection = null; });
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
