package com.example.jeason.mediasessionexercise;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private PlayerView playerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent foregroundAudioIntent = new Intent(this, AudioPlayService.class);
        Util.startForegroundService(this, foregroundAudioIntent);
    }


}
