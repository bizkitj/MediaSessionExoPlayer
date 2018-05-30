package com.example.jeason.mediasessionexercise;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class AudioPlayService extends Service implements AudioManager.OnAudioFocusChangeListener, Player.EventListener {
    private static final String CHANNEL_ID = "This is the channel";
    private static final int NOTIFICATION_ID = 101;
    private static final String TAG = AudioPlayService.class.getSimpleName();
    private Context context;
    private SimpleExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    private MediaSessionConnector mediaSessionConnector;
    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;
    private BroadcastReceiver audioBecomingNoisy;
    private IntentFilter audioBecomingNoisyIntentFilter;

    public AudioPlayService() {
        context = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "MyExoPlayer"));
        ExtractorMediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(getString(R.string.media_url)));
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
        player.addListener(this);
        //refer to https://developer.android.com/guide/topics/media-apps/audio-focus
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioBecomingNoisy = new AudioBecomingNoisy();
        audioBecomingNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(context, CHANNEL_ID, R.string.playBack_channelName, NOTIFICATION_ID,
                new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(context, MainActivity.class);
                        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        return null;
                    }
                });
        playerNotificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
            @Override
            public void onNotificationStarted(int notificationId, Notification notification) {
                startForeground(notificationId, notification);
            }

            @Override
            public void onNotificationCancelled(int notificationId) {
                stopSelf();
            }
        });
        playerNotificationManager.setPlayer(player);
        mediaSession = new MediaSessionCompat(context, TAG);
        mediaSession.setActive(true);
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setPlayer(player, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.release();
        mediaSessionConnector.setPlayer(null, null);
        playerNotificationManager.setPlayer(null);
        player.release();
        player = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                player.setPlayWhenReady(true);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                player.setPlayWhenReady(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                player.setPlayWhenReady(false);
                break;
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        int requestAudioFocus = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (playbackState == Player.STATE_READY && playWhenReady) {
            //To do, request AudioFocus here.
            this.registerReceiver(audioBecomingNoisy, audioBecomingNoisyIntentFilter);
            if (requestAudioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                player.setPlayWhenReady(true);
            }else{
                player.setPlayWhenReady(false);
            }
        } else if (playbackState == Player.STATE_READY) {
            //Abandon audio focus here.
            this.unregisterReceiver(audioBecomingNoisy);
            audioManager.abandonAudioFocus(this);

        }

    }

    private class AudioBecomingNoisy extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {

            if (player.getPlayWhenReady()){
                player.setPlayWhenReady(false);
            }
        }
    }

    //region Description
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }
    //endregion
}
