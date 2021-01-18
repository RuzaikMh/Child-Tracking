package com.example.childtracking;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class test2 extends AppCompatActivity {

    TextView txt1,txt2,txt3;
    String value1,value2,value3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test2);

        txt1 = findViewById(R.id.Result1);
        txt2 = findViewById(R.id.Result2);
        txt3 = findViewById(R.id.Result3);



        txt1.setText(value1);
        txt2.setText(value2);
        txt3.setText(value3);
    }
}