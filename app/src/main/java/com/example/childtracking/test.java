package com.example.childtracking;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class test extends AppCompatActivity {

    Button log,register;
    String str_log, str_register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        log = findViewById(R.id.log_btn);
        register = findViewById(R.id.btnRegister);

        str_log = log.getText().toString();
        str_register = register.getText().toString();


        log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), trackingHistroy.class));
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent send = new Intent(getApplicationContext(),AddTracker.class);
                send.putExtra("str_log", str_log);
                send.putExtra("str_register", str_register);
                startActivity(send);
            }
        });


    }



}