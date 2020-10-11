package com.example.childtracking;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashScreen extends AppCompatActivity {

    Animation top,bottom;
    ImageView imageView;
    TextView textView3,textView4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        top = AnimationUtils.loadAnimation(this,R.anim.top_animation);
        bottom = AnimationUtils.loadAnimation(this,R.anim.bottom_animation);

        imageView = findViewById(R.id.imageView);
        textView3 = findViewById(R.id.textView3);
        textView4 = findViewById(R.id.textView4);

        imageView.setAnimation(top);
        textView3.setAnimation(bottom);
        textView4.setAnimation(bottom);

    }
}