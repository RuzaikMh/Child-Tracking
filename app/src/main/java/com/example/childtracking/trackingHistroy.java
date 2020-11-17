package com.example.childtracking;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class trackingHistroy extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker marker;
    private static final String URL_PRODUCTS = "https://childgps.000webhostapp.com/getData.php";
    List<History> productList;
    private static final String TAG = "Histroy";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadHistroy();
        setContentView(R.layout.activity_tracking_histroy);
        productList = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL_PRODUCTS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            //converting the string to json array object
                            JSONArray array = new JSONArray(response);

                            //traversing through all the object
                            for (int i = 0; i < array.length(); i++) {

                                //getting product object from json array
                                JSONObject product = array.getJSONObject(i);

                                //adding the product to product list
                                productList.add(new History(
                                        product.getInt("Id"),
                                        product.getString("deviceID"),
                                        product.getDouble("logitude"),
                                        product.getDouble("latitude")
                                ));
                                Log.d(TAG, "history: " + productList.get(i).getLatitude() + productList.get(i).getLongitude());
                                LatLng add = new LatLng(productList.get(i).getLatitude(), productList.get(i).getLongitude());
                                mMap.addMarker(new MarkerOptions()
                                        .position(add));
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(add));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        //adding our stringrequest to queue
        Volley.newRequestQueue(this).add(stringRequest);

        for(int i=0 ; i < productList.size(); i++){
//            LatLng add = new LatLng(productList.get(i).getLatitude(), productList.get(i).getLongitude());
//            googleMap.addMarker(new MarkerOptions()
//                    .position(add));

        }

        // Add a marker in Sydney and move the camera
//        for(int i = 0 ; i < productList.size() ; i++){
//            Log.d(TAG, "onMapReady: " + productList.get(i));
//        }
    }

    public void loadHistroy(){

    }

}