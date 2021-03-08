package com.example.childtracking;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class trackingHistroy extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    List<History> TrackerList;
    private static final String TAG = "Histroy";
    private LatLng latLng;
    private ImageView deleteView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_histroy);
        TrackerList = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        deleteView = findViewById(R.id.trash_imageView);

        deleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(trackingHistroy.this);
                builder.setTitle("Do you want clear all history related to this tracker")
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getDefault(new firebaseCallback() {
                                    @Override
                                    public void onCallback(String tracker) {
                                        String url = "https://childgps.000webhostapp.com/DeleteHistory.php?trackerID=" + tracker;

                                        StringRequest request = new StringRequest(Request.Method.GET, url,
                                                new Response.Listener<String>() {
                                                    @Override
                                                    public void onResponse(String response) {
                                                        Toast.makeText(trackingHistroy.this, response, Toast.LENGTH_SHORT).show();
                                                        if(response.equals("Tracking history deleted successfully")){
                                                            mMap.clear();
                                                        }
                                                    }
                                                }, new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Toast.makeText(trackingHistroy.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                        Volley.newRequestQueue(getApplicationContext()).add(request);
                                    }
                                });
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(false);

        getDefault(new firebaseCallback() {
            @Override
            public void onCallback(String tracker) {
                String Url = "https://childgps.000webhostapp.com/getData.php?default=" + tracker;

                StringRequest stringRequest = new StringRequest(Request.Method.GET, Url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    //converting the string to json array object
                                    JSONArray jsonArray = new JSONArray(response);

                                    //traversing through all the object
                                    for (int i = 0; i < jsonArray.length(); i++) {

                                        //getting tracker object from json array
                                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                                        //adding the tracker to TrackerList list
                                        TrackerList.add(new History(
                                                jsonObject.getInt("Id"),
                                                jsonObject.getString("deviceID"),
                                                jsonObject.getDouble("logitude"),
                                                jsonObject.getDouble("latitude")
                                        ));

                                        if(TrackerList.get(i).getLongitude() != 0 && TrackerList.get(i).getLatitude() != 0) {
                                            latLng = new LatLng(TrackerList.get(i).getLatitude(), TrackerList.get(i).getLongitude());
                                            mMap.addMarker(new MarkerOptions()
                                                    .position(latLng));
                                        }
                                    }
                                    if(latLng != null) {
                                        CameraPosition position = new CameraPosition.Builder()
                                                .target(latLng)
                                                .zoom(13)
                                                .bearing(180)
                                                .tilt(30)
                                                .build();
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 3000, null);
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d(TAG, "onErrorResponse Tracking history : " + error.getMessage());
                            }
                        });

                //adding our stringrequest to queue
                Volley.newRequestQueue(getApplicationContext()).add(stringRequest);
            }
        });
    }

    private interface firebaseCallback{
        void onCallback(String tracker);
    }

    private void getDefault(final firebaseCallback firebaseCallback){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference = firestore.collection("users").document(uid);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    String defaultID = documentSnapshot.getString("Default TrackerID");

                    firebaseCallback.onCallback(defaultID);
                }
            }
        });
    }
}