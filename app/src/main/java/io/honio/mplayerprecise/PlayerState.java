package io.honio.mplayerprecise;

/**
 * Created by Igor on 24/10/16.
 */
public interface PlayerState {
    //region Player state constants
    /**
     * The player is neither prepared or being prepared.
     */
    int STATE_IDLE = 1;
    /**
     * The player is being prepared.
     */
    int STATE_PREPARING = 2;
    /**
     * The player is prepared but not able to immediately play from the current position. The cause
     * is {TrackRenderer} specific, but this state typically occurs when more data needs
     * to be buffered for playback to start.
     */
    int STATE_BUFFERING = 3;
    /**
     * The player is prepared and able to immediately play from the current position. The player will
     * be playing if {getPlayWhenReady()} returns true, and paused otherwise.
     */
    int STATE_READY = 4;
    /**
     * The player has finished playing the media.
     */
    int STATE_ENDED = 5;
    //endregion
}
