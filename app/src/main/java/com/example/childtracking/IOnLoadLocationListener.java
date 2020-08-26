package com.example.childtracking;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface IOnLoadLocationListener {
    void onLoadLocationSuccess(List<MyLatLng> latLngs);
    void onLocationFailed(String message);
}
