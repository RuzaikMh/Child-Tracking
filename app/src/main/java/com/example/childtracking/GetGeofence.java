package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.LatLng;
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
    private List<LatLng> dangerousArea1 = new ArrayList<>();
    private ListView myListView;
    private IOnLoadLocationListener listener;
    private ArrayAdapter<LatLng> arrayAdapter;
    private String DefaultTracker;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_geofence2);
        getData();
        arrayAdapter = new ArrayAdapter<LatLng>(this,android.R.layout.simple_list_item_1,dangerousArea1);
        myListView = (ListView) findViewById(R.id.ListView);
        myListView.setAdapter(arrayAdapter);

    }

    public void getData(){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference = firebaseFirestore.collection("users").document(uid);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    DefaultTracker = documentSnapshot.getString("Default TrackerID");

                    FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+DefaultTracker).child("Geo-fence areas")
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    List<MyLatLng> latLngList = new ArrayList<>();
                                    for(DataSnapshot locationSnapshot : snapshot.getChildren())
                                    {
                                        MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                                        latLngList.add(latLng);
                                        Log.d(TAG, "keys: " + locationSnapshot.getKey());
                                    }
                                    onLoadLocationSuccess(latLngList);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    listener.onLocationFailed(error.getMessage());
                                }
                            });
                }
            }
        });

    }


    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        for(MyLatLng myLatLng : latLngs)
        {
            LatLng convert = new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
            dangerousArea1.add(convert);
            arrayAdapter.notifyDataSetChanged();
        }

    }

    public void btnDelete(View view){
        if(DefaultTracker != null) {
            FirebaseDatabase.getInstance()
                    .getReference("Tracker/deviceId/"+ DefaultTracker)
                    .child("Geo-fence areas")
                    .removeValue();
            arrayAdapter.clear();
            getData();
        }else{
            Toast.makeText(this, "Wait until data loads", Toast.LENGTH_SHORT).show();
        }
    }
}