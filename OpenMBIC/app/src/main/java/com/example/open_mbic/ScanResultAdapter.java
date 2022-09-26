package com.example.open_mbic;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import butterknife.BindView;

/*
This class provides the results of the environment scan as a scrolling list view.
*/

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder> {

        /**
         * Provide a reference to the type of views that you are using
         * (custom ViewHolder).
         */
        public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            @SuppressLint("NonConstantResourceId")
            @BindView(R.id.device_name)
            TextView mDeviceName;
            @SuppressLint("NonConstantResourceId")
            @BindView(R.id.mac_address)
            TextView mMacAddress;
            @SuppressLint("NonConstantResourceId")
            @BindView(R.id.signal_strength)
            TextView mSignalStrength;

            public OnScanListener onScanListener;

            public ViewHolder(View view, OnScanListener onScanListener) {
                super(view);
                // Define click listener for the ViewHolder's View
                mDeviceName = view.findViewById(R.id.device_name);
                mMacAddress = view.findViewById(R.id.mac_address);
                mSignalStrength = view.findViewById(R.id.signal_strength);

                this.onScanListener = onScanListener;
                view.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                onScanListener.onScanClick(getAdapterPosition());
            }
        }

        /**
         * Initialize the dataset of the Adapter.
         *
         * @param dataSet String[] containing the data to populate views to be used
         * by RecyclerView.
         */

        ArrayList<ScanResult> scanResults;

        public OnScanListener onScanListener;

        public ScanResultAdapter(ArrayList<ScanResult> mScanResults, OnScanListener mOnNoteListener) {

            scanResults = mScanResults;
            onScanListener = mOnNoteListener;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            // Create a new view, which defines the UI of the list item
            Context context = viewGroup.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View contactView = inflater.inflate(R.layout.row_scan_result, viewGroup, false);
            ViewHolder viewHolder = new ViewHolder(contactView, onScanListener);
            return viewHolder;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {

            // Get element from your dataset at this position and replace the
            // contents of the view with that element

            // Get the data model based on position
            ScanResult scanResult = scanResults.get(position);
            BluetoothDevice device = scanResult.getDevice();

            // Set item views based on your views and data model
            TextView textView = viewHolder.mDeviceName;
            textView.setText(device.getName());
            TextView textView2 = viewHolder.mMacAddress;
            textView2.setText(device.getAddress());
            TextView textView3 = viewHolder.mSignalStrength;
            textView3.setText(scanResult.getRssi()+"");
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return scanResults.size();
        }

        public interface OnScanListener{
            void onScanClick(int position);
        }
    }
