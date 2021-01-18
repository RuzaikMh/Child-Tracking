package com.example.childtracking;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;


public class test extends AppCompatActivity {

    EditText username,password;
    String uname,pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        username = findViewById(R.id.txt_userName);
        password = findViewById(R.id.txt_pass);
    }

    public void login(View view){
        uname = username.getText().toString().trim();
        pass = password.getText().toString().trim();

        startActivity(new Intent(getApplicationContext(), test2.class));
    }

}