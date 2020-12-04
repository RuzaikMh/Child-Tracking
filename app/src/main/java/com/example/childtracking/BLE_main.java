package com.example.childtracking;
import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.skyfishjy.library.RippleBackground;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;

public class BLE_main extends AppCompatActivity{

    public static final int REQUEST_ENABLE_BT = 1;

    private HashMap<String, BTLE_Device> mBTDevicesHashMap;
    private ArrayList<BTLE_Device> mBTDevicesArrayList;
    private TextView RssiTextView, DistanceTextView;
    private RippleBackground rippleBackground;
    private BroadcastReceiver_BTState mBTStateUpdateReceiver;
    private Scanner_BTLE scanner_btle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_b_l_e_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        scanner_btle = new Scanner_BTLE(this, 90000);

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
        stopScan();
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

    public void ScanClicked(View view){
        if (!scanner_btle.isScanning()) {
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
        scanner_btle.start();
    }

    public void stopScan() {
        rippleBackground.stopRippleAnimation();
        scanner_btle.stop();
    }

    public double calculateDistance(int rssi) {

        int txPower = -59; //hard coded power value. Usually ranges between -59 to -65

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

}