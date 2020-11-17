package com.example.childtracking;

public class History {
    private int id;
    private String deviceId;
    private double longitude;
    private double latitude;

    public History(int id, String deviceId, double longitude, double latitude) {
        this.id = id;
        this.deviceId = deviceId;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public int getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }
}
