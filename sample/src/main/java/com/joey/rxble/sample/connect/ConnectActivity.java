package com.joey.rxble.sample.connect;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.joey.rxble.R;
import com.joey.rxble.RxBle;
import com.joey.rxble.RxBleOperator;
import com.joey.rxble.operation.RxBleTransformer;
import com.joey.rxble.sample.util.HexString;
import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;

public class ConnectActivity extends AppCompatActivity {

    private boolean isConnect;
    private String mDevice;
    private RxBleOperator mOperator;
    private TextView tvDevice;
    private Button btConnect;
    private TextView tvRead;
    private ConnectAdapter mAdapter;
    private UUID mRead;
    private Button btRead;
    private EditText etWrite;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        initView();
        initBle();
    }

    private void initView() {
        btConnect = findViewById(R.id.connect_toggle_btn);
        btRead = findViewById(R.id.read_toggle_btn);
        tvDevice = findViewById(R.id.tv_mac);
        tvRead = findViewById(R.id.tv_read_content);
        btRead = findViewById(R.id.read_toggle_btn);
        etWrite = findViewById(R.id.etWrite);
        btRead.setVisibility(View.GONE);
        btRead.setOnClickListener(v -> read(mRead));
        RecyclerView rvScan = findViewById(R.id.scan_results);
        mAdapter = new ConnectAdapter();
        rvScan.setLayoutManager(new LinearLayoutManager(this));
        rvScan.setAdapter(mAdapter);
        rvScan.setHasFixedSize(true);
        rvScan.setItemAnimator(null);
        mAdapter.setOnAdapterItemClickListener((pos, result) -> {
            click(result);
        });
        btConnect.setOnClickListener(v -> {
            if (isConnect) {
                disconnect();
                btConnect.setText("Connect");
                isConnect = false;
            } else {
                connect();
            }
        });
    }

    private void disconnect() {
        mOperator.disconnect();
        mAdapter.clearConnectResults();
        tvRead.setText("none");
    }

    private void initBle() {
        mDevice = getIntent().getStringExtra("device_address");
        mOperator = RxBle.create().setConnectRetryTimes(5);
        tvDevice.setText(mDevice);
        connect();
    }

    private void connect() {
        mOperator.add(mOperator.connect(mDevice, false, new Timeout(5, TimeUnit.SECONDS))
                .flatMap((Function<RxBleConnection, Observable<RxBleDeviceServices>>)
                        rxBleConnection -> rxBleConnection.discoverServices().toObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::refresh, this::error));
    }

    private void error(Throwable throwable) {
        throwable.printStackTrace();
        toast(throwable.toString());
    }


    private void click(Object result) {
        if (result instanceof BluetoothGattCharacteristic) {
            List<String> choice = new ArrayList<>(3);
            if (RxBle.isCharacteristicReadable((BluetoothGattCharacteristic) result))
                choice.add("Read");
            if (RxBle.isCharacteristicWritable((BluetoothGattCharacteristic) result))
                choice.add("Write");
            if (RxBle.isCharacteristicNotifiable((BluetoothGattCharacteristic) result))
                choice.add("Notify");
            if (RxBle.isCharacteristicIndicatable((BluetoothGattCharacteristic) result))
                choice.add("Indicate");


            if (choice.isEmpty()) {
                toast("no operation for this characteristic");
                return;
            }

            new AlertDialog.Builder(this)
                    .setItems(choice.toArray(new String[choice.size()]),
                            ((dialog, which) -> handleCharacteristics(dialog, which, choice, (BluetoothGattCharacteristic) result)))
                    .setCancelable(true)
                    .create()
                    .show();
        }

        if (result instanceof BluetoothGattDescriptor) {
            new AlertDialog.Builder(this)
                    .setItems(new String[]{"Read", "Write"},
                            ((dialog, which) -> handleDescriptor(dialog, which, (BluetoothGattDescriptor) result)))
                    .setCancelable(true)
                    .create()
                    .show();
        }
    }

    private void handleDescriptor(DialogInterface dialog, int which, BluetoothGattDescriptor result) {
        switch (which) {
            case 0:
                readDescriptor(result);
                break;
            case 1:
                writeDescriptor(result);
                break;
        }
    }

    private void handleCharacteristics(DialogInterface dialog, int which, List<String> choice, BluetoothGattCharacteristic result) {
        String operation = choice.get(which);
        if ("Read".equals(operation)) {
            mRead = result.getUuid();
            btRead.setVisibility(View.VISIBLE);
            btRead.setText("Read:" + mRead);
            read(result.getUuid());
        } else if ("Write".equals(operation)) {
            write(result.getUuid());
        } else if ("Notify".equals(operation)) {
            notifyC(result.getUuid());
        } else if ("Indicate".equals(operation)) {
            indicate(result.getUuid());
        }
        dialog.dismiss();
    }

    @SuppressLint("CheckResult")
    private void readDescriptor(BluetoothGattDescriptor result) {
        mOperator.readDescriptor(mDevice, result)
                .subscribe(bytes -> {
                    toast("read descriptor message:" + HexString.bytesToHex(bytes));
                    tvRead.setText("read descriptor message:" + HexString.bytesToHex(bytes));
                }, this::error);
    }

    @SuppressLint("CheckResult")
    private void writeDescriptor(BluetoothGattDescriptor result) {
        if (TextUtils.isEmpty(etWrite.getText())) {
            toast("write content can not be null");
            return;
        }
        mOperator.writeDescriptor(mDevice, result, HexString.hexToBytes(etWrite.getText().toString()))
                .subscribe(bytes -> {
                    toast("write descriptor message:" + HexString.bytesToHex(bytes));
                    tvRead.setText("write descriptor message:" + HexString.bytesToHex(bytes));
                }, this::error);
    }

    @SuppressLint("CheckResult")
    private void write(UUID uuid) {
        if (TextUtils.isEmpty(etWrite.getText())) {
            toast("write content can not be null");
            return;
        }
        mOperator.connect(mDevice)
                .flatMap(RxBleTransformer.writeCharacteristic(mDevice, uuid, HexString.hexToBytes(etWrite.getText().toString()), mOperator))
                .subscribe(bytes -> {
                    tvRead.setText("write success:" + HexString.bytesToHex(bytes));
                    toast("write success:" + HexString.bytesToHex(bytes));
                }, this::error);
    }

    private void indicate(UUID uuid) {
        mOperator.add(mOperator.connect(mDevice)
                .flatMap(RxBleTransformer.indicateCharacteristic(mDevice, uuid, NotificationSetupMode.DEFAULT, mOperator))
                .subscribe(bytes -> {
                    toast("indicate message:" + HexString.bytesToHex(bytes));
                    tvRead.setText("indicate message:" + HexString.bytesToHex(bytes));
                }, this::error));
    }

    private void notifyC(UUID uuid) {
        mOperator.add(mOperator.connect(mDevice)
                .flatMap(RxBleTransformer.notifyCharacteristic(mDevice, uuid, null, mOperator))
                .subscribe(bytes -> {
                    toast("notify success:" + HexString.bytesToHex(bytes));
                    tvRead.setText("notify success:" + HexString.bytesToHex(bytes));
                }, this::error));
    }

    private void read(UUID result) {
        mOperator.add(mOperator.connect(mDevice)
                .flatMap(RxBleTransformer.readCharacteristic(mDevice, result, mOperator))
                .subscribe(bytes -> {
                    toast("read success:" + HexString.bytesToHex(bytes));
                    tvRead.setText("read success:" + HexString.bytesToHex(bytes));
                }, this::error));
    }

    private void refresh(RxBleDeviceServices rxBleDeviceServices) {
        isConnect = true;
        btConnect.setText("DisConnect");
        rxBleDeviceServices.getBluetoothGattServices();
        mAdapter.addConnectionData(rxBleDeviceServices.getBluetoothGattServices());
    }

    @Override
    protected void onDestroy() {
        mOperator.release();
        super.onDestroy();
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.d("RxBleDemo", msg);
    }
}
