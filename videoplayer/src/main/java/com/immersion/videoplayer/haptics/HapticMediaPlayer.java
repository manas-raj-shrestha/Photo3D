/*
 *
 * * =======================================================================
 * * Copyright (c) 2014-2015  Immersion Corporation.  All rights reserved.
 * *                     Immersion Corporation Confidential and Proprietary.
 * *
 * * =======================================================================
 * /
 */

package com.immersion.videoplayer.haptics;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.TextUtils;

import com.immersion.hapticmediasdk.HapticContentSDK;
import com.immersion.hapticmediasdk.HapticContentSDKFactory;
import com.immersion.videoplayer.R;
import com.immersion.videoplayer.utils.ResourceUtils;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

/**
 * MediaPlayer class that can be used to control haptic playback as well
 * as audio/video files.
 */
public class HapticMediaPlayer extends MediaPlayer {

    private static int HAPTIC_SYNC_INTERVAL = 1000;

    /**
     * Haptic SDK player instance
     */
    private HapticContentSDK mHapticPlayer;

    /**
     * Handler used to sync haptic player to content
     */
    private Handler mHandler = new Handler();

    private boolean isHaptFilePresent = false;
    private boolean mWasPaused = false;

    /**
     * Used to synchronize the haptic track to the media player at
     * specific intervals.
     */
    private Runnable syncHapticPlayer = new Runnable() {
        @Override
        public void run() {
            try {
                if (isPlaying()) {

                    mHapticPlayer.update(getCurrentPosition());
                    mHandler.postDelayed(syncHapticPlayer,HAPTIC_SYNC_INTERVAL /*todo DataHolder.HAPTIC_SYNC_INTERVAL*/);
                }
            } catch (IllegalStateException e) {
                // calling isPlaying() after completion results in IllegalStateException.
                e.printStackTrace();
            }
        }

    };

    /**
     * Create the haptic player with stored credentials and provided context
     *
     * @param context {@link Context} of the Video Player {@link android.view.SurfaceView}
     */
    private void initHapticPlayer(Context context) {

        String customer = "da210c0debe404074cfa69cb274fe53ce33f61d25b628c4c12c06a5b58953a15";
        String password = "jd@strat123";

        mHapticPlayer = HapticContentSDKFactory.GetNewSDKInstance(
                HapticContentSDK.SDKMODE_MEDIAPLAYBACK,
                context, customer, password, HapticContentSDK.DEFAULT_DNS);
    }

    /**
     * Sets the haptic source to a file path.
     *
     * @param context the Context to use when creating the haptic player
     * @param hapt    the hapt file of the data you want to play
     * @throws IOException if the data source does not exist
     */
    public void setHapticDataSource(Context context, String hapt) throws IOException {
        if (TextUtils.isEmpty(hapt)) {
            isHaptFilePresent = false;
        } else {
            File file = new File(hapt);
            isHaptFilePresent = file.exists();

        }

        initHapticPlayer(context);

        if (isHaptFilePresent) {
            mHapticPlayer.openHaptics(hapt);
        } else {
            isHaptFilePresent = false;
            Timber.d(ResourceUtils.getString(context, R.string.status_haptic_file_fetch_error));
            throw new IOException("setDataSource failed");
        }

    }

    @Override
    public void start() throws IllegalStateException {
        super.start();
        if (!isHaptFilePresent) {
            return;
        }

        if (mWasPaused) {
            mHapticPlayer.resume();
        } else {
            mHapticPlayer.play();
        }

        mHandler.post(syncHapticPlayer);

        mWasPaused = false;
    }

    @Override
    public void stop() throws IllegalStateException {
        super.stop();
        if (!isHaptFilePresent) {
            return;
        }

        mHapticPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        super.pause();
        if (!isHaptFilePresent) {
            return;
        }

        mWasPaused = true;
        mHapticPlayer.pause();
    }

    @Override
    public void seekTo(int mSec) throws IllegalStateException {
        super.seekTo(mSec);
        if (!isHaptFilePresent) {
            return;
        }

        mHapticPlayer.seek(mSec);
    }

    public void releaseAll() {
        release();
        if (mHapticPlayer != null) {
            mHapticPlayer = null;
        }
    }
}
