package com.example.childtracking;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class geoListAdapter extends ArrayAdapter<MyLatLng> {

    private static final String TAG = "geoAdapter";
    private Context mContext;
    private int mResource;
    private List<MyLatLng> GeoList = new ArrayList<>();



    public geoListAdapter(@NonNull Context context, int resource, @NonNull List<MyLatLng> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        GeoList = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;

        if(listItemView == null)
            listItemView = LayoutInflater.from(mContext).inflate(mResource,parent,false);

        MyLatLng CurrentGeo = GeoList.get(position);

        TextView txName = listItemView.findViewById(R.id.txtGeoName);
        TextView txRadius = listItemView.findViewById(R.id.txtGeoRadius);
        TextView txRef = listItemView.findViewById(R.id.txtGeoRef);

        String name = CurrentGeo.getName();
        double radius = CurrentGeo.getRadius();
        String ref = CurrentGeo.getGeoFenceKey();

        txName.setText("Name : "+name);
        txRadius.setText("Radius : "+ radius);
        txRef.setText(ref);

        return listItemView;
    }
}
