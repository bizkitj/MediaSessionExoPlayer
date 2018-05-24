package com.example.jeason.mediasessionexercise.Player;

import android.content.Context;

import com.google.android.exoplayer2.ExoPlayer;

public class ThePlayer {
    private ExoPlayer mPlayer;
    private Context mContext;

    public ThePlayer(Context mContext) {
        this.mContext = mContext.getApplicationContext();
    }


}
