package com.joey.rxble.connect;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.joey.rxble.R;
import com.joey.rxble.RxBle;
import com.joey.rxble.RxBleOperator;
import com.joey.rxble.operation.RxBleTransformer;
import com.joey.rxble.util.HexString;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDeviceServices;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
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
        mOperator = RxBle.create(this).setConnectRetryTimes(5);
        tvDevice.setText(mDevice);
        connect();
    }

    private void connect() {
        mOperator.add(mOperator.connect(mDevice)
                .flatMap((Function<RxBleConnection, Observable<RxBleDeviceServices>>)
                        rxBleConnection -> rxBleConnection.discoverServices().toObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::refresh, this::error));
    }

    private void error(Throwable throwable) {
        throwable.printStackTrace();
        toast(throwable.toString());
    }


    private void click(BluetoothGattCharacteristic result) {
        List<String> choice = new ArrayList(3);
        if (RxBle.isCharacteristicReadable(result)) {
            choice.add("Read");
        } else if (RxBle.isCharacteristicWritable(result)) {
            choice.add("Write");
        } else if (RxBle.isCharacteristicNotifiable(result)) {
            choice.add("Notify");
            choice.add("Indicate");
        }
        if (choice.isEmpty()) {
            toast("no operation for this characteristic");
            return;
        }
        new AlertDialog.Builder(this)
                .setItems(choice.toArray(new String[choice.size()]),
                        ((dialog, which) -> handleCharacteristics(dialog, which, choice, result)))
                .setCancelable(true)
                .create()
                .show();
    }

    private void handleCharacteristics(DialogInterface dialog, int which, List<String> choice, BluetoothGattCharacteristic result) {
        String operation = choice.get(which);
        if ("Read".equals(operation)) {
            mRead = result.getUuid();
            btRead.setVisibility(View.VISIBLE);
            btRead.setText("Read:" + mRead);
            read(result.getUuid());
        } else if ("Write".equals(operation)) {
            toast("write");
        } else if ("Notify".equals(operation)) {
            notifyC(result.getUuid());

        } else if ("Indicate".equals(operation)) {
            indicate(result.getUuid());
        }
        dialog.dismiss();
    }

    private void indicate(UUID uuid) {

    }

    private void notifyC(UUID uuid) {
        mOperator.add(mOperator.connect(mDevice)
                .flatMap(RxBleTransformer.notifyCharacteristic(mDevice, uuid, null, mOperator))
                .subscribe(bytes -> {
                    toast("notify success:" + HexString.bytesToHex(bytes));
                    tvRead.setText(HexString.bytesToHex(bytes));
                }, this::error));
    }

    private void read(UUID result) {
        mOperator.add(mOperator.connect(mDevice)
                .flatMap(RxBleTransformer.readCharacteristic(mDevice, result, mOperator))
                .subscribe(bytes -> {
                    toast("read success:" + HexString.bytesToHex(bytes));
                    tvRead.setText(HexString.bytesToHex(bytes));
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
