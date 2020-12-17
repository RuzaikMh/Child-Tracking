package com.example.childtracking;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.skyfishjy.library.RippleBackground;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BLE_main extends AppCompatActivity{

    public static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "BLE Main";

    private HashMap<String, BTLE_Device> mBTDevicesHashMap;
    private ArrayList<BTLE_Device> mBTDevicesArrayList;
    private TextView RssiTextView, DistanceTextView;
    private RippleBackground rippleBackground;
    private BroadcastReceiver_BTState mBTStateUpdateReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private List<ScanFilter> filters = new ArrayList<>();
    private String uid;
    private FirebaseFirestore rootRef;
    private DocumentReference uidRef;
    private String beaconID;
    private String DefaultTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_b_l_e_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        mHandler = new Handler();

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();
        rippleBackground = (RippleBackground)findViewById(R.id.content);

        RssiTextView = findViewById(R.id.textView6);
        DistanceTextView = findViewById(R.id.textView5);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mBTStateUpdateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBTStateUpdateReceiver);
        stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void stop() {
        scanLeDevice(false);
    }

    public void start() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BLE_main.REQUEST_ENABLE_BT);
            stopRippleAnimation();
        } else {
            scanLeDevice(true);
        }
    }

    // Called Back when the started activity returns a result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                //Toast.makeText(this, "bluetooth enabled", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "error: turning on bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void scanLeDevice(final boolean enable) {
        if(mLEScanner == null && mBluetoothAdapter != null){
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        if (enable && !mScanning) {
            Toast.makeText(getApplicationContext(), "Starting BLE scan...", Toast.LENGTH_SHORT).show();
            // Stops scanning after a pre-defined scan period.
            long scanPeriod = 90000;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Stopping BLE scan...", Toast.LENGTH_SHORT).show();
                    mScanning = false;
                    stopScan();
                }
            }, scanPeriod);

            readData(new FirestoreCallback() {
                @Override
                public void onCallback(String id) {
                    UUID beaconUUID = UUID.fromString(id);
                    mScanning = true;
                    ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(beaconUUID)).build();
                    filters.add(filter);
                    ScanSettings settings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                            .build();

                    mLEScanner.startScan(filters,settings,mLeScanCallback);
                }
            });

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
            addDevice(result.getDevice(), new_rssi);
        }
    };

    public void ScanClicked(View view){
        if (!isScanning()) {
            startScan();
            Toast.makeText(this, "Scan Started", Toast.LENGTH_SHORT).show();
        } else {
            stopScan();
            Toast.makeText(this, "Scan Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    public void addDevice(BluetoothDevice device, int rssi) {

        String address = device.getAddress();
        BTLE_Device btleDevice = new BTLE_Device(device);
        if (!mBTDevicesHashMap.containsKey(address)) {
            btleDevice.setRSSI(rssi);
            btleDevice.setDistnace(calculateDistance(rssi));

            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);
        }
        else {
            mBTDevicesHashMap.get(address).setRSSI(rssi);
            mBTDevicesHashMap.get(address).setDistnace(calculateDistance(rssi));
        }

        BTLE_Device devices = mBTDevicesArrayList.get(0);

        int BLErssi = devices.getRSSI();
        double distance = devices.getDistnace();

        RssiTextView.setText(Integer.toString(BLErssi));
        DistanceTextView.setText(Double.toString(round(distance,2)) + "M");
    }

    public void startScan(){
        rippleBackground.startRippleAnimation();
        mBTDevicesArrayList.clear();
        mBTDevicesHashMap.clear();
        start();
    }

    public void stopScan() {
        rippleBackground.stopRippleAnimation();
        stop();
    }

    public void stopRippleAnimation(){
        rippleBackground.stopRippleAnimation();
    }

    public double calculateDistance(int rssi) {

        int txPower = -59; 

        if (rssi == 0) {
            return -1.0;
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double distance =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return distance;
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public void readData(final FirestoreCallback firestoreCallback){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef = FirebaseFirestore.getInstance();
        uidRef = rootRef.collection("users").document(uid);

        uidRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    DefaultTracker = document.getString("Default TrackerID");

                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+DefaultTracker);
                    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            beaconID = snapshot.child("beaconID").getValue(String.class);
                            firestoreCallback.onCallback(beaconID);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.d(TAG, "onCancelled: " + error);
                        }
                    });

                }
            }
        });
    }

    private interface FirestoreCallback{
        void onCallback(String id);
    }

}