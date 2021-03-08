package com.example.childtracking;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String CHANNEL_ID = "Child Tracker";
    public static  final String CHANNEL_IN_APP = "Child Tracker Alerts";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Child GPS Tracker",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_IN_APP,
                    "Child Tracker Notification",
                    NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(channel);
        }
    }
}
