package com.example.childtracking;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BroadcastReceiver_BTState extends BroadcastReceiver {

    Context activityContext;

    public BroadcastReceiver_BTState(Context activityContext) {
        this.activityContext = activityContext;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Toast.makeText(activityContext, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Toast.makeText(activityContext, "Bluetooth is turning off...", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_ON:
                    Toast.makeText(activityContext, "Bluetooth is on", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Toast.makeText(activityContext, "Bluetooth is turning on...", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
