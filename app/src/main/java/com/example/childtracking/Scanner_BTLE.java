package com.example.childtracking;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Scanner_BTLE {

    private BLE_main ble_main;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private long scanPeriod;
    private List<ScanFilter> filters = new ArrayList<>();
    private static final String TAG = "Scanner";

    public Scanner_BTLE(BLE_main mainActivity, long scanPeriod) {
        ble_main = mainActivity;

        mHandler = new Handler();

        this.scanPeriod = scanPeriod;

        final BluetoothManager bluetoothManager =
                (BluetoothManager) ble_main.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void start() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ble_main.startActivityForResult(enableBtIntent, BLE_main.REQUEST_ENABLE_BT);
            ble_main.stopScan();
        } else {
            scanLeDevice(true);
        }
    }

    public void stop() {
        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable && !mScanning) {
            Toast.makeText(ble_main.getApplicationContext(), "Starting BLE scan...", Toast.LENGTH_SHORT).show();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ble_main.getApplicationContext(), "Stopping BLE scan...", Toast.LENGTH_SHORT).show();
                    mScanning = false;
                    ble_main.stopScan();
                }
            }, scanPeriod);

            mScanning = true;
            ScanFilter filter = new ScanFilter.Builder().setDeviceName("Child GPS Tracker").build();
            filters.add(filter);
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();
            //UUID[] uuids = new UUID[1];
            //uuids[0] = UUID.fromString("87b99b2c-90fd-11e9-bc42-526af7764f64");
            //mBluetoothAdapter.startLeScan(mLeScanCallback);
            mLEScanner.startScan(filters,settings,mLeScanCallback);
        } else {
            mScanning = false;
            mLEScanner.stopScan(mLeScanCallback);
        }
    }

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);

            final int new_rssi = result.getRssi();
            ble_main.addDevice(result.getDevice(), new_rssi);

        }
    };
}