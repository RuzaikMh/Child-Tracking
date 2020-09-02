package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GetGeofence extends AppCompatActivity {

    private static final String TAG = "GetGeofence";
    private List<LatLng> dangerousArea1 = new ArrayList<>();
    private ListView myListView;
    private IOnLoadLocationListener listener;
    private ArrayAdapter<LatLng> arrayAdapter;

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
        FirebaseDatabase.getInstance()
                .getReference("DangerousArea")
                .child("Locations")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<MyLatLng> latLngList = new ArrayList<>();
                        for(DataSnapshot locationSnapshot : snapshot.getChildren())
                        {
                            MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                            latLngList.add(latLng);
                            Log.d(TAG, "onDataChange: LOOk" + latLngList.get(0));

                        }
                        onLoadLocationSuccess(latLngList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onLocationFailed(error.getMessage());
                    }
                });
    }


    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        for(MyLatLng myLatLng : latLngs)
        {
            LatLng convert = new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
            dangerousArea1.add(convert);
            arrayAdapter.notifyDataSetChanged();
            Log.d(TAG, "onLoadLocationSuccess: LOOk1" + dangerousArea1);
        }

    }

    public void btnDelete(View view){
        FirebaseDatabase.getInstance()
                .getReference("DangerousArea")
                .child("Locations")
                .removeValue();
        arrayAdapter.clear();
        getData();
    }
}