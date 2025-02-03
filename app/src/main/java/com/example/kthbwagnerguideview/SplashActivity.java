package com.example.kthbwagnerguideview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefs";
    private static final String PREF_SPLASHSCREEN = "splashscreen";
    private static final String PREF_SPLASHSCREENVIDEO = "splashscreenvideo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hämta preferenser
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isSplashEnabled = sharedPreferences.getBoolean(PREF_SPLASHSCREEN, false);
        boolean isSplashVideoEnabled = sharedPreferences.getBoolean(PREF_SPLASHSCREENVIDEO, false);

        if (!isSplashEnabled) {
            // Gå direkt till huvudaktiviteten
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // Stäng splash-aktiviteten
            return;
        }

        // Gör aktiviteten fullskärm

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | // Döljer statusfältet
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | // Döljer navigeringsfältet
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY); // Aktiverar "immersive mode"

        setContentView(R.layout.activity_splash);

        LinearLayout linearLayout = findViewById(R.id.label_map_group);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) linearLayout.getLayoutParams();

        TextView proceedButton = findViewById(R.id.proceed_button);
        //proceedButton.setBackgroundColor(Color.parseColor("#900029ED"));
        // Skapa en GradientDrawable med rundade hörn
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#40FFFFFF")); // Sätt bakgrundsfärgen
        drawable.setCornerRadius(30f); // Sätt radien för de rundade hörnen

        proceedButton.setBackground(drawable);

        proceedButton.setOnClickListener(v -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });


        params.gravity = Gravity.BOTTOM;
        linearLayout.setLayoutParams(params);

        ImageView backgroundimage = findViewById(R.id.overlay_image);

        if (isSplashVideoEnabled) {
            backgroundimage.setImageAlpha(0);

            VideoView videoView = findViewById(R.id.splash_video);

            // Ange videons URI
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro_marinbla);
            videoView.setVideoURI(videoUri);

            // Starta videon automatiskt
            videoView.start();

            // Lyssna på när videon är klar och starta om den
            videoView.setOnCompletionListener(mp -> {
                // Starta videon igen när den är klar
                videoView.start();
            });
        } else {
            backgroundimage.setImageAlpha(255);
            backgroundimage.setImageResource(R.drawable.screen_bg_wagner_empty);
        }


    }

}
