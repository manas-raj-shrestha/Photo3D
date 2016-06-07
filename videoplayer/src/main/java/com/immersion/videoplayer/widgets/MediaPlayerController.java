package com.immersion.videoplayer.widgets;

public interface MediaPlayerController {
    void start();

    void pause();

    int getDuration();

    int getCurrentPosition();

    void seekTo(int pos);

    int getBufferPercentage();

    boolean isPlaying();

    boolean canPause();
}
