package com.immersion.videoplayer.widgets;
/**
 * MediaController customization based on
 * http://stackoverflow.com/questions/12482203/how-to-create-custom-ui-for-android-mediacontroller/14323144#14323144
 * which itself is based on the Android Open Source Project.
 * <p/>
 * <p/>
 * CUSTOMIZATIONS INCLUDE:
 * <ol>
 * <li>Fast forward, rewind buttons, previous and fullscreen controls have been removed.
 * <li>Layout View IDs have been modified to correspond to the MediaController layout.
 * <li>hide() now contains a custom fadeout animation.
 * <li>Logic for removed controls have also been deleted, such as updateFullScreen(), doToggleFullscreen(), etc.
 * <li>SeekBar listener interface with an instance field.
 * </ol>
 */

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.immersion.videoplayer.R;
import com.immersion.videoplayer.interfaces.EventHandler;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import timber.log.Timber;


/**
 * A view containing controls for a MediaPlayer. Typically contains the
 * buttons like "Play/Pause", "Home" and a progress
 * slider. It takes care of synchronizing the controls with the state
 * of the MediaPlayer.
 * <p/>
 * The way to use this class is to instantiate it programmatically.
 * The MediaController will create a abc set of controls
 * and put them in a window floating above your application. Specifically,
 * the controls will float above the view specified with setAnchorView().
 * The window will disappear if left idle for three seconds and reappear
 * when the user touches the anchor view.
 * <p/>
 * Functions like show() and hide() have no effect when MediaController
 * is created in an xml layout.
 */

public class MediaController extends FrameLayout implements View.OnClickListener {

    private static final String TAG = MediaController.class.getSimpleName();

    private MediaPlayerController mPlayer;
    private final Context context;

    private ViewGroup mAnchor;
    private View mRoot;
    private boolean showControls;
    private boolean dragging;
    private static final int DEFAULT_TIME_OUT = 2000; //2 seconds for control visibility timeout
    private static final int TEN_SECONDS = 10000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;

    private ImageButton imgPlayPause;
    private ImageView imgHapticButton;
    private TextView txtVideoTitle;
    private RelativeLayout layoutPlayerBarTop;
    private RelativeLayout layoutBackArrow;
    private SeekBar seekBarMediaController;
    private TextView txtEndTime;
    private TextView txtCurrentTime;

    private final Handler mHandler = new MessageHandler(this);
    private String videoTitle;
    private boolean hide;
    private EventHandler eventHandler;
    private int elapsedTime;

    /*Initialize media player*/
    public MediaController(Context context) {
        this(context, null);
    }

    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = null;
        this.context = context;
        eventHandler = (EventHandler) context;
    }

    private void setUpViews(View mRoot) {
        imgPlayPause = (ImageButton) mRoot.findViewById(R.id.img_play_pause);
        txtVideoTitle = (TextView) mRoot.findViewById(R.id.txt_vdo_title);
        layoutPlayerBarTop = (RelativeLayout) mRoot.findViewById(R.id.layout_top_bar);
        seekBarMediaController = (SeekBar) mRoot.findViewById(R.id.seekbar_media_controller);
        txtEndTime = (TextView) mRoot.findViewById(R.id.txt_Vdo_End_time);
        txtCurrentTime = (TextView) mRoot.findViewById(R.id.txt_player_current_time);
        layoutBackArrow = (RelativeLayout) mRoot.findViewById(R.id.layout_back_arrow);
        // TODO: 12/22/2015 add methods to inflate this item nullable if not present
        imgHapticButton = (ImageView) mRoot.findViewById(R.id.img_haptic);

        layoutBackArrow.setOnClickListener(this);
        imgPlayPause.setOnClickListener(this);
        imgHapticButton.setOnClickListener(this);
    }

    @Override
    public void onFinishInflate() {
        if (mRoot != null) {
            setUpViews(mRoot);
            initControllerView();
        }
        super.onFinishInflate();
    }

    //set media player
    public void setMediaPlayer(MediaPlayerController player) {
        mPlayer = player;
        updatePausePlay();
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     *
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(ViewGroup view) {
        mAnchor = view;
        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     *
     * @return The controller view.
     * @since This doesn't work as advertised
     */
    private View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller, null);
        setUpViews(mRoot);
        initControllerView();

        return mRoot;
    }

    /**
     * Set view and set values according to the views
     */
    private void initControllerView() {
        if (imgPlayPause != null) {
            imgPlayPause.requestFocus();
        }

        Drawable seekBarDrawable = ContextCompat.getDrawable(context, R.drawable.seekbar)
                .getConstantState().newDrawable();
        seekBarMediaController.setProgressDrawable(seekBarDrawable);

        seekBarMediaController.setOnSeekBarChangeListener(mSeekListener);
        seekBarMediaController.setMax(1000);

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        txtVideoTitle.setText(videoTitle);
        if (elapsedTime > 0)
            mPlayer.seekTo(elapsedTime);
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 2 seconds of inactivity.
     */
    public void show() {
        show(DEFAULT_TIME_OUT);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seek to any position.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void
    disableUnsupportedButtons() {
        if (mPlayer == null) {
            return;
        }

        try {
            if (imgPlayPause != null && !mPlayer.canPause()) {
                imgPlayPause.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seek to any position., and so we don't disable
            // the buttons.
            Timber.e(ex.getMessage());
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show the controller until hide() is called.
     */
    public void show(int timeout) {
        Log.d("ANIMATION", "@show() ShowControls : " + showControls + " hide :" + hide);

        if (!showControls && mAnchor != null) {
            showControls = true;
            setProgress();
            if (imgPlayPause != null) {
                imgPlayPause.requestFocus();
            }
            disableUnsupportedButtons();

            LayoutParams tlp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );

            //make sure of no duplication
            mAnchor.removeView(this);
            mAnchor.addView(this, tlp);
        }
        updatePausePlay();

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showControls the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        //if set to hide, do not trigger more fade out messages
        // if already set to hide, cancel the hide animation
        if (!hide) {
            mHandler.removeMessages(FADE_OUT);

            Message msg = mHandler.obtainMessage(FADE_OUT);
            if (timeout != 0 && checkRemainingTime()) {
                mHandler.sendMessageDelayed(msg, timeout);
            }
        } else {

            animate().cancel();
        }
    }

    //returns boolean value if controller is shown on view or not
    public boolean isShowing() {
        return showControls;
    }

    /**
     * Remove the controller from the screen.
     */
    private void hide() {
        if (mAnchor == null) {
            return;
        }

        if (showControls) {
            try {
                animate().alpha(0).setDuration(600).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        hide = !hide;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (hide) {
                            mAnchor.removeView(MediaController.this);
                            mHandler.removeMessages(SHOW_PROGRESS);
                            hide = false;
                            showControls = false;
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        //make the controls reappear and cancel the hide operation.
                        super.onAnimationCancel(animation);
                        animate().alpha(1).setDuration(400).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                showControls = true;
                            }
                        }).start();
                        //signals the cancellation
                        hide = false;
                    }
                }).start();


            } catch (IllegalArgumentException ex) {
                Timber.e(ex.getMessage());
            }
        }
    }

    /**
     * function to convert millisecond to second, minute and hour and return the formatted value
     *
     * @param timeMs : millisecond time got from system
     */
    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public void hidePlayPause() {
        if (imgPlayPause != null)
            imgPlayPause.animate().alpha(0).start();
    }

    public void showPlayPause() {
        if (imgPlayPause != null)
            imgPlayPause.animate().alpha(1).start();
    }

    /*
    * sets progress to {@link SeekBar} and
    * sets running time and video total time into the view
    * */
    private int setProgress() {
        if (mPlayer == null || dragging) {
            return 0;
        }

        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (seekBarMediaController != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                seekBarMediaController.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            seekBarMediaController.setSecondaryProgress(percent * 10);
        }

        if (txtEndTime != null) {
            txtEndTime.setText(stringForTime(duration));
        }
        if (txtCurrentTime != null) {
            txtCurrentTime.setText(stringForTime(position));
        }

        return position;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        Timber.d("TrackBallEvent");
        show(DEFAULT_TIME_OUT);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (mPlayer == null) {
            return true;
        }

        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(DEFAULT_TIME_OUT);
                if (imgPlayPause != null) {
                    imgPlayPause.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(DEFAULT_TIME_OUT);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(DEFAULT_TIME_OUT);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(DEFAULT_TIME_OUT);
        return super.dispatchKeyEvent(event);
    }

    /*
    updates play and pause button according to the state of media player
     *i.e video being played or paused
     * according to {@link com.immersion.tactilekeyboard.constants.PlayerStateHolder} drawable is changed programmatically
     * according to state drawable is changed programmatically*/
    private void updatePausePlay() {
        if (mRoot == null || imgPlayPause == null || mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            imgPlayPause.setImageResource(R.drawable.media_pause);
            hideTitleBar();
        } else {
            imgPlayPause.setImageResource(R.drawable.media_play);
            showTitleBar();
        }
    }

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    /**
     * There are two scenarios that can trigger the {@link SeekBar} listener to trigger:
     * <p/>
     * The first is the user using the touchpad to adjust the position of the
     * {@link SeekBar} thumb. In this case onStartTrackingTouch is called followed by
     * a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
     * We're setting the field "mDragging" to true for the duration of the dragging
     * session to avoid jumps in the position in case of ongoing playback.
     * <p/>
     * The second scenario involves the user operating the scroll ball, in this
     * case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
     * we will simply apply the updated position without suspending regular updates.
     */
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        private boolean pausedForSeek = false;

        public void onStartTrackingTouch(SeekBar bar) {
            if (mPlayer == null) {
                return;
            }

            show(3600000);

            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                pausedForSeek = true;
            }

            dragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the {@link SeekBar} and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (mPlayer == null) {
                return;
            }

            if (!fromUser) {
                return;
            }

            long duration = mPlayer.getDuration();
            long newPosition = (duration * progress) / 1000L;
            mPlayer.seekTo((int) newPosition);
            if (txtCurrentTime != null) {
                txtCurrentTime.setText(stringForTime((int) newPosition));
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
            dragging = false;
            setProgress();
            updatePausePlay();

            if (pausedForSeek) {
                mPlayer.start();
                pausedForSeek = false;
            }

            //customized to keep UI visible when paused and before 10 seconds
            if (mPlayer.isPlaying() && checkRemainingTime()) {
                show(DEFAULT_TIME_OUT);
            }

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showControls.
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    /**
     * Checks if more than the given time remains until the video playback is complete.
     *
     * @return True if more than ten seconds remain.
     */
    private boolean checkRemainingTime() {
        int videoTimeRemain = mPlayer.getDuration() - mPlayer.getCurrentPosition();
        return videoTimeRemain > MediaController.TEN_SECONDS;
    }


    /**
     * Enable or Disable play/pause button and {@link SeekBar} according to condition
     *
     * @param enabled passed to enable or disable play/pause button and {@link SeekBar}
     */
    @Override
    public void setEnabled(boolean enabled) {
        if (imgPlayPause != null) {
            imgPlayPause.setEnabled(enabled);
        }

        if (seekBarMediaController != null) {
            seekBarMediaController.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public void onInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(MediaController.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(MediaController.class.getName());
    }

    public void setCurrentTitle(String title) {
        videoTitle = title;
    }

    public void setElapsedTime(int milliSec) {
        elapsedTime = milliSec;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.img_play_pause) {
            doPauseResume();
        } else if (i == R.id.layout_back_arrow) {
            // update elapse time
            eventHandler.updateElapsedTime(mPlayer.getCurrentPosition());
            //implement onBack Press using Interface
            eventHandler.backPressed();
        } else if (i == R.id.img_haptic) {
            // TODO: 12/22/2015 add haptic methods
        }
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<MediaController> mView;
        private SeekBarListener mSeekBarListener;

        MessageHandler(MediaController view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaController view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    view.hide();
                    break;
                case SHOW_PROGRESS:
                    pos = view.setProgress();
                    if (!view.dragging && view.mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1);
                        if (mSeekBarListener != null)
                            mSeekBarListener.onSeek(pos);
                    }
                    break;
            }
        }

        public void setSeekBarListener(SeekBarListener listener) {
            mSeekBarListener = listener;
        }
    }

    /**
     * Set a listener
     *
     * @param listener {@link SeekBarListener} instance.
     */
    public void setSeekBarListener(SeekBarListener listener) {
        if (mHandler != null)
            ((MessageHandler) mHandler).setSeekBarListener(listener);
    }

    public interface SeekBarListener {
        void onSeek(int position);
    }

    /**
     * This function shows the title bar if it is hidden with fade in animation
     */
    private void showTitleBar() {
        if (layoutPlayerBarTop != null && layoutPlayerBarTop.getVisibility() == View.GONE) {
            layoutPlayerBarTop.setVisibility(View.VISIBLE);
            Animation slideOutLeftAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            layoutPlayerBarTop.startAnimation(slideOutLeftAnimation);
        }
    }

    /**
     * This function hides the title bar bar if it is visible with fade out animation
     */
    private void hideTitleBar() {
        if (layoutPlayerBarTop != null && layoutPlayerBarTop.getVisibility() == View.VISIBLE) {
            layoutPlayerBarTop.setVisibility(View.GONE);
            Animation slideOutLeftAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
            layoutPlayerBarTop.startAnimation(slideOutLeftAnimation);
        }
    }
}

