package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
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
import java.util.ArrayList;
import java.util.List;


public class GetGeofence extends AppCompatActivity {

    private static final String TAG = "GetGeofence";
    private ListView myListView;
    private Button delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_geofence2);

        myListView = findViewById(R.id.ListView);
        delete = findViewById(R.id.button7);

        getData(new firebaseCallBack() {
            @Override
            public void onCallback(List<MyLatLng> myLatLngArrayList, final String Tracker) {

                final geoListAdapter adapter = new geoListAdapter(getApplicationContext(),
                        R.layout.adpter_view_layout,myLatLngArrayList);

                myListView.setAdapter(adapter);

                myListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, final View view, int i, long l) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(GetGeofence.this);
                        builder.setTitle("Do you want to remove this geo-fence")
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                })
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        TextView ref = view.findViewById(R.id.txtGeoRef);
                                        String geoFenceRef = ref.getText().toString();
                                        DatabaseReference db = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+ Tracker)
                                                .child("Geo-fence areas").child(geoFenceRef);
                                        db.removeValue();
                                        adapter.notifyDataSetChanged();

                                    }
                                });

                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();

                        return true;
                        //means that the event is consumed. It is handled. No other click events will be notified.
                    }
                });

                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FirebaseDatabase.getInstance()
                                .getReference("Tracker/deviceId/"+Tracker)
                                .child("Geo-fence areas")
                                .removeValue();
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    public void getData(final firebaseCallBack firebaseCallBack){

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference = firebaseFirestore.collection("users").document(uid);

        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    final String DefaultTracker = documentSnapshot.getString("Default TrackerID");

                    FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+DefaultTracker).child("Geo-fence areas")
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    List<MyLatLng> latLngList = new ArrayList<>();
                                    for(DataSnapshot locationSnapshot : snapshot.getChildren())
                                    {
                                        MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                                        latLng.setGeoFenceKey(locationSnapshot.getKey());
                                        latLngList.add(latLng);
                                        Log.d(TAG, "keys: " + locationSnapshot.getKey());
                                    }
                                    firebaseCallBack.onCallback(latLngList, DefaultTracker);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.d(TAG, "onCancelled: "+ error.getMessage());
                                }
                            });
                }
            }
        });

    }

    private interface firebaseCallBack{
        void onCallback(List<MyLatLng> myLatLngArrayList, String Tracker);
    }
}