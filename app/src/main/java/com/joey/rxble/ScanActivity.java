package com.joey.rxble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.polidea.rxandroidble2.scan.ScanResult;

public class ScanActivity extends AppCompatActivity implements View.OnClickListener {

    private RxBleOperator mOperator;
    private ScanAdapter mAdapter;

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
        findViewById(R.id.disconect_toggle_btn).setOnClickListener(this);
        findViewById(R.id.stop_toggle_btn).setOnClickListener(this);
        findViewById(R.id.quit_toggle_btn).setOnClickListener(this);
        RecyclerView rvScan = findViewById(R.id.scan_results);
        mAdapter = new ScanAdapter();
        rvScan.setLayoutManager(new LinearLayoutManager(this));
        rvScan.setAdapter(mAdapter);
        rvScan.setHasFixedSize(true);
        rvScan.setItemAnimator(null);
        mAdapter.setOnAdapterItemClickListener((pos, result) -> connect(result));
    }

    private void initBle() {
        RxBle.init(getApplicationContext());
        RxBle.markState();
        mOperator = RxBle.create(this);
    }

    public void connect(ScanResult result) {

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.open_toggle_btn:
                open();
                break;
            case R.id.scan_toggle_btn:
                scan();
                break;
            case R.id.disconect_toggle_btn:
                disconnect();
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
        }
    }

    private void quit() {
        finish();
    }

    private void open() {
        mOperator.collect(mOperator.enable()
                .subscribe(rxBleClient -> toast("enable bluetooth success!"),
                        throwable -> toast("enable failed" + throwable.toString())));
    }

    private void scan() {
        mOperator.collect(mOperator.scan()
                .subscribe(this::refresh,
                        throwable -> toast("enable failed" + throwable.toString())));
    }

    private void stopScan() {
        mOperator.stopScan();
    }

    private void disconnect() {
        mOperator.disconnect();
    }

    private void close() {
        mOperator.disable();
    }


    private void refresh(ScanResult scanResult) {
        mAdapter.addScanResult(scanResult);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOperator.release();
        RxBle.restoreState();
    }


    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
