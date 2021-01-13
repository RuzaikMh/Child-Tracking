package com.example.childtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    public static final String TAG = "TAG";
    EditText mFullname,mEmail,mPassword,mTrackerId,mTrackerPassword;
    Button  mRegisterBtn;
    TextView mLoginBtn;
    FirebaseAuth fAuth;
    ProgressBar progressBar;
    FirebaseFirestore firestore;
    String userID,password,email,fullname,trackerId,trackerPass,uid;
    int TrackerPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        mFullname = findViewById(R.id.fullName);
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mRegisterBtn = findViewById(R.id.btnRegister);
        mLoginBtn = findViewById(R.id.linkToLog);
        mTrackerId = findViewById(R.id.trackerID);
        mTrackerPassword = findViewById(R.id.trackerPassword);

        fAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);

        if(fAuth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
            finish();
        }

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if(getCurrentFocus() != null) {
                    InputMethodManager inputMethodManager = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                }

                email = mEmail.getText().toString().trim();
                password = mPassword.getText().toString().trim();
                fullname = mFullname.getText().toString();
                trackerId = mTrackerId.getText().toString();
                trackerPass = mTrackerPassword.getText().toString();


                if(TextUtils.isEmpty(fullname)){
                    mFullname.setError("Name is Required");
                    return;
                }

                if(TextUtils.isEmpty(email)){
                    mEmail.setError("Email is Required.");
                    return;
                }

                if(TextUtils.isEmpty(password)){
                    mPassword.setError("Password is Required");
                    return;
                }

                if(password.length() < 6){
                    mPassword.setError("Password must be greater than 5 Characters");
                    return;
                }

                if(TextUtils.isEmpty(trackerId)){
                    mTrackerId.setError("Tracker Id is Required");
                    return;
                }

                if(TextUtils.isEmpty(trackerPass)){
                    mTrackerPassword.setError("Tracker password is Required");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tracker/deviceId");
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChild(trackerId)){
                            String status = snapshot.child(trackerId+"/password").getValue(String.class);
                            if(trackerPass.equals(status)){
                                fAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if(task.isSuccessful()){
                                            Toast.makeText(Register.this, "Successfully Created Account.", Toast.LENGTH_SHORT).show();
                                            userID = fAuth.getCurrentUser().getUid();
                                            final DocumentReference documentReference = firestore.collection("users").document(userID);
                                            final Map<String,Object> user = new HashMap<>();
                                            user.put("fname",fullname);
                                            user.put("email",email);
                                            user.put("Default TrackerID",trackerId);
                                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d(TAG, "onSuccess: user profile is created " + userID);
                                                    documentReference.update("Tracker IDs", FieldValue.arrayUnion(trackerId));
                                                }
                                            });
                                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                        }else{
                                            Toast.makeText(Register.this, "Error !" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    }
                                });
                            }
                            else {
                                mTrackerPassword.setError("Tracker password is wrong");
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                        else{
                            mTrackerId.setError("Tracker Id doesn't exist");
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),Login.class));
            }
        });
    }
}