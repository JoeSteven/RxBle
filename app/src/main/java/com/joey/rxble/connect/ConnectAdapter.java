package com.joey.rxble.connect;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.joey.rxble.R;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Description:
 * author:Joey
 * date:2018/8/6
 */
public class ConnectAdapter extends RecyclerView.Adapter<ConnectAdapter.ConnectHolder> {

    public static class ConnectHolder extends RecyclerView.ViewHolder{
        public ConnectHolder(View itemView) {
            super(itemView);
        }
    }

    public static class ConnectServiceHolder extends ConnectHolder {

        TextView line1;

        ConnectServiceHolder(View itemView) {
            super(itemView);
            line1 = itemView.findViewById(R.id.scan_line1);
        }
    }

    public static class ConnectCharacteristicHolder extends ConnectHolder {

        TextView line1;
        TextView line2;

        ConnectCharacteristicHolder(View itemView) {
            super(itemView);
            line1 = itemView.findViewById(R.id.scan_line1);
            line2 = itemView.findViewById(R.id.scan_line2);
        }
    }

    interface OnAdapterItemClickListener {
        void onAdapterViewClick(int pos, BluetoothGattCharacteristic result);
    }

    private final List<Object> data = new ArrayList<>();
    private OnAdapterItemClickListener onAdapterItemClickListener;

    void addConnectionData(List<BluetoothGattService> result) {
        // Not the best way to ensure distinct devices, just for sake on the demo.
        for (BluetoothGattService service : result) {
            data.add(service);
            data.addAll(service.getCharacteristics());
        }
        notifyDataSetChanged();
    }

    void clearConnectResults() {
        data.clear();
        notifyDataSetChanged();
    }


    void setOnAdapterItemClickListener(OnAdapterItemClickListener onAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener;
    }

    @Override
    public int getItemCount() {
        return data!=null ? data.size():0;
    }

    @Override
    public int getItemViewType(int position) {
        Object obj = data.get(position);
        if (obj instanceof  BluetoothGattService) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public ConnectHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType){
            case 0:
                final View sView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_connect_service, parent, false);
                return new ConnectServiceHolder(sView);
            default:
                final View cView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan, parent, false);
                return new ConnectCharacteristicHolder(cView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ConnectHolder holder, int position) {
        if (holder instanceof ConnectServiceHolder) {
            BluetoothGattService bluetoothGattService = (BluetoothGattService) data.get(position);
            ((ConnectServiceHolder) holder).line1.setText(String.format(Locale.getDefault(), "Service: %s", bluetoothGattService.getUuid()));
            return;
        }
        if (holder instanceof ConnectCharacteristicHolder) {
            BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) data.get(position);
            ((ConnectCharacteristicHolder) holder).line1.setText(String.format(Locale.getDefault(), "Characteristic: %s", characteristic.getUuid()));
            ((ConnectCharacteristicHolder) holder).line2.setText(String.format(Locale.getDefault(), "property: %s", describeProperties(characteristic)));
            ((ConnectCharacteristicHolder) holder).itemView.setOnClickListener(v -> {onAdapterItemClickListener.onAdapterViewClick(position, characteristic);});
        }
    }

    private String describeProperties(BluetoothGattCharacteristic characteristic) {
        List<String> properties = new ArrayList<>();
        if (isCharacteristicReadable(characteristic)) properties.add("Read");
        if (isCharacteristicWriteable(characteristic)) properties.add("Write");
        if (isCharacteristicNotifiable(characteristic)) properties.add("Notify");
        return TextUtils.join(" ", properties);
    }

    private boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    private boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    private boolean isCharacteristicWriteable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE
                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }
}
