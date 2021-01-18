package com.example.childtracking;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    FirebaseFirestore rootRef;
    DocumentReference uidRef;
    String uid;
    String DefaultTracker = null;
    GridLayout mainGrid;
    TextView welcome;
    ImageView profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainGrid = findViewById(R.id.mainGrid);
        setSingleEvent(mainGrid);
        welcome = findViewById(R.id.welcomeMsg);
        profile = findViewById(R.id.imageView4);

        Intent stopService = new Intent(getApplicationContext(), FirebaseService.class);
        stopService(stopService);

        loadDataOnCreate();

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(),Login.class));
                finish();
            }
        });

    }

    private void loadDataOnCreate(){
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
    }

    private void loadDataOnResume(){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef = FirebaseFirestore.getInstance();
        uidRef = rootRef.collection("users").document(uid);
        uidRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    String data = (String) document.get("Default TrackerID");
                    if(data != null){
                        DefaultTracker = data;
                    }
                }
            }
        });
    }

    private void setSingleEvent(GridLayout mainGrid) {
        for (int i = 0; i < mainGrid.getChildCount(); i++) {
            CardView cardView = (CardView) mainGrid.getChildAt(i);
            final int id = i;
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(id == 0)
                    {
                        if(DefaultTracker != null) {
                            Intent intent = new Intent(getApplicationContext(), LiveLocation.class);
                            intent.putExtra("defaultTracker", DefaultTracker);
                            startActivity(intent);
                        }else {
                            Toast.makeText(MainActivity.this, "Please wait until data loads", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else if(id == 1)
                    {
                        startActivity(new Intent(getApplicationContext(),GetGeofence.class));
                    }
                    else if(id == 2)
                    {
                        startActivity(new Intent(getApplicationContext(),BLE_main.class));
                    }
                    else if(id == 3)
                    {
                        startActivity(new Intent(getApplicationContext(),AddTracker.class));
                    }
                    else if(id == 4)
                    {
                        startActivity(new Intent(getApplicationContext(),trackingHistroy.class));
                    }
                    else if(id == 5)
                    {
                        Intent serviceIntent = new Intent(getApplicationContext(), FirebaseService.class);
                        stopService(serviceIntent);

                    }
                    else if(id == 6)
                    {
                        startActivity(new Intent(getApplicationContext(),SettingsActivity.class));
                    }
                    else if(id == 7){
                        startActivity(new Intent(getApplicationContext(),test.class));
                    }

                }
            });
        }
    }

    public void onResume() {

        super.onResume();
        loadDataOnResume();
    }
}