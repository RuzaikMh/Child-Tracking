package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toggleButton = (ToggleButton) findViewById(R.id.toggle1);
        getStatus();
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    FirebaseDatabase.getInstance()
                            .getReference("Alert")
                            .child("Status")
                            .setValue(1);
                } else {
                    FirebaseDatabase.getInstance()
                            .getReference("Alert")
                            .child("Status")
                            .setValue(0);
                }
            }
        });
    }

    public void btnLiveLocation(View view){
        startActivity(new Intent(getApplicationContext(),LiveLocation.class));
    }

    public void btnGeoFence(View view){
        startActivity(new Intent(getApplicationContext(),GetGeofence.class));
    }

    public void logout(View view) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getApplicationContext(),Login.class));
        finish();
    }

    public void getStatus(){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Alert");
        ValueEventListener listener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int status = dataSnapshot.child("Status").getValue(Integer.class);
                if(status == 1){
                    toggleButton.setChecked(true);
                }else{
                    toggleButton.setChecked(false);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}