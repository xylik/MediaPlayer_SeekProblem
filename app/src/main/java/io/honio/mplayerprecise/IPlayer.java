package io.honio.mplayerprecise;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;
import android.view.TextureView;

import net.protyposis.android.mediaplayer.MediaPlayer;

/**
 * Created by Igor on 05/10/16.
 */
public interface IPlayer {

    //region Player listeners
    public interface OnVideoSizeChangeListener {
        /**
         * Listen for this event to update aspect ratio of played video.
         */
        void onVideoSizeChanged(int videoWidth, int videoHeight, int unappliedRotationDegrees, float pixelWidthHeightRatio);
    }

    public interface OnPlayerStartedDrawingListener {
        void onPlayerStartedDrawing();
    }

    public interface OnPlayerStateChangeListener {
        /**
         * Listen for this event to be notified about player state changes.
         */
        void onPlayerStateChanged(boolean playWhenReady, int playbackState);
    }

    public interface OnPlayerErrorListener {
        void onPlayerError(Exception error);
    }

    public interface OnVideoProgressListener {
        void onProgressUpdate(long currentPosition);
    }
    //endregion

    void playFromUri(Uri videoUri, long startPositionMs, long endPositionMs, boolean playInLoop, boolean playWhenReady);

    void playFromUri(Uri videoUri, int videoId, long startPositionMs, long endPositionMs, boolean playInLoop, boolean playWhenReady);

    void prepare(Uri videoUri);

    void setDisplay(TextureView tv);

    void play();

    void pause();

    void stop();

    void seekTo(long msec);

    boolean isPlaying();

    long getCurrentPosition();

    long getMediaDuration();

    void release();

    void setPlayerStateChangeListener(OnPlayerStateChangeListener listener);
    void setPlayerErrorListener(OnPlayerErrorListener listener);
    void setVideoSizeChangeListener(OnVideoSizeChangeListener listener);
    void setPlayerStartedDrawingListener(OnPlayerStartedDrawingListener listener);
    void setProgressListener(OnVideoProgressListener progressLietener);
    void setSeekCompleteListener(MediaPlayer.OnSeekCompleteListener l);
}
