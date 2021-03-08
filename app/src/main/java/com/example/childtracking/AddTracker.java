package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.List;

public class AddTracker extends AppCompatActivity {

    private static final String TAG = "add tracker" ;
    EditText ID,password;
    String deviceID,pass,uid;
    ProgressBar progressBar;
    Button add;
    FirebaseFirestore firebaseFirestore;
    DocumentReference documentReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tracker);
        ID = findViewById(R.id.trackerIDtxt);
        password= findViewById(R.id.passTxt);
        progressBar = findViewById(R.id.progressBar3);
        add = findViewById(R.id.btnAdd);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getCurrentFocus() != null) {
                    InputMethodManager inputManager = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                }

                deviceID = ID.getText().toString();
                pass = password.getText().toString();

                if(TextUtils.isEmpty(deviceID)){
                    ID.setError("Tracker Id is required");
                    return;
                }

                if(TextUtils.isEmpty(pass)){
                    password.setError("Enter Password");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tracker/deviceId");
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(deviceID)) {
                            String trackerPassword = dataSnapshot.child(deviceID+"/password").getValue(String.class);
                            if(pass.equals(trackerPassword)){
                                uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                firebaseFirestore = FirebaseFirestore.getInstance();
                                documentReference = firebaseFirestore.collection("users").document(uid);

                                documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()) {
                                            DocumentSnapshot documentSnapshot = task.getResult();
                                            List<String> data = (List<String>) documentSnapshot.get("Tracker IDs");

                                            if (data != null && data.contains(deviceID)) {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(AddTracker.this, "Child already added", Toast.LENGTH_SHORT).show();
                                            } else {
                                                documentReference.update("Tracker IDs", FieldValue.arrayUnion(deviceID));
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(AddTracker.this, "Child added", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        else {
                                            Log.d(TAG, "Cached get failed: ", task.getException());
                                        }
                                    }
                                });
                            }
                            else {
                                progressBar.setVisibility(View.GONE);
                                password.setError("Wrong password");
                            }
                        }
                        else {
                            progressBar.setVisibility(View.GONE);
                            ID.setError("The ID does't exits");
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
