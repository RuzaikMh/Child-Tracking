package com.example.childtracking;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static  com.example.childtracking.App.CHANNEL_ID;

public class FirebaseService extends Service implements IOnLoadLocationListener, GeoQueryEventListener {

    private static final String TAG = "service";
    private IOnLoadLocationListener listener;
    private List<GeoQuery> geoQueryList = new ArrayList<>();
    private GeoQuery geoQuery;
    String DefaultTracker;
    DatabaseReference geoFence,fallDetect,location;
    ValueEventListener geoFenceListener,fallDetectListener,locationListener;
    DatabaseReference myLocationRef;
    GeoFire geoFire;

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
                .setContentTitle("Child Tracking")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.child5)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);


        settingGeoFire();

        readTrackerLocation(new firebaseCallBack() {
            @Override
            public void onCallBack(double longitude, double latitude) {
                geoFire.setLocation("Child "+DefaultTracker, new GeoLocation(latitude,longitude));
            }
        });

        listener = this;
        geoFence = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + DefaultTracker).child("Geo-fence areas");
               geoFenceListener = geoFence.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //update the dangerous area list
                        List<MyLatLng> latLngList = new ArrayList<>();
                        for(DataSnapshot locationSnapshot : snapshot.getChildren())
                        {
                            MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                            latLngList.add(latLng);
                        }
                        listener.onLoadLocationSuccess(latLngList);
                        Log.d(TAG, "geo list size : " + latLngList.size());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onLocationFailed(error.getMessage());
                    }
                });

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

            }
        });
        return START_NOT_STICKY;
    }

    private void settingGeoFire(){
        myLocationRef = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/" + DefaultTracker);
        geoFire = new GeoFire(myLocationRef);
    }

    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        if(geoQueryList != null && !geoQueryList.isEmpty()){
            Log.d(TAG, "geoQuery list check before remove" + geoQueryList);
            for(GeoQuery geoQuery : geoQueryList){
                geoQuery.removeAllListeners();
            }
            geoQueryList.clear();
        }

        for(MyLatLng myLatLng : latLngs)
        {
            geoQuery = geoFire.queryAtLocation(new GeoLocation(myLatLng.getLatitude(), myLatLng.getLongitude()), myLatLng.getRadius());
            geoQuery.addGeoQueryEventListener(FirebaseService.this);
            geoQueryList.add(geoQuery);
            Log.d(TAG, "geoQuery list check" + geoQueryList);
            Log.d(TAG, "geo-fence details : " + "Lat :"+myLatLng.getLatitude()+ " lng :" + myLatLng.getLongitude()+" radius : "+myLatLng.getRadius());
        }
    }

    @Override
    public void onLocationFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();
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

    private interface firebaseCallBack{
        void onCallBack(double longitude, double latitude);
    }

    private void readTrackerLocation(final firebaseCallBack firebaseCallBack){
        location = FirebaseDatabase.getInstance().getReference("Tracker/deviceId/"+DefaultTracker);
        locationListener = location.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double latitude = snapshot.child("latitude").getValue(Double.class);
                Double longitude = snapshot.child("logitude").getValue(Double.class);

                firebaseCallBack.onCallBack(longitude,latitude);
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

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(),notification);
    }
}
