package com.example.jeason.mediasessionexercise;

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

public class MediaPlayerAdapter {
    private final Context mContext;
    private ExoPlayer player;
    private int mState;
    private MediaMetadataCompat mCurrentMedia;

    public MediaPlayerAdapter(Context mContext) {
        this.mContext = mContext.getApplicationContext();
    }

    private void initializePlayer() {
//        if (player == null) {
//            player = ExoPlayerFactory.newSimpleInstance(mContext, new DefaultTrackSelector());
//            MediaSource mediaSource = buildMediaSource(Uri.parse(getString(R.string.media_url)));
//            player.prepare(mediaSource);
//            player.setPlayWhenReady(true);
//        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource(uri,
                new DefaultHttpDataSourceFactory("ua"),
                new DefaultExtractorsFactory(), null, null);
    }
}
