package com.example.childtracking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static  com.example.childtracking.App.CHANNEL_ID;
import static com.example.childtracking.App.CHANNEL_IN_APP;

public class FirebaseService extends Service implements GeoQueryEventListener {

    private static final String TAG = "service";
    private String GROUP_KEY_GEO = "com.android.example.GEO";
    private String GROUP_KEY_BACKGROUND = "com.android.example.GEO";
    private List<GeoQuery> geoQueryList = new ArrayList<>();
    private GeoQuery geoQuery;
    private String DefaultTracker;
    private DatabaseReference geoFence,fallDetect,location;
    private ValueEventListener geoFenceListener,fallDetectListener,locationListener;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DefaultTracker = intent.getStringExtra("inputExtra");
        Log.d(TAG, "Extra Default : " + DefaultTracker);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background Service")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_baseline_child_care)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY_BACKGROUND)
                .build();
        startForeground(1, notification);

        settingGeoFire();

        readTrackerLocation();

        getFallAlerts();

        loadGeofenceAreas();

        return START_NOT_STICKY;
    }

    private void settingGeoFire(){
        myLocationRef = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + DefaultTracker);
        geoFire = new GeoFire(myLocationRef);
    }

    public void loadGeofenceAreas(){
        geoFence = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + DefaultTracker).child("Geo-fence areas");
        geoFenceListener = geoFence.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MyLatLng> latLngList = new ArrayList<>();
                for(DataSnapshot locationSnapshot : snapshot.getChildren()){
                    MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                    latLngList.add(latLng);
                }

                if(geoQueryList != null && !geoQueryList.isEmpty()){
                    Log.d(TAG, "geoQuery list check before remove" + geoQueryList);
                    for(GeoQuery geoQuery : geoQueryList){
                        geoQuery.removeAllListeners();
                    }
                    geoQueryList.clear();
                }

                for(MyLatLng myLatLng : latLngList){
                    geoQuery = geoFire.queryAtLocation(new GeoLocation(myLatLng.getLatitude(),
                            myLatLng.getLongitude()), myLatLng.getRadius());
                    geoQuery.addGeoQueryEventListener(FirebaseService.this);
                    geoQueryList.add(geoQuery);
                    Log.d(TAG, "geoQuery list check" + geoQueryList);
                    Log.d(TAG, "geo-fence details : " + "Lat :"+myLatLng.getLatitude()+ " lng :"
                            + myLatLng.getLongitude()+" radius : "+myLatLng.getRadius());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "loadGeofenceAreas error : " + error.getMessage());
            }
        });
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("Geo-fence triggered",String.format("%s entered the geo-fence area",key));
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("Geo-fence triggered",String.format("%s leaved the geo-fence area",key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("Geo-fence triggered",String.format("%s move within the geo-fence area",key));
    }

    @Override
    public void onGeoQueryReady() {
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {

    }

    private void getFallAlerts(){
        fallDetect = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + DefaultTracker);
        fallDetectListener = fallDetect.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.child("fall").getValue(String.class);
                if(status != null) {
                    if (status.equals("true")) {
                        sendNotification("Child " + DefaultTracker + " Fell" , "the system detected a fall");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.getMessage());
            }
        });
    }

    private void readTrackerLocation(){
        location = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+DefaultTracker);
        locationListener = location.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double latitude = snapshot.child("latitude").getValue(Double.class);
                Double longitude = snapshot.child("logitude").getValue(Double.class);

                geoFire.setLocation("Child "+DefaultTracker, new GeoLocation(latitude,longitude));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "firebase callback error : " + error);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(geoQueryList != null && !geoQueryList.isEmpty()){
            Log.d(TAG, "geoQuery list check before remove" + geoQueryList);
            for(GeoQuery geoQuery : geoQueryList){
                geoQuery.removeAllListeners();
            }
            geoQueryList.clear();
        }

        geoFence.removeEventListener(geoFenceListener);
        fallDetect.removeEventListener(fallDetectListener);
        location.removeEventListener(locationListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendNotification(String title, String content) {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this,CHANNEL_IN_APP)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_baseline_child_care)
                .setContentIntent(contentIntent)
                .setGroup(GROUP_KEY_GEO)
                .build();

        notificationManager.notify(new Random().nextInt(),notification);
    }
}
