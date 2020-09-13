package com.example.childtracking;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import com.google.android.gms.location.GeofencingClient;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class LiveLocation extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMapLongClickListener, GeoQueryEventListener, IOnLoadLocationListener {

    private ImageView mCustomButton;
    private static final String TAG = "LiveLocation";
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    private GoogleMap mMap;
    Marker marker;
    Circle circle;
    private List<Circle> geoCircle;
    private GeofencingClient geofencingClient;
    private float GEOFENCE_RADIUS = 200;
    private GeoFire geoFire;
    public List<LatLng> dangerousArea = new ArrayList<>();
    public List<LatLng> saveLocation = new ArrayList<>();
    private DatabaseReference myLocationRef;
    private IOnLoadLocationListener listener;
    GeoQuery geoQuery;
    SupportMapFragment mapFragment;
    String radiusMeter;
    String syncInterval;
    double radius;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        mCustomButton = (ImageView) findViewById(R.id.custom_button);
        mCustomButton.setClickable(true);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingGeoFire();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        radiusMeter = sharedPreferences.getString("radiusMeters","0.2");
        GEOFENCE_RADIUS = Float.parseFloat(radiusMeter) * 1000;
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
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mCustomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Current Location");
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double latitude = snapshot.child("Lat").getValue(Double.class);
                        Double longitude = snapshot.child("Lag").getValue(Double.class);

                        CameraPosition position = new CameraPosition.Builder()
                                .target(new LatLng(latitude,longitude)) // Sets the new camera position
                                .zoom(17) // Sets the zoom
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
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        readData(new firebaseCallback() {
            @Override
            public void onCallback(double longitude, double latitude) {

            }
        });


        mMap.setOnMapLongClickListener(this);
        listener = this;
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

                        }
                        listener.onLoadLocationSuccess(latLngList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onLocationFailed(error.getMessage());
                    }
                });
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

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("Child",String.format("%s entered the dangerous area",key));
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("Child",String.format("%s leaved the dangerous area",key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("Child",String.format("%s move within the dangerous area",key));
    }

    @Override
    public void onGeoQueryReady() {
    
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void settingGeoFire(){
        myLocationRef = FirebaseDatabase.getInstance().getReference("Current Location");
        geoFire = new GeoFire(myLocationRef);
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

    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        dangerousArea = new ArrayList<>();
        for(MyLatLng myLatLng : latLngs)
        {
            LatLng convert = new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
            dangerousArea.add(convert);
        }

        if(geoQuery != null){
            geoQuery.removeAllListeners();
        }

        if(geoCircle != null) {
            deleteCircle();
        }
        for(LatLng latLng1 : dangerousArea){
            addCircle(latLng1,GEOFENCE_RADIUS);

            geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng1.latitude,latLng1.longitude),radius);
            geoQuery.addGeoQueryEventListener(LiveLocation.this);
            Log.d(TAG, "onLoadLocationSuccess: loool"+GEOFENCE_RADIUS+"  "+radius);
        }
    }

    @Override
    public void onLocationFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();
    }

    private interface firebaseCallback{
        void onCallback(double longitude, double latitude);
    }

    private void readData(final firebaseCallback firebaseCallback){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Current Location");
        ValueEventListener listener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double latitude = dataSnapshot.child("Lat").getValue(Double.class);
                Double longitude = dataSnapshot.child("Lag").getValue(Double.class);

                LatLng location = new LatLng(latitude, longitude);
                if (marker != null) {
                    marker.remove();
                }
                marker = mMap.addMarker(new MarkerOptions().position(location).title("Child Location")
                .icon(bitmapDescriptor(getApplicationContext(),R.drawable.ic_baseline_emoji_people_24)));

                geoFire.setLocation("Child", new GeoLocation(latitude,longitude));

                firebaseCallback.onCallback(longitude,latitude);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(new Criteria(), true);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location locations = locationManager.getLastKnownLocation(provider);
        List<String> providerList = locationManager.getAllProviders();
        if (null != locations && null != providerList && providerList.size() > 0) {
            double longitude = locations.getLongitude();
            double latitude = locations.getLatitude();
            Geocoder geocoder = new Geocoder(getApplicationContext(),
                    Locale.getDefault());
            try {
                List<Address> listAddresses = geocoder.getFromLocation(latitude,
                        longitude, 1);
                if (null != listAddresses && listAddresses.size() > 0) {
                    String state = listAddresses.get(0).getAdminArea();
                    String country = listAddresses.get(0).getCountryName();
                    String subLocality = listAddresses.get(0).getSubLocality();
                    markerOptions.title("" + latLng + "," + subLocality + "," + state
                            + "," + country);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mCurrLocationMarker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
                    this);
        }
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "permission denied",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        saveLocation.add(new LatLng(latLng.latitude,latLng.longitude));
        LatLng mine = new LatLng(latLng.latitude,latLng.longitude);

        FirebaseDatabase.getInstance()
                .getReference("DangerousArea")
                .child("Locations")
                .push()
                .setValue(mine)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(LiveLocation.this, "Geo-fence Added!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(LiveLocation.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
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
}
