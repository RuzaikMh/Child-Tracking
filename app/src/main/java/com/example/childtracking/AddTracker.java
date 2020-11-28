package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firestore.v1.WriteResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTracker extends AppCompatActivity {

    private static final String TAG = "add tracker" ;
    EditText ID,password,child;
    String deviceID;
    String pass;
    String childName;
    int passInt;
    ProgressBar progressBar;
    Button add;
    String uid;
    FirebaseFirestore rootRef;
    DocumentReference uidRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tracker);

        ID = findViewById(R.id.trackerIDtxt);
        password= findViewById(R.id.passTxt);
        //child = findViewById(R.id.editTextChild);
        progressBar = findViewById(R.id.progressBar3);
        add = findViewById(R.id.txtAdd);


        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                deviceID = ID.getText().toString();
                pass = password.getText().toString();
                //childName = child.getText().toString();
                passInt = Integer.parseInt(pass);
                progressBar.setVisibility(View.VISIBLE);
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tracker/deviceId");
                databaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(deviceID)) {
                            int status = dataSnapshot.child(deviceID+"/password").getValue(Integer.class);
                            if(passInt == status){
                                uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                rootRef = FirebaseFirestore.getInstance();
                                uidRef = rootRef.collection("users").document(uid);
                                uidRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            List<String> data = (List<String>) document.get("Tracker IDs");
                                            Log.d(TAG, "onComplete: paaru : " + data);
                                            if (data != null && data.contains(deviceID)) {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(AddTracker.this, "Child already added", Toast.LENGTH_SHORT).show();
                                            } else {
                                                uidRef.update("Tracker IDs", FieldValue.arrayUnion(deviceID));
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(AddTracker.this, "Child added", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        else {
                                            Log.d(TAG, "Cached get failed: ", task.getException());
                                        }
                                    }
                                });
                                //uidRef.update("Tracker IDs", FieldValue.arrayUnion(deviceID));
                                //Toast.makeText(AddTracker.this, "Child added ID :" + uid, Toast.LENGTH_SHORT).show();
                            }
                            else {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AddTracker.this, "Wrong password", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AddTracker.this, "The ID does't exits", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    public void btnViewChild(View view){
        startActivity(new Intent(getApplicationContext(),ViewChild.class));
    }
}
