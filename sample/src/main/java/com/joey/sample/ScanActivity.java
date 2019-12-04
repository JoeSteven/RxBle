package com.joey.sample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.joey.rxble.RxBle;
import com.joey.rxble.RxBleOperator;
import com.joey.sample.connect.ConnectActivity;
import com.joey.rxble.operation.RxBleTransformer;
import com.joey.sample.util.HexString;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.util.UUID;

public class ScanActivity extends AppCompatActivity implements View.OnClickListener {

    private RxBleOperator mOperator;
    private ScanAdapter mAdapter;
    private EditText etDevice;
    private EditText etUUID;
    private TextView tvRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        initView();
        initBle();
    }

    private void initView() {
        findViewById(R.id.open_toggle_btn).setOnClickListener(this);
        findViewById(R.id.scan_toggle_btn).setOnClickListener(this);
        findViewById(R.id.close_toggle_btn).setOnClickListener(this);
        findViewById(R.id.stop_toggle_btn).setOnClickListener(this);
        findViewById(R.id.quit_toggle_btn).setOnClickListener(this);
        findViewById(R.id.bt_read).setOnClickListener(this);
        tvRead = findViewById(R.id.tv_read);
        etDevice = findViewById(R.id.et_device);
        etUUID = findViewById(R.id.et_uuid);
        etDevice.setText(getSharedPreferences("default", MODE_PRIVATE)
                .getString("device", ""));
        etUUID.setText(getSharedPreferences("default", MODE_PRIVATE)
                .getString("uuid", ""));
        RecyclerView rvScan = findViewById(R.id.scan_results);
        mAdapter = new ScanAdapter();
        rvScan.setLayoutManager(new LinearLayoutManager(this));
        rvScan.setAdapter(mAdapter);
        rvScan.setHasFixedSize(true);
        rvScan.setItemAnimator(null);
        mAdapter.setOnAdapterItemClickListener((pos, result) -> connect(result));
    }

    private void initBle() {
        mOperator = RxBle.create();
    }

    public void connect(ScanResult result) {
        Intent intent = new Intent(this, ConnectActivity.class);
        intent.putExtra("device_address", result.getBleDevice().getMacAddress());
        startActivity(intent);
    }

    @SuppressLint("CheckResult")
    public void read() {
        if (TextUtils.isEmpty(etDevice.getText()) || TextUtils.isEmpty(etUUID.getText())) {
            toast("device address and uuid must not be null");
            return;
        }
        UUID uuid = UUID.fromString(etUUID.getText().toString());

        mOperator.scan()
                .filter(result -> TextUtils.equals(result.getBleDevice().getMacAddress(), etDevice.getText().toString()))// 找到指定设备
                .firstOrError() // 不要重复发送数据
                .flatMapObservable(result -> mOperator.connect(result.getBleDevice())) // 连接设备
                .flatMap(RxBleTransformer.readCharacteristic(etDevice.getText().toString(), uuid, mOperator))//读取数据
                .subscribe(bytes -> {
                            tvRead.setText("read success:" + HexString.bytesToHex(bytes));
                            toast("read success:" + HexString.bytesToHex(bytes));
                        },
                        throwable -> tvRead.setText("read failed:" + throwable.toString()));

        getSharedPreferences("default", MODE_PRIVATE)
                .edit()
                .putString("device", etDevice.getText().toString())
                .putString("uuid", etUUID.getText().toString())
                .apply();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.open_toggle_btn:
                open();
                break;
            case R.id.scan_toggle_btn:
                scan();
                break;
            case R.id.stop_toggle_btn:
                stopScan();
                break;
            case R.id.quit_toggle_btn:
                quit();
                break;
            case R.id.close_toggle_btn:
                close();
                break;
            case R.id.bt_read:
                read();
                break;
        }
    }

    private void quit() {
        finish();
    }

    private void open() {
        mOperator.add(mOperator.enable()
                .subscribe(rxBleClient -> toast("enable bluetooth success!"),
                        throwable -> toast("enable failed " + throwable.toString())));
    }

    private void scan() {
        mOperator.add(mOperator.scan()
                .subscribe(this::refresh,
                        throwable -> toast("enable failed " + throwable.toString())));
    }

    private void stopScan() {
        mOperator.stopScan();
        toast("stop scan");
    }

    private void close() {
        mOperator.disable();
    }


    private void refresh(ScanResult scanResult) {
        mAdapter.addScanResult(scanResult);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOperator.disconnect();
        mOperator.stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOperator.release();
    }


    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.d("RxBleDemo", msg);
    }

}
