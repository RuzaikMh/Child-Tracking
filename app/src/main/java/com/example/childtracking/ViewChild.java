package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
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

    Button Mdefault;
    DocumentReference documentReference;
    ListView listView;
    List<String> data = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    String selectedItem;
    TextView txtDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_child);

        Mdefault = findViewById(R.id.MakeDefaultBtn);
        listView = findViewById(R.id.ChidList);
        txtDefault = findViewById(R.id.welcomeMsg2);

        readTrackerData(new firestoreCallBack() {
            @Override
            public void onCallBack(List<String> list, String Dtracker) {

            txtDefault.setText("Current Default Tracker ID : "+Dtracker);

            arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, list);
            listView.setAdapter(arrayAdapter);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for(int a = 0; a < parent.getChildCount(); a++)
                {
                    parent.getChildAt(a).setBackgroundColor(Color.TRANSPARENT);
                }

                view.setBackgroundColor(Color.rgb(70,93,202));
                selectedItem = (String) listView.getItemAtPosition(position);

            }
        });
    }

    public void makeDefault(View view){
        if(selectedItem != null) {

            readTrackerData(new firestoreCallBack() {
                @Override
                public void onCallBack(List<String> list, String Dtracker) {
                    if (!Dtracker.equals(selectedItem)) {
                        documentReference.update("Default TrackerID", selectedItem).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                            Toast.makeText(ViewChild.this, "Default : " + selectedItem, Toast.LENGTH_SHORT).show();
                            txtDefault.setText("Current Default Tracker ID : "+selectedItem);

                            Intent serviceIntent = new Intent(getApplicationContext(), FirebaseService.class);
                            stopService(serviceIntent);
                            serviceIntent.putExtra("inputExtra", selectedItem);
                            ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
                            }
                        });
                    } else {
                        Toast.makeText(ViewChild.this, "Already selected as default", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            } else {
                Toast.makeText(this, "Please Select an item", Toast.LENGTH_SHORT).show();
        }
    }

    public void delete(View view) {
        if (selectedItem != null) {

            readTrackerData(new firestoreCallBack() {
                @Override
                public void onCallBack(List<String> list, String Dtracker) {
                    if (Dtracker.equals(selectedItem)) {
                        Toast.makeText(ViewChild.this, "You cannot delete default", Toast.LENGTH_SHORT).show();
                    } else {
                        documentReference.update("Tracker IDs", FieldValue.arrayRemove(selectedItem));
                        finish();
                        startActivity(getIntent());
                    }
                }
            });
        } else {
                Toast.makeText(this, "Please Select an item", Toast.LENGTH_SHORT).show();
        }
    }

    private interface firestoreCallBack{
        void onCallBack(List<String> list , String Dtracker);
    }

    private void readTrackerData(final firestoreCallBack firestoreCallBack){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        documentReference = firestore.collection("users").document(uid);

        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    String id = documentSnapshot.getString("Default TrackerID");
                    data = (List<String>) documentSnapshot.get("Tracker IDs");

                    firestoreCallBack.onCallBack(data,id);
                }
            }
        });
    }
}