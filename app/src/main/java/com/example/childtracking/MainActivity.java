package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    FirebaseFirestore rootRef;
    DocumentReference uidRef;
    String uid,DefaultTracker,DefaultTrackerChanged;
    GridLayout mainGrid;
    TextView welcome;
    ImageView profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainGrid = (GridLayout) findViewById(R.id.mainGrid);
        setSingleEvent(mainGrid);
        welcome = findViewById(R.id.welcomeMsg);
        profile = findViewById(R.id.imageView4);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef = FirebaseFirestore.getInstance();
        uidRef = rootRef.collection("users").document(uid);
        uidRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    String data = (String) document.get("Default TrackerID");
                    String fname = (String) document.get("fname");
                    welcome.setText("Welcome " + fname);
                    if(data != null){
                        DefaultTracker = data;

                        Intent serviceIntent = new Intent(getApplicationContext(), FirebaseService.class);
                        serviceIntent.putExtra("inputExtra", DefaultTracker);
                        ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
                    }
                }
            }
        });

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(),Login.class));
                finish();
            }
        });

    }

    private void setSingleEvent(GridLayout mainGrid) {
        for (int i = 0; i < mainGrid.getChildCount(); i++) {
            CardView cardView = (CardView) mainGrid.getChildAt(i);
            final int finalI = i;
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(finalI == 0)
                    {
                        Intent intent = new Intent(getApplicationContext(),LiveLocation.class);
                        intent.putExtra("defaultTracker", DefaultTracker);
                        startActivity(intent);
                    }
                    else if(finalI == 1)
                    {
                        startActivity(new Intent(getApplicationContext(),GetGeofence.class));
                    }
                    else if(finalI == 2)
                    {
                        startActivity(new Intent(getApplicationContext(),BLE_main.class));
                    }
                    else if(finalI == 3)
                    {
                        startActivity(new Intent(getApplicationContext(),AddTracker.class));
                    }
                    else if(finalI == 4)
                    {
                        startActivity(new Intent(getApplicationContext(),trackingHistroy.class));
                    }
                    else if(finalI == 5)
                    {
                        Intent serviceIntent = new Intent(getApplicationContext(), FirebaseService.class);
                        stopService(serviceIntent);

                    }
                    else if(finalI == 6)
                    {
                        startActivity(new Intent(getApplicationContext(),SettingsActivity.class));
                    }

                }
            });
        }
    }

    public void onResume() {

        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String syncInterval = sharedPreferences.getString("sync_interval","5000");
        double interval =  Double.parseDouble(syncInterval);
        boolean syncLocation = sharedPreferences.getBoolean("perform_sync",true);

        FirebaseDatabase.getInstance()
                .getReference("Alert")
                .child("Interval")
                .setValue(interval);
        if(syncLocation == true) {
            FirebaseDatabase.getInstance()
                    .getReference("Alert")
                    .child("Sync")
                    .setValue(1);
        }else{
            FirebaseDatabase.getInstance()
                    .getReference("Alert")
                    .child("Sync")
                    .setValue(0);
        }

    }
}