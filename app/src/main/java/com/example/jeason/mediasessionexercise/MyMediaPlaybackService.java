package com.example.jeason.mediasessionexercise;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.example.jeason.mediasessionexercise.Player.ThePlayer;

import java.util.List;

public class MyMediaPlaybackService extends MediaBrowserServiceCompat {
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private ThePlayer mPlayback;

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaSession = new MediaSessionCompat(this, "MediaSession");
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mMediaSession.setPlaybackState(mStateBuilder.build());
        mMediaSession.setCallback(new MyMediaSessionCallback());
        setSessionToken(mMediaSession.getSessionToken());

        MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "who?")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "the album")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "track name")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 2000);
        mMediaSession.setMetadata(metaData.build());

    }

    /**
     * Called to get the root information for browsing by a particular client.
     * <p>
     * The implementation should verify that the client package has permission
     * to access browse media information before returning the root id; it
     * should return null if the client is not allowed to access this
     * information.
     * </p>
     *
     * @param clientPackageName The package name of the application which is
     *                          requesting access to browse media.
     * @param clientUid         The uid of the application which is requesting access to
     *                          browse media.
     * @param rootHints         An optional bundle of service-specific arguments to send
     *                          to the media browse service when connecting and retrieving the
     *                          root id for browsing, or null if none. The contents of this
     *                          bundle may affect the information returned when browsing.
     * @return The {@link BrowserRoot} for accessing this app's content or null.
     * @see BrowserRoot#EXTRA_RECENT
     * @see BrowserRoot#EXTRA_OFFLINE
     * @see BrowserRoot#EXTRA_SUGGESTED
     */
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("empty", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaSession.release();
    }

    private class MyMediaSessionCallback extends MediaSessionCompat.Callback  {
        @Override
        public void onPrepare() {
            super.onPrepare();
            mMediaSession.setActive(true);
        }

        @Override
        public void onPlay() {
//                  mPlayback.playFromMedia();
        }


        @Override
        public void onPause() {
//            mPlayback.pause();
        }


        @Override
        public void onStop() {
//            mPlayback.stop();
            mMediaSession.setActive(false);
        }
    }
}
