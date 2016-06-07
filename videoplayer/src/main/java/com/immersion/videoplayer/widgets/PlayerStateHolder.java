package com.immersion.videoplayer.widgets;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class to hold the VideoPlayerActivity state
 * <ol>
 * <li>PLAYING: The video is playing</li>
 * <li>PAUSED: The video is in pause state</li>
 * <li>POST_PLAYBACK: The video playback completed. Showing post playback screen</li>
 * </ol>
 * Parcelable implementation allows this class to be passed through Intents
 * or be saved on OnSavedInstanceState
 */
public class PlayerStateHolder implements Parcelable {

    private States mState;

    public void set(States currentState) {
        mState = currentState;
    }

    public States get() {
        return mState;
    }

    public enum States {
        PLAYING, PAUSED, MANUALLY_PAUSED, POST_PLAYBACK
    }

    public PlayerStateHolder() {
    }

    private PlayerStateHolder(Parcel in) {
        mState = (States) in.readValue(States.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mState);
    }

    @SuppressWarnings("unused")
    public static final Creator<PlayerStateHolder> CREATOR = new Creator<PlayerStateHolder>() {
        @Override
        public PlayerStateHolder createFromParcel(Parcel in) {
            return new PlayerStateHolder(in);
        }

        @Override
        public PlayerStateHolder[] newArray(int size) {
            return new PlayerStateHolder[size];
        }
    };

}