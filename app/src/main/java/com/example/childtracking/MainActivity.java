package com.example.childtracking;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    FirebaseFirestore firebaseFirestore;
    DocumentReference documentReference;
    String uid;
    String DefaultTracker = null;
    GridLayout mainGrid;
    TextView welcome;
    ImageView Logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainGrid = findViewById(R.id.mainGrid);
        onGridClick(mainGrid);
        welcome = findViewById(R.id.welcomeMsg);
        Logout = findViewById(R.id.imageView4);

        Intent stopService = new Intent(getApplicationContext(), FirebaseService.class);
        stopService(stopService);

        loadDataOnCreate();

        Logout.setOnClickListener(new View.OnClickListener() {
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
        firebaseFirestore = FirebaseFirestore.getInstance();
        documentReference = firebaseFirestore.collection("users").document(uid);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    String data = (String) documentSnapshot.get("Default TrackerID");
                    String fname = (String) documentSnapshot.get("fname");
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
        firebaseFirestore = FirebaseFirestore.getInstance();
        documentReference = firebaseFirestore.collection("users").document(uid);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    String data = (String) documentSnapshot.get("Default TrackerID");
                    if(data != null){
                        DefaultTracker = data;
                    }
                }
            }
        });
    }

    private void onGridClick(GridLayout mainGrid) {
        for (int i = 0; i < mainGrid.getChildCount(); i++) {
            CardView cardView = (CardView) mainGrid.getChildAt(i);
            final int id = i;
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(id == 0)
                    {
                        startActivity(new Intent(getApplicationContext(), LiveLocation.class));
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

                        if(DefaultTracker != null) {
                            serviceIntent.putExtra("inputExtra", DefaultTracker);
                            ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
                        }
                    }
                    else if(id == 6)
                    {
                        startActivity(new Intent(getApplicationContext(),SettingsActivity.class));
                    }
                    else if(id == 7){
                        Intent serviceIntent = new Intent(getApplicationContext(), FirebaseService.class);
                        stopService(serviceIntent);
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