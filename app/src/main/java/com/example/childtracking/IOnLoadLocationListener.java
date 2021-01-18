package com.example.childtracking;
import java.util.List;

public interface IOnLoadLocationListener {
    void onLoadLocationSuccess(List<MyLatLng> latLngs);
    void onLocationFailed(String message);
}
