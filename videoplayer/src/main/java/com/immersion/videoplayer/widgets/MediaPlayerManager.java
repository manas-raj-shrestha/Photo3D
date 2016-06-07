package com.immersion.videoplayer.widgets;

import android.animation.LayoutTransition;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.immersion.videoplayer.haptics.HapticMediaPlayer;
import com.immersion.videoplayer.model.Video;
import com.immersion.videoplayer.utils.ResourceUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Basically a helper type of class that handles all base implementation for the MediaPlayer.
 * It consists of most callbacks that are essential for the media player, including the
 * MediaController.
 * <p/>
 * Invoking the constructor fires up the MediaPlayer immediately, therefore, it is preferable
 * to have it instantiated in points such as onViewCreated(). Also, onSaveInstanceState()
 * and onRestoreInstanceState() are recommended to be invoked wherever appropriate. This is to
 * enable the manager to be able to "remember" the duration the MediaPlayer was at.
 * <p/>
 * Apart from the setup, this also allows the user to register callbacks which will be invoked
 * based on the duration and start factor supplied.
 */
public abstract class MediaPlayerManager implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayerController {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private final WeakReference<Context> mContext;

    private Context context;

    private PlayerStateHolder mStateHolder;
    //Leaks memory
    private HapticMediaPlayer mPlayer;

    private MediaController mController;
    private SurfaceView mVideoSurface;
    private FrameLayout mVideoSurfaceContainer;
//    private FrameLayout vdoPlaceHolder;

    /**
     * Remembers (not persistent) current position of the video when player is interrupted.
     */
    private int mCurrentVideoDurationPosition;
    private MediaController.SeekBarListener mSeekBarListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;
    private boolean mPrepared;
    private boolean mKeepPlayPauseHidden;
    private static final int START_POINT = 0;

    /**
     * Constructs the MediaPlayerManager and immediately starts the MediaPlayer.
     *
     * @param videoSurface          The video surface that will host the MediaPlayer.
     * @param videoSurfaceContainer The video surface container for the MediaController anchor.
     * @param stateHolder           stateHolder for the global state information
     */
    public MediaPlayerManager(SurfaceView videoSurface, FrameLayout videoSurfaceContainer, PlayerStateHolder stateHolder) {
        mContext = new WeakReference<>(videoSurface.getContext());
        this.context = videoSurface.getContext();
        mVideoSurface = videoSurface;
//        vdoPlaceHolder = videoPlaceHolder;
        mVideoSurfaceContainer = videoSurfaceContainer;
        mStateHolder = stateHolder;
        initSurfaceAndHolder();


    }

    /**
     * Experimental code, subject to change
     *
     * @param videoPath Path to the video to be played (extension required)
     */
    private void setVideoFile(String videoPath) {
        try {

            mPlayer.setDataSource(videoPath);
        } catch (IOException e) {
            Timber.e(e.getMessage());
        }
    }

    /**
     * Experimental code, subject to change
     *
     * @param haptFilePath Path to the video to be played
     */

    private void setHaptFile(String haptFilePath) {
        try {
            mPlayer.setHapticDataSource(context, haptFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Makes ready the anchor layout for MediaController and also sets it.
     */
    private void initAnchorAndSet() {
        LayoutTransition transition = new LayoutTransition();
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
        mVideoSurfaceContainer.setLayoutTransition(transition);
        mController.setAnchorView(mVideoSurfaceContainer);
    }

    /**
     * Initializes the underlying SurfaceView and its holder.
     */
    private void initSurfaceAndHolder() {
        SurfaceHolder videoHolder = mVideoSurface.getHolder();
        videoHolder.addCallback(this);

    }

    /**
     * Initializes the MediaPlayer.
     */
    private void initPlayer() {
        mPlayer = new HapticMediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(mOnErrorListener);
        mPlayer.setOnCompletionListener(getOnCompletionListener());
    }

    /**
     * Initializes the MediaController component. It is the component behind the UI for the
     * MediaPlayer.
     */
    private void initController() {
        mController = new MediaController(mContext.get());
        mController.setMediaPlayer(this);
        mController.setSeekBarListener(mSeekBarListener);
        mController.setCurrentTitle(getVideo().title);
        mController.setElapsedTime(getVideo().elapsedTime);
        registerPhoneListeners(true);
    }

    /**
     * Media Player start() and pause() state
     */
    @Override
    public void start() {
        if (mPlayer != null) {
            mStateHolder.set(PlayerStateHolder.States.PLAYING);
            if (mController.isShowing()) {
                //for controls to remain visible before 10 seconds of playback completion
                showController(false);
            }
            if (mKeepPlayPauseHidden) {
                hidePlayPause(true);
            }
            mPlayer.start();
        }
    }

    @Override
    public void pause() {
        if (mPlayer != null) {
            mStateHolder.set(PlayerStateHolder.States.MANUALLY_PAUSED);
            mPlayer.pause();
            mController.showPlayPause();
            showController(true);
        }
    }


    /**
     * Media player playback duration
     */
    @Override
    public int getDuration() {
        return mPlayer != null && mController != null ? mPlayer.getDuration() : START_POINT;
    }

    /**
     * MediaPlayer current seek position
     */
    @Override
    public int getCurrentPosition() {
        return mPlayer != null ? mPlayer.getCurrentPosition() : START_POINT;
    }

    /**
     * MediaPlayer seek to position to make video playback start from set position
     */
    @Override
    public void seekTo(int pos) {
        mKeepPlayPauseHidden = false; //reset

        if (mPlayer != null) {
            mPlayer.seekTo(pos);
        }
    }

    /**
     * MediaPlayer pause or playing state
     */
    @Override
    public boolean isPlaying() {
        return mStateHolder.get() == (PlayerStateHolder.States.PLAYING);
    }

    /**
     * MediaPlayer buffer percentage
     */
    @Override
    public int getBufferPercentage() {
        return 0;
    }

    /**
     * MediaPlayer can be paused
     */
    @Override
    public boolean canPause() {
        return true;
    }


    /**
     * @param callback Holds the procedure to be called upon.
     * @param fromEnd  If set to true, duration accounts before end, eg. 5 seconds before playback
     *                 ends.
     * @param duration Determines when the callback is to be run.
     */
    public void registerCallback(Runnable callback, boolean fromEnd, long duration) {
        tasks.add(new ScheduledTask(callback, fromEnd, duration));
    }

    /**
     * Unregister the supplied callback.
     *
     * @param callback Callback to be unregistered.
     */
    private void unregisterCallback(Runnable callback) {
        mHandler.removeCallbacks(callback);
    }

    /**
     * Clear all callbacks ever set.
     */
    public void unregisterAllCallbacks() {
        for (ScheduledTask task : tasks) {
            unregisterCallback(task.getCallback());
        }
    }

    /**
     * Allows to set a {@link android.widget.SeekBar} listener.
     *
     * @param listener The listener to be set.
     */
    public void setSeekBarListener(MediaController.SeekBarListener listener) {
        mSeekBarListener = listener;
        if (mController != null)
            mController.setSeekBarListener(mSeekBarListener);
    }

    /**
     * Set an OnErrorListener that gets called when error occur.
     *
     * @param listener The listener to be set.
     */
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        mOnErrorListener = listener;
        if (mPlayer != null)
            mPlayer.setOnErrorListener(listener);
    }

    /**
     * Interface to perform necessary action on SurfaceView
     * surface changes.
     *
     * When app is moved to the background, the surface gets * destroyed, hence, all the information
     * to be cached is implemented surfaceDestroyed(), also * the MediaPlayer instance is also
     * released. Likewise, surfaceCreated() should handle the (re)initialization of the MediaPlayer
     * instances and alike.
     */

    /**
     * {@inheritDoc }
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mPlayer == null) {
            initPlayer();
            setVideoFile(getVideo().location);
            setHaptFile(getVideo().haptFile);
            mPlayer.setDisplay(holder);
            mPlayer.setScreenOnWhilePlaying(true);
            if (!mPrepared) {
                mPlayer.prepareAsync();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mPrepared) {
            mPlayer.setDisplay(holder);
            if (mStateHolder.get() == PlayerStateHolder.States.PAUSED) {
                start();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCurrentVideoDurationPosition = getCurrentPosition();
        mPlayer.releaseAll();
        mPlayer = null;
        mPrepared = false;
        registerPhoneListeners(false);
        phoneStateListener = null;
        context = null;
    }

    /**
     * Internal pause state that prevent some handlers from running.
     */
    public void performPause() {
        if (mPrepared) {
            if (mStateHolder.get() == PlayerStateHolder.States.PLAYING) {
                mStateHolder.set(PlayerStateHolder.States.PAUSED);
            }
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
            }
        }
    }

    /**
     * During phone call or any other app when required to use Audio
     * pause our playback
     */
    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                pauseMediaPlayerOnly();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                pauseMediaPlayerOnly();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                break;
        }

    }

    /**
     * Method to pause media player called from audio focus change
     * and phone call listeners
     */
    private void pauseMediaPlayerOnly() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
    }

    /**
     * Method to register and unregister phone call state
     *
     * @param register value to listen call state so video can be paused during incoming call
     *                 *** leaks Memory
     */
    private void registerPhoneListeners(boolean register) {
//        TelephonyManager mgr = (TelephonyManager) mContext.get().getSystemService(Context.TELEPHONY_SERVICE);
//        if (mgr != null) {
//            if (register) {
//                mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//            } else {
//                mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
//            }
//        }
    }


    /**
     * Listener on phone call,dial etc needed to pause media playback during phone call
     */
    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                //Incoming call: Pause music
                pauseMediaPlayerOnly();
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                //A call is dialing, active or on hold
                pauseMediaPlayerOnly();
            }

            super.onCallStateChanged(state, incomingNumber);
        }
    };

    /**
     * Prepare listener for {@link MediaPlayer}
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        mPrepared = true;
        /*
        * Fits video to mobile screen according to video proportion
        * calculates height of mobile screen and sets video proportion to set height and width of video being played
        * */
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;
        int screenWidth = ResourceUtils.getScreenWidth(context)/* - ResourceUtils.getDimension(R.dimen.screen_width_offset)*/;
        int screenHeight = ResourceUtils.getScreenHeight(context)/* - ResourceUtils.getDimension(R.dimen.screen_height_offset)*/;
        float screenProportion = (float) screenWidth / (float) screenHeight;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mVideoSurface.getLayoutParams();

        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        lp.gravity = Gravity.CENTER;
        mVideoSurface.setLayoutParams(lp);

        if (mController == null) { //prevent duplicate controllers from stacking up
            initController();
            initAnchorAndSet();
        } else {
            mController.show();
        }

        if (mCurrentVideoDurationPosition > 0) {
            seekTo(mCurrentVideoDurationPosition);
        }

        //video not paused manually, therefore, continue play
        if (mStateHolder.get() == PlayerStateHolder.States.PAUSED || isPlaying()) {
            start();
        }

        //vdoPlaceHolder.setVisibility(View.INVISIBLE);
    }

    /**
     * Checks whether the UI controls are visible.
     *
     * @return Returns true if the UI controls are visible.
     */
    public boolean isControllerShown() {
        return mController != null && mController.isShowing();
    }

    /**
     * Causes the MediaController to show UI controls.
     *
     * @param indefinitely Causes the controller to be shown until hide gets called.
     */
    public void showController(boolean indefinitely) {
        if (mController != null) {
            if (!indefinitely)
                if (isPlaying()) {
                    mController.show();
                    return;
                }

            mController.show(0);
        }
    }

    /**
     * The interface gets called upon whenever the media playback is complete.
     */
    public abstract MediaPlayer.OnCompletionListener getOnCompletionListener();

    public abstract Video getVideo();

    private void hidePlayPause(boolean keepHidden) {
        if (mController != null) {
            mController.hidePlayPause();
        }

        mKeepPlayPauseHidden = keepHidden;
    }
}
