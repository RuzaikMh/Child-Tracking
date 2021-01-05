package com.example.childtracking;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class LiveLocation extends FragmentActivity implements OnMapReadyCallback,
         GoogleMap.OnMapLongClickListener, GeoQueryEventListener, IOnLoadLocationListener {

    private static final String TAG = "LiveLocation";
    private static final int REQUEST_ENABLE_LOCATION = 12;

    private ImageView mCustomButton;
    Marker mCurrLocationMarker,marker;
    private GoogleMap mMap;
    Circle circle;
    private List<Circle> geoCircle;
    private float GEOFENCE_RADIUS = 200;
    private GeoFire geoFire;
    public List<LatLng> dangerousArea = new ArrayList<>();
    private DatabaseReference myLocationRef;
    private IOnLoadLocationListener listener;
    GeoQuery geoQuery;
    private List<GeoQuery> geoQueryList = new ArrayList<>();
    SupportMapFragment mapFragment;
    String radiusMeter,uid,DefaultTracker,tracker,getExtra,syncInterval;
    double radius;
    FirebaseFirestore rootRef;
    DocumentReference uidRef;
    FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private int enter, exit, move;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_live_location);

        Intent intent = getIntent();
        DefaultTracker = intent.getStringExtra("defaultTracker");
        Toast.makeText(this, "Default tracker is " + DefaultTracker, Toast.LENGTH_SHORT).show();
        settingGeoFire();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mCustomButton = (ImageView) findViewById(R.id.custom_button);
        mCustomButton.setClickable(true);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        radiusMeter = sharedPreferences.getString("radiusMeters","0.2");
        GEOFENCE_RADIUS = Float.parseFloat(radiusMeter) * 1000;
        radius = Double.parseDouble(radiusMeter);
    }

    private void settingGeoFire(){
        myLocationRef = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + DefaultTracker);
        geoFire = new GeoFire(myLocationRef);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        setConrolsPositions();

        readData(new firebaseCallback() {
            @Override
            public void onCallback(double longitude, double latitude) {
                geoFire.setLocation("Child "+DefaultTracker, new GeoLocation(latitude,longitude));
            }
        });

        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();

        mCustomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+tracker);
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double latitude = snapshot.child("latitude").getValue(Double.class);
                        Double longitude = snapshot.child("logitude").getValue(Double.class);
                        Log.d(TAG, "defalut here: " + tracker);
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

        mMap.setOnMapLongClickListener(this);

        listener = this;
        FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + DefaultTracker).child("Geo-fence areas")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //update the dangerous area list
                        List<MyLatLng> latLngList = new ArrayList<>();
                        for(DataSnapshot locationSnapshot : snapshot.getChildren())
                        {
                            MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                            latLngList.add(latLng);
                            Log.d(TAG, "see here : " + latLngList.size());
                        }
                        listener.onLoadLocationSuccess(latLngList);
                        Log.d(TAG, "see here : " + latLngList.size());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onLocationFailed(error.getMessage());
                    }
                });
    }

    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        dangerousArea = new ArrayList<>();
        for(MyLatLng myLatLng : latLngs)
        {
            LatLng convert = new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
            dangerousArea.add(convert);
        }

        if(geoQueryList != null && !geoQueryList.isEmpty()){
            Log.d(TAG, "geoQuery list check before remove" + geoQueryList);
            for(GeoQuery geoQuery : geoQueryList){
                geoQuery.removeAllListeners();
            }
            geoQueryList.clear();
        }

        if(geoCircle != null) {
            deleteCircle();
        }
        for(LatLng latLng1 : dangerousArea){
            addCircle(latLng1,GEOFENCE_RADIUS);

            // creates a new query around the location
            geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng1.latitude, latLng1.longitude), radius);
            geoQuery.addGeoQueryEventListener(LiveLocation.this);
            geoQueryList.add(geoQuery);
            Log.d(TAG, "geoQuery list check" + geoQueryList);
        }
    }

    @Override
    public void onLocationFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("Child",String.format("%s entered the dangerous area",key));
        enter++;
        Log.d(TAG, "onKeyEntered: test " + enter);
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("Child",String.format("%s leaved the dangerous area",key));
        exit++;
        Log.d(TAG, "onKeyExited: test " + exit);
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("Child",String.format("%s move within the dangerous area",key));
        move++;
        Log.d(TAG, "onKeyMoved: test " + move);
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private interface firebaseCallback{
        void onCallback(double longitude, double latitude);
    }

    private void readData(final firebaseCallback firebaseCallback){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef = FirebaseFirestore.getInstance();
        uidRef = rootRef.collection("users").document(uid);
        uidRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot document = task.getResult();
                tracker = (String) document.get("Default TrackerID");

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+tracker);
                databaseReference.addValueEventListener(new ValueEventListener() {
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

                        firebaseCallback.onCallback(longitude,latitude);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d(TAG, "firebase callback error : " + databaseError);
                    }
                });
            }
        });
    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        LatLng add = new LatLng(latLng.latitude,latLng.longitude);

            FirebaseDatabase.getInstance()
                    .getReference("Tracker/deviceId/" + DefaultTracker)
                    .child("Geo-fence areas")
                    .push()
                    .setValue(add)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(LiveLocation.this, "New geo-fence Added!", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(LiveLocation.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
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
        circleOptions.strokeColor(4);
        circle = mMap.addCircle(circleOptions);
        geoCircle.add(circle);
    }

    private void sendNotification(String title, String content) {
        Toast.makeText(this,""+content,Toast.LENGTH_SHORT).show();

        String NOTIFICATION_CHANNEL_ID = "Child Tracking";
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"My notification",
                    NotificationManager.IMPORTANCE_DEFAULT);

            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[] {0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(),notification);
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
