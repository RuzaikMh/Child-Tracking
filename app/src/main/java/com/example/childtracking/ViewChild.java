package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ViewChild extends AppCompatActivity {

    private static final String TAG = "viewChild";
    Button Mdefault;
    String uid;
    FirebaseFirestore rootRef;
    DocumentReference uidRef;
    ListView listView;
    List<String> data = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    String selectedItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_child);

        Mdefault = findViewById(R.id.MakeDefaultBtn);
        listView = findViewById(R.id.ChidList);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef = FirebaseFirestore.getInstance();
        uidRef = rootRef.collection("users").document(uid);
        Log.d(TAG, "data here : " + uidRef);
        uidRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    data = (List<String>) document.get("Tracker IDs");
                    Log.d(TAG, "data here : " + data);
                    if (data != null) {
                        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,data);
                        listView.setAdapter(arrayAdapter);
                        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                    } else {
                        openDialog();
                    }
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                for(int a = 0; a < adapterView.getChildCount(); a++)
                {
                    adapterView.getChildAt(a).setBackgroundColor(Color.TRANSPARENT);
                }

                view.setBackgroundColor(Color.rgb(223,227,238));
                selectedItem = (String) listView.getItemAtPosition(i);

            }
        });
    }

    public void openDialog(){
        Dialog newDialog = new Dialog();
        newDialog.show(getSupportFragmentManager(),"Dialog");
    }

    public void makeDefault(View view){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef = FirebaseFirestore.getInstance();
        uidRef = rootRef.collection("users").document(uid);

        uidRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    final String data = (String) document.get("Default TrackerID");
                    if(data != null){
                        if(!data.equals(selectedItem)){
                            uidRef.update("Default TrackerID",selectedItem).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(ViewChild.this, "Default : " + selectedItem, Toast.LENGTH_SHORT).show();
                                }
                            });

                            Intent serviceIntent = new Intent(getApplicationContext(), FirebaseService.class);
                            serviceIntent.putExtra("inputExtra", data);

                        }
                        else {
                            Toast.makeText(ViewChild.this, "Already selected as default", Toast.LENGTH_SHORT).show();
                        }
                    }

                    else{
                        uidRef.update("Default TrackerID",selectedItem).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(ViewChild.this, "Default : " + data, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });


    }

}