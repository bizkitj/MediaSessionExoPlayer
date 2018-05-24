package com.example.jeason.mediasessionexercise;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

public class MainActivity extends AppCompatActivity implements Player.EventListener, AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static MediaSessionCompat mediaSessionCompat;
    private ExoPlayer mPlayer;
    private PlayerView mPlayerView;
    private Uri mMediaUri;
    private AudioManager mAudioManager;
    private BroadcastReceiver mAudioBecommingNoisy;
    private IntentFilter mAudioBecomingNoisyIntentFilter;
    private PlaybackStateCompat.Builder mStateBuilder;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMediaUri = Uri.parse(getString(R.string.media_url));
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioBecommingNoisy = new AudioBecommingNoisy();
        mAudioBecomingNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        initializeMediaSession();
    }

    private void initializeMediaSession() {
        mediaSessionCompat = new MediaSessionCompat(this, TAG);

        mediaSessionCompat.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSessionCompat.setMediaButtonReceiver(null);

        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_REWIND |
                        PlaybackStateCompat.ACTION_FAST_FORWARD);

        mediaSessionCompat.setPlaybackState(mStateBuilder.build());
        mediaSessionCompat.setCallback(new MediaSessionCallback(this));
        mediaSessionCompat.setActive(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer(mMediaUri);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializePlayer(mMediaUri);
    }

    private void initializePlayer(Uri mediaUri) {
        if (mPlayer == null) {
            mPlayerView = findViewById(R.id.player_view);
            Renderer[] audioRenders = new Renderer[1];
            audioRenders[0] = new MediaCodecAudioRenderer(MediaCodecSelector.DEFAULT);
            TrackSelector audioTrackSelection = new DefaultTrackSelector();
            mPlayer = ExoPlayerFactory.newInstance(audioRenders, audioTrackSelection);
            mPlayerView.setPlayer(mPlayer);
            mPlayer.setPlayWhenReady(true);
            mPlayerView.setControllerAutoShow(true);
            mPlayerView.showController();
            MediaSource mediaSource = buildMediaSource(mediaUri);//local sanskrit
            mPlayer.prepare(mediaSource);
            mPlayer.addListener(this);

        }

    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource(uri,
                new DefaultHttpDataSourceFactory("ua"),
                new DefaultExtractorsFactory(), null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseEverything();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseEverything();
    }

    private void releaseEverything() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer.removeListener(this);
            mPlayer = null;
            mediaSessionCompat.setActive(false);
            notificationManager.cancelAll();
        }
    }

    /**
     * Called when the value returned from either {@link #getPlayWhenReady()} or
     * {@link #getPlaybackState()} changes.
     *
     * @param playWhenReady Whether playback will proceed when ready.
     * @param playbackState One of the {@code STATE} constants.
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                Log.v(TAG, "onPlayerStateChanged " + "STATE_IDLE");
                break;
            case Player.STATE_BUFFERING:
                Log.v(TAG, "onPlayerStateChanged " + "STATE_BUFFERING");
                break;
            case Player.STATE_READY:
                Log.v(TAG, "onPlayerStateChanged " + "STATE_READY");
                break;
            case Player.STATE_ENDED:
                Log.v(TAG, "onPlayerStateChanged " + "STATE_ENDED");
                break;
        }

        if (playbackState == Player.STATE_READY && playWhenReady) {
            int requestAudioFocusReuslt = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (requestAudioFocusReuslt == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.v(TAG, String.valueOf(mPlayer.getPlayWhenReady()));
                this.registerReceiver(mAudioBecommingNoisy, mAudioBecomingNoisyIntentFilter);
                mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, mPlayer.getCurrentPosition(), 1.0f);
                Log.v(TAG, "is Playing");
            }

        } else if (playbackState == Player.STATE_READY) {
            mAudioManager.abandonAudioFocus(this);
            try {
                if (mAudioBecommingNoisy != null) {
                    this.unregisterReceiver(mAudioBecommingNoisy);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, mPlayer.getCurrentPosition(), 1.0f);
            Log.v(TAG, "Paused");
        }
        mediaSessionCompat.setPlaybackState(mStateBuilder.build());
        showNotification(mStateBuilder.build());
    }

    private void showNotification(PlaybackStateCompat playbackStateCompat) {

        int icon;
        int iconRewind = R.drawable.exo_controls_rewind;
        int iconFastForward = R.drawable.exo_controls_fastforward;
        String playPause;
        switch (playbackStateCompat.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                icon = R.drawable.exo_controls_pause;
                playPause = "Playing";
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                icon = R.drawable.exo_controls_play;
                playPause = "Paused";
                break;
            default:
                icon = R.drawable.exo_controls_pause;
                playPause = "Playing";
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "ID");

        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(icon, playPause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));

        NotificationCompat.Action rewindAction = new NotificationCompat.Action(iconRewind, "Rewind",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_REWIND));

        NotificationCompat.Action fastForward = new NotificationCompat.Action(iconFastForward, "Forward",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_FAST_FORWARD));

        notificationBuilder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat.getSessionToken())
                .setShowActionsInCompactView(1, 0, 2))
                .setContentText("sb text")
                .setContentTitle("sb Title")
                .addAction(playPauseAction)
                .addAction(rewindAction)
                .addAction(fastForward)
                .setSmallIcon(R.drawable.exo_edit_mode_logo);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }

    @Override
    public void onAudioFocusChange(int audioFocusChanged) {
        switch (audioFocusChanged) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mPlayer.setPlayWhenReady(false);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                mPlayer.setPlayWhenReady(true);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                mPlayer.setPlayWhenReady(false);
                break;
        }
    }


    //<editor-fold desc="Description">

    /**
     * Called when the timeline and/or manifest has been refreshed.
     * <p>
     * Note that if the timeline has changed then a position discontinuity may also have occurred.
     * For example, the current period index may have changed as a result of periods being added or
     * removed from the timeline. This will <em>not</em> be reported via a separate call to
     * {@link #onPositionDiscontinuity()}.
     *
     * @param timeline The latest timeline. Never null, but may be empty.
     * @param manifest The latest manifest. May be null.
     */


    /**
     * Called when the timeline and/or manifest has been refreshed.
     * <p>
     * Note that if the timeline has changed then a position discontinuity may also have occurred.
     * For example, the current period index may have changed as a result of periods being added or
     * removed from the timeline. This will <em>not</em> be reported via a separate call to
     * {@link #onPositionDiscontinuity(int)}.
     *
     * @param timeline The latest timeline. Never null, but may be empty.
     * @param manifest The latest manifest. May be null.
     * @param reason   The {@link TimelineChangeReason} responsible for this timeline change.
     */
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    /**
     * Called when the available or selected tracks change.
     *
     * @param trackGroups     The available tracks. Never null, but may be of length zero.
     * @param trackSelections The track selections for each renderer. Never null and always of
     *                        length {@link #getRendererCount()}, but may contain null elements.
     */
    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    /**
     * Called when the player starts or stops loading the source.
     *
     * @param isLoading Whether the source is currently being loaded.
     */
    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    /**
     * Called when the value of {@link #getRepeatMode()} changes.
     *
     * @param repeatMode The {@link RepeatMode} used for playback.
     */
    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    /**
     * Called when the value of {@link #getShuffleModeEnabled()} changes.
     *
     * @param shuffleModeEnabled Whether shuffling of windows is enabled.
     */
    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    /**
     * Called when an error occurs. The playback state will transition to {@link #STATE_IDLE}
     * immediately after this method is called. The player instance can still be used, and
     * {@link #release()} must still be called on the player should it no longer be required.
     *
     * @param error The error.
     */
    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    /**
     * Called when a position discontinuity occurs without a change to the timeline. A position
     * discontinuity occurs when the current window or period index changes (as a result of playback
     * transitioning from one period in the timeline to the next), or when the playback position
     * jumps within the period currently being played (as a result of a seek being performed, or
     * when the source introduces a discontinuity internally).
     * <p>
     * When a position discontinuity occurs as a result of a change to the timeline this method is
     * <em>not</em> called. {@link #onTimelineChanged(Timeline, Object, int)} is called in this
     * case.
     *
     * @param reason The {@link DiscontinuityReason} responsible for the discontinuity.
     */
    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    /**
     * Called when the current playback parameters change. The playback parameters may change due to
     * a call to {@link #setPlaybackParameters(PlaybackParameters)}, or the player itself may change
     * them (for example, if audio playback switches to passthrough mode, where speed adjustment is
     * no longer possible).
     *
     * @param playbackParameters The playback parameters.
     */
    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    /**
     * Called when all pending seek requests have been processed by the player. This is guaranteed
     * to happen after any necessary changes to the player state were reported to
     * {@link #onPlayerStateChanged(boolean, int)}.
     */
    @Override
    public void onSeekProcessed() {

    }

    //</editor-fold>

    public static class MediaButtonEvent extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
        }
    }

    private class AudioBecommingNoisy extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPlayer.setPlayWhenReady(false);
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private Context mContext;

        public MediaSessionCallback(Context context) {
            this.mContext = context;
        }

        /**
         * Override to handle requests to fast forward.
         */
        @Override
        public void onFastForward() {
            super.onFastForward();
            Log.v(TAG, "onFastForward");
        }

        /**
         * Override to handle requests to rewind.
         */
        @Override
        public void onRewind() {
            super.onRewind();
            Log.v(TAG, "onRewind");
        }

        /**
         * Override to handle requests to begin playback.
         */
        @Override
        public void onPlay() {
            super.onPlay();
            mPlayer.setPlayWhenReady(true);
        }

        /**
         * Override to handle requests to pause playback.
         */
        @Override
        public void onPause() {
            super.onPause();
            mPlayer.setPlayWhenReady(false);
        }

        /**
         * Override to handle requests to stop playback.
         */
        @Override
        public void onStop() {
            super.onStop();
            mPlayer.setPlayWhenReady(false);
        }
    }

}
