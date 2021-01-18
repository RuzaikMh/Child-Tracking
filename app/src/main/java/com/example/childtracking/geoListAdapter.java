package com.example.childtracking;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class geoListAdapter extends ArrayAdapter<MyLatLng> {

    private static final String TAG = "geoAdapter";
    private Context mContext;
    int mResource;


    public geoListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<MyLatLng> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String name = getItem(position).getName();
        double radius = getItem(position).getRadius();
        String ref = getItem(position).getGeoFenceKey();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource,parent,false);

        TextView txName = convertView.findViewById(R.id.txtGeoName);
        TextView txRadius = convertView.findViewById(R.id.txtGeoRadius);
        TextView txRef = convertView.findViewById(R.id.txtGeoRef);

        txName.setText("Name : "+name);
        txRadius.setText("Radius : "+ radius);
        txRef.setText(ref);

        return convertView;
    }
}
