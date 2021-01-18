package com.example.childtracking;

public class MyLatLng {
    private  double latitude;
    private  double longitude;
    private double radius;
    private String name;
    private String geoFenceKey;

    public MyLatLng() {
    }

    public String getGeoFenceKey() {
        return geoFenceKey;
    }

    public void setGeoFenceKey(String geoFenceKey) {
        this.geoFenceKey = geoFenceKey;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
