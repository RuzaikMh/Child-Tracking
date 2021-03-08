package com.example.childtracking;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;


public class LiveLocation extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener {

    private static final String TAG = "LiveLocation";
    private static final int REQUEST_ENABLE_LOCATION = 12;

    private ImageView mCustomButton;
    Marker marker;
    private GoogleMap mMap;
    Circle circle;
    private List<Circle> geoCircle;
    SupportMapFragment mapFragment;
    String radiusMeter, uid, DefaultTracker;
    double radius;
    FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private DatabaseReference databaseReference, GeofenceRef, CurrentLocationRef;
    private ValueEventListener GeofenceEvent, CurrentLocationEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_location);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mCustomButton = findViewById(R.id.custom_button);
        mCustomButton.setClickable(true);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        radiusMeter = sharedPreferences.getString("radiusMeters","0.2");
        radius = Double.parseDouble(radiusMeter);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        setConrolsPositions();

        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();

        getDefaultTracker(new DefaultTrackerCallback() {
            @Override
            public void onCallBack(final String defaultTracker) {
                Toast.makeText(LiveLocation.this, "Default  Tracker is " + defaultTracker, Toast.LENGTH_SHORT).show();

                loadGeofenceAreas(defaultTracker);

                readTrackerLocation(defaultTracker);

                mCustomButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().
                                getReference("Tracker/deviceId/"+defaultTracker);
                        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Double latitude = snapshot.child("latitude").getValue(Double.class);
                                Double longitude = snapshot.child("logitude").getValue(Double.class);

                                CameraPosition position = new CameraPosition.Builder()
                                        .target(new LatLng(latitude,longitude)) // Sets the new camera position
                                        .zoom(15) // Sets the zoom
                                        .bearing(180) // Rotate the camera
                                        .tilt(30) // Set the camera tilt
                                        .build(); // Creates a CameraPosition from the builder

                                mMap.animateCamera(CameraUpdateFactory
                                        .newCameraPosition(position), 3000,null);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                });
            }
        });

        mMap.setOnMapLongClickListener(this);
    }

    private interface DefaultTrackerCallback{
        void onCallBack(String defaultTracker);
    }

    private void getDefaultTracker(final DefaultTrackerCallback defaultTrackerCallback){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference = firebaseFirestore.collection("users").document(uid);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    DefaultTracker = documentSnapshot.getString("Default TrackerID");

                    defaultTrackerCallback.onCallBack(DefaultTracker);
                }
            }
        });
    }

    public void loadGeofenceAreas(String trackerId){
       GeofenceRef =  FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + trackerId).child("Geo-fence areas");
            GeofenceEvent = GeofenceRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<MyLatLng> latLngList = new ArrayList<>();
                        for(DataSnapshot locationSnapshot : snapshot.getChildren())
                        {
                            MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                            latLngList.add(latLng);
                        }
                        if(geoCircle != null) {
                            deleteCircle();
                        }

                        for(MyLatLng myLatLng : latLngList)
                        {
                            LatLng latLng1 = new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
                            addCircle(latLng1, (float) myLatLng.getRadius() * 1000);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: " + error.getMessage());
                    }
                });
    }

    private void readTrackerLocation(String DefaultTrackerId){
        CurrentLocationRef = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+DefaultTrackerId);
            CurrentLocationEvent = CurrentLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                Double longitude = dataSnapshot.child("logitude").getValue(Double.class);

                LatLng location = new LatLng(latitude, longitude);
                if (marker != null) {
                    marker.remove();
                }
                marker = mMap.addMarker(new MarkerOptions().position(location).title("Child Location")
                        .icon(bitmapDescriptor(getApplicationContext(),R.drawable.ic_baseline_emoji_people_24)));

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "firebase callback error : " + databaseError);
            }
        });
    }

    @Override
    public void onMapLongClick(final LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //LayoutInflater takes an XML file as input and builds the View object from it
        LayoutInflater layoutInflater = this.getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.layout_dialog,null);
        final EditText geoName = view.findViewById(R.id.editTxtGeo);

        builder.setView(view)
                .setTitle("Enter Geo-fence name")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final String geoFenceName = geoName.getText().toString().trim();

                        if(geoFenceName.isEmpty()){
                            Toast.makeText(LiveLocation.this, "Geo-fence not added name empty", Toast.LENGTH_SHORT).show();
                        }else {
                            LatLng add = new LatLng(latLng.latitude,latLng.longitude);

                            databaseReference = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + DefaultTracker).child("Geo-fence areas");
                            final String key = databaseReference.push().getKey();

                            databaseReference.child(key).setValue(add).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(LiveLocation.this, "New geo-fence Added!", Toast.LENGTH_SHORT).show();
                                    databaseReference.child(key).child("radius").setValue(radius);
                                    databaseReference.child(key).child("name").setValue(geoFenceName);
                                }
                            });
                        }
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void deleteCircle(){

        for(int i = 0 ; i <= geoCircle.size() - 1 ; i++){
            Circle mCircle = geoCircle.get(i);
            mCircle.remove();
        }
        geoCircle.clear();
    }

    private void addCircle(LatLng latLng, float radius){
        if(geoCircle == null){
            geoCircle = new ArrayList<>();
        }

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255,255,0,0));
        circleOptions.fillColor(Color.argb(64,255,0,0));
        circle = mMap.addCircle(circleOptions);
        geoCircle.add(circle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(GeofenceEvent != null){
            GeofenceRef.removeEventListener(GeofenceEvent);
        }
        if(CurrentLocationEvent != null){
            CurrentLocationRef.removeEventListener(CurrentLocationEvent);
        }
    }

    void setConrolsPositions() {
        try {
            // get parent view for default Google Maps control button
            final ViewGroup parent = (ViewGroup) mapFragment.getView().findViewWithTag("GoogleMapMyLocationButton").getParent();
            parent.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        // get view for default Google Maps control button
                        View defaultButton = mapFragment.getView().findViewWithTag("GoogleMapMyLocationButton");

                        // remove custom button view from activity root layout
                        ViewGroup customButtonParent = (ViewGroup) mCustomButton.getParent();
                        customButtonParent.removeView(mCustomButton);

                        // add custom button view to Google Maps control button parent
                        ViewGroup defaultButtonParent = (ViewGroup) defaultButton.getParent();
                        defaultButtonParent.addView(mCustomButton);

                        // create layout with same size as default Google Maps control button
                        RelativeLayout.LayoutParams customButtonLayoutParams = new RelativeLayout.LayoutParams(defaultButton.getHeight(), defaultButton.getHeight());

                        // align custom button view layout relative to defaultButton
                        customButtonLayoutParams.addRule(RelativeLayout.ALIGN_LEFT, defaultButton.getId());
                        customButtonLayoutParams.addRule(RelativeLayout.BELOW, defaultButton.getId());

                        // add other settings (optional)
                        mCustomButton.setAlpha(defaultButton.getAlpha());
                        mCustomButton.setPadding(defaultButton.getPaddingLeft(), defaultButton.getPaddingTop(),
                                defaultButton.getPaddingRight(), defaultButton.getPaddingBottom());

                        // apply layout settings to custom button view
                        mCustomButton.setLayoutParams(customButtonLayoutParams);
                        mCustomButton.setVisibility(View.VISIBLE);


                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private BitmapDescriptor bitmapDescriptor(Context context, int vectorResId){
        Drawable vectorDrawable = ContextCompat.getDrawable(context,vectorResId);
        vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void getDeviceLocation(){
        try {
            if(locationPermissionGranted){
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful()){
                            lastKnownLocation = task.getResult();
                            if(lastKnownLocation != null){
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()),15));
                            }
                        }else {
                            Log.d(TAG, "onComplete: Current location is null.");
                            Log.e(TAG,  "Exception: %s", task.getException());
                        }
                    }
                });
            }

        }catch (SecurityException e){
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void getLocationPermission(){
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED){
            locationPermissionGranted = true;
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ENABLE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       locationPermissionGranted = false;
        if(requestCode == REQUEST_ENABLE_LOCATION){
           if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
              locationPermissionGranted = true;
           }
       }
        updateLocationUI();
    }

    @SuppressLint("MissingPermission")
    private void updateLocationUI(){
        if(mMap == null){
            return;
        }
        try {
            if(locationPermissionGranted){
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            }else{
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        }catch (SecurityException e){
            Log.d(TAG, "updateLocationUI: Exception" + e.getMessage());
        }
    }
}
