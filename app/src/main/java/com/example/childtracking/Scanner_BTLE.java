package com.example.childtracking;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Scanner_BTLE {

    private BLE_main ma;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private long scanPeriod;
    private int signalStrength;
    private List<ScanFilter> filters = new ArrayList<>();
    private static final String TAG = "Scanner";

    public Scanner_BTLE(BLE_main mainActivity, long scanPeriod, int signalStrength) {
        ma = mainActivity;

        mHandler = new Handler();

        this.scanPeriod = scanPeriod;
        this.signalStrength = signalStrength;

        final BluetoothManager bluetoothManager =
                (BluetoothManager) ma.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void start() {
        if (!Utils.checkBluetooth(mBluetoothAdapter)) {
            Utils.requestUserBluetooth(ma);
            ma.stopScan();
        } else {
            scanLeDevice(true);
        }
    }

    public void stop() {
        scanLeDevice(false);
    }

    // If you want to scan for only specific types of peripherals,
    // you can instead call startLeScan(UUID[], BluetoothAdapter.LeScanCallback),
    // providing an array of UUID objects that specify the GATT services your app supports.
    private void scanLeDevice(final boolean enable) {
        if (enable && !mScanning) {
            Utils.toast(ma.getApplicationContext(), "Starting BLE scan...");

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Utils.toast(ma.getApplicationContext(), "Stopping BLE scan...");

                    mScanning = false;

                    mLEScanner.stopScan(mLeScanCallback);

                    ma.stopScan();
                }
            }, scanPeriod);

            mScanning = true;
            ScanFilter filter = new ScanFilter.Builder().setDeviceName("Child GPS Tracker").build();
            filters.add(filter);
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
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



    // Device scan callback.
  /*  private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    final int new_rssi = rssi;
                    if (rssi > signalStrength) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ma.addDevice(device, new_rssi);
                            }
                        });
                    }
                }
            };
*/
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);

            final int new_rssi = result.getRssi();
            if (result.getRssi() > signalStrength) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ma.addDevice(result.getDevice(), new_rssi);
                        Log.d(TAG, "run: uuids" + result.getDevice().getName());
                    }
                });
            }
        }
    };
}