package com.joey.rxble;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;

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
public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ScanHolder> {
    public static class ScanHolder extends RecyclerView.ViewHolder {

        TextView line1;
        TextView line2;
        TextView line3;

        ScanHolder(View itemView) {
            super(itemView);
            line1 = itemView.findViewById(R.id.scan_line1);
            line2 = itemView.findViewById(R.id.scan_line2);
            line3 = itemView.findViewById(R.id.scan_line3);
        }
    }

    interface OnAdapterItemClickListener {

        void onAdapterViewClick(int pos, ScanResult result);
    }

    private static final Comparator<ScanResult> SORTING_COMPARATOR = (lhs, rhs) ->
            lhs.getBleDevice().getMacAddress().compareTo(rhs.getBleDevice().getMacAddress());
    private final List<ScanResult> data = new ArrayList<>();
    private OnAdapterItemClickListener onAdapterItemClickListener;

    void addScanResult(ScanResult bleScanResult) {
        // Not the best way to ensure distinct devices, just for sake on the demo.

        for (int i = 0; i < data.size(); i++) {

            if (data.get(i).getBleDevice().equals(bleScanResult.getBleDevice())) {
                data.set(i, bleScanResult);
                notifyItemChanged(i);
                return;
            }
        }

        data.add(bleScanResult);
        Collections.sort(data, SORTING_COMPARATOR);
        notifyDataSetChanged();
    }

    void clearScanResults() {
        data.clear();
        notifyDataSetChanged();
    }

    ScanResult getItemAtPosition(int childAdapterPosition) {
        return data.get(childAdapterPosition);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    @Override
    public ScanHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan, parent, false);
        return new ScanHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanHolder holder, int position) {
        final ScanResult rxBleScanResult = data.get(position);
        final RxBleDevice bleDevice = rxBleScanResult.getBleDevice();
        holder.itemView.setOnClickListener(v -> {
            if (onAdapterItemClickListener != null) {
                onAdapterItemClickListener.onAdapterViewClick(position, rxBleScanResult);
            }
        });
        holder.line1.setText(String.format(Locale.getDefault(), "%s (%s)", bleDevice.getMacAddress(), bleDevice.getName()));
        holder.line2.setText(String.format(Locale.getDefault(), "RSSI: %d", rxBleScanResult.getRssi()));
        holder.line3.setText(String.format(Locale.getDefault(), "Type: %d", rxBleScanResult.getBleDevice().getBluetoothDevice().getType()));
    }

    void setOnAdapterItemClickListener(OnAdapterItemClickListener onAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener;
    }
}
