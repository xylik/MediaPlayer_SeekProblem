package io.honio.mplayerprecise;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
//import android.media.MediaPlayer;
import net.protyposis.android.mediaplayer.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Igor on 05/10/16.
 */
public class MediaPlayerImp implements IPlayer {
    public static final String TAG = MediaPlayerImp.class.getSimpleName();

    private OnVideoSizeChangeListener mVideoSizeChangeListener;
    private OnPlayerStartedDrawingListener mPlayerStartedDrawingListener;
    private OnPlayerStateChangeListener mPlayerStateChangeListener;
    private OnPlayerErrorListener mOnErrorListener;
    private OnVideoProgressListener mVideoProgressListener;
    private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener;

    private Uri mUri;
    private Map<String, String> mHeaders;
    private static long PROGRESS_UPDATE_INTERVAL_MS = 20;

    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private Context mContext = null;
    private TextureView mDrawingView;
    private SurfaceTexture mDrawingSurface = null;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mCurrentBufferPercentage;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;
    private int mSeekWhenPrepared = 0;  // recording the seek position while preparing
    private boolean playWhenReady = false;
    private boolean isPlayerReleased = true;
    private VideoPlayParameters videoPlayParameters;

    public static class MediaPlayerImpException extends RuntimeException {
        public MediaPlayerImpException() {
        }
        public MediaPlayerImpException(String detailMessage) {
            super(detailMessage);
        }
        public MediaPlayerImpException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
        public MediaPlayerImpException(Throwable throwable) {
            super(throwable);
        }
    }

    public static IPlayer instance(Context ctx, TextureView tv) {
        return new MediaPlayerImp(ctx, tv);
    }

    private MediaPlayerImp(Context ctx, TextureView tv) {
        this.mContext = ctx;
        mDrawingView = tv;
    }

    @Override
    public void playFromUri(Uri videoUri, long startPositionMs, long endPositionMs, boolean playInLoop, boolean playWhenReady) {
        throw new MediaPlayerImpException("Not expected call, stream playing not allowed with MediaPlayer!");
    }

    @Override
    public void playFromUri(Uri videoUri, int videoId, long startPositionMs, long endPositionMs, boolean playInLoop, boolean playWhenReady) {
        videoPlayParameters = new VideoPlayParameters(videoUri, startPositionMs, endPositionMs, playInLoop, playWhenReady);
        mHeaders = new HashMap<>();
        this.playWhenReady = playWhenReady;
        mUri = videoUri;

        seekTo(startPositionMs);
        openVideo();
        if(playWhenReady) play();
    }

    @Override
    public void prepare(Uri videoUri) {
        initVideoView();
        mUri = videoUri;
        mHeaders = new HashMap<>();
        mUri = videoUri;
        openVideo();
    }

    private void initVideoView() {
        if(mDrawingView.isAvailable()) {
            mDrawingSurface = mDrawingView.getSurfaceTexture();
            mSurfaceWidth = mDrawingView.getWidth();
            mSurfaceHeight = mDrawingView.getHeight();
        }

        if(mDrawingSurface != null) {
            mVideoWidth = mSurfaceWidth;
            mVideoHeight = mSurfaceHeight;
        }else {
            mVideoWidth = 0;
            mVideoHeight = 0;
        }
        mDrawingView.setSurfaceTextureListener(mSurfaceTextureListener);
//        mDrawingView.setFocusable(true);
//        mDrawingView.setFocusableInTouchMode(true);
//        mDrawingView.requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
    }

    private void openVideo() {
        if (mUri == null || mDrawingSurface == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // we shouldn't clear the target state, because somebody might have
        // called play() previously
        release();
        try {
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
            mMediaPlayer.setSurface(new Surface(mDrawingSurface));
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }
//    private void startTimer() {
//        timerSubscription = Observable.interval(PROGRESS_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .onBackpressureDrop()
//                .subscribe(
//                        aLong -> {
//                            if(isPlayerReleased) return;
//                            if(videoPlayParameters.playInLoop && getCurrentPosition() >= videoPlayParameters.endPositionMs) {
//                                revindVideo(videoPlayParameters);
//                                return;
//                            }
//                            if(mVideoProgressListener != null) mVideoProgressListener.onProgressUpdate(getCurrentPosition());
//                        },
//                        error ->  {
//                            Log.e(TAG, Log.getStackTraceString(error));
//                        }
//                );
//    }

    private void revindVideo(VideoPlayParameters videoPlayParameters) {
        pause();
        seekTo(videoPlayParameters.startPositionMs);
        play();
    }

    @Override
    public void setDisplay(TextureView tv) {
        mDrawingView = tv;
    }

    @Override
    public void play() {
        if (isInPlaybackState()) {
            mMediaPlayer.setPlaybackSpeed(0.1f);
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            if(mPlayerStateChangeListener != null) mPlayerStateChangeListener.onPlayerStateChanged(playWhenReady, PlayerState.STATE_READY);
            isPlayerReleased = false;
//            startTimer();
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
//        if(timerSubscription != null && !timerSubscription.isUnsubscribed()) timerSubscription.unsubscribe();
        mDrawingView.setSurfaceTextureListener(null);
        mDrawingView = null;
        mDrawingSurface = null;
        isPlayerReleased = true;
    }

    @Override
    public void seekTo(long msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo((int)msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = (int)msec;
        }
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public long getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }

        return 0;
    }

    @Override
    public long getMediaDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public void release() {
        isPlayerReleased = true;
        if(mDrawingView != null && mCurrentState != STATE_IDLE) {
            mDrawingView.setSurfaceTextureListener(null);
            mDrawingView = null;
            mDrawingSurface = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
//        if(timerSubscription != null && !timerSubscription.isUnsubscribed()) timerSubscription.unsubscribe();
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    //region Player state listener setters
    public void setPlayerStateChangeListener(OnPlayerStateChangeListener l) {
        this.mPlayerStateChangeListener = l;
    }

    @Override
    public void setPlayerErrorListener(OnPlayerErrorListener l) {
        this.mOnErrorListener = l;
    }

    public void setVideoSizeChangeListener(OnVideoSizeChangeListener l) {
        this.mVideoSizeChangeListener = l;
    }

    public void setPlayerStartedDrawingListener(OnPlayerStartedDrawingListener l) {
        this.mPlayerStartedDrawingListener = l;
    }

    @Override
    public void setProgressListener(OnVideoProgressListener l) {
        this.mVideoProgressListener = l;
    }

    @Override
    public void setSeekCompleteListener(MediaPlayer.OnSeekCompleteListener l){
        this.mSeekCompleteListener = l;
    }
    //endregion

    MediaPlayer.OnPreparedListener mPreparedListener =
        new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    mCurrentState = STATE_PREPARED;
                    if (mPlayerStateChangeListener != null) {
                        mPlayerStateChangeListener.onPlayerStateChanged(playWhenReady, PlayerState.STATE_BUFFERING);
                    }

                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();

                    int seekToPosition = mSeekWhenPrepared;  //mSeekWhenPrepared may be changed after seekTo() call
                    if (seekToPosition != 0) {
                        seekTo(seekToPosition);
                    }

                    if (mTargetState == STATE_PLAYING) {
                        play();
                    }
                }
        };

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
        new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();
                if(mVideoSizeChangeListener != null) mVideoSizeChangeListener.onVideoSizeChanged(mVideoWidth, mVideoHeight, -1, -1);
            }
        };

    private MediaPlayer.OnCompletionListener mCompletionListener =
        new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mCurrentState = STATE_PLAYBACK_COMPLETED;
                mTargetState = STATE_PLAYBACK_COMPLETED;
            }
        };

    private MediaPlayer.OnErrorListener mErrorListener =
        new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
                Log.d(TAG, "Error: " + framework_err + "," + impl_err);
                mCurrentState = STATE_ERROR;
                mTargetState = STATE_ERROR;

                /* If an error handler has been supplied, use it and finish. */
                if (mOnErrorListener != null) mOnErrorListener.onPlayerError(new MediaPlayerImpException("Error: " + framework_err + "," + impl_err));
                return true;
            }
        };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
        new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mCurrentBufferPercentage = percent;
            }
        };

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int w, int h) {
            mDrawingSurface = surface;
            openVideo();

            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
//            boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
//            if (mMediaPlayer != null && isValidState && hasValidSize) {
            if (mMediaPlayer != null && isValidState) {
                if (mSeekWhenPrepared != 0) seekTo(mSeekWhenPrepared);
                play();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int w, int h) {
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
//            boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
//            if (mMediaPlayer != null && isValidState && hasValidSize) {
            if(mMediaPlayer != null && isValidState) {
                if (mSeekWhenPrepared != 0) seekTo(mSeekWhenPrepared);
                play();
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mDrawingSurface = null;
            release();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //empty
        }
    };

    private class VideoPlayParameters {
        public Uri videoUri;
        public long startPositionMs;
        public long endPositionMs;
        public boolean playInLoop;
        public boolean playWhenReady;

        public VideoPlayParameters(Uri videoUri, long startPositionMs, long endPositionMs, boolean playInLoop, boolean playWhenReady) {
            this.videoUri = videoUri;
            this.startPositionMs = startPositionMs;
            this.endPositionMs = endPositionMs;
            this.playInLoop = playInLoop;
            this.playWhenReady = playWhenReady;
        }
    }
}
