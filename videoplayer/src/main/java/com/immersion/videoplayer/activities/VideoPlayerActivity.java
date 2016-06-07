package com.immersion.videoplayer.activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.immersion.videoplayer.R;
import com.immersion.videoplayer.fragments.VideoPlayerFragment;
import com.immersion.videoplayer.interfaces.EventHandler;
import com.immersion.videoplayer.model.Video;
import com.immersion.videoplayer.utils.ResourceUtils;
import com.immersion.videoplayer.widgets.PlayerStateHolder;

import java.io.File;

import timber.log.Timber;

/**
 * VideoPlayerActivity hosts VideoPlayerFragment which plays the video
 * This activity is responsible for showing Video Screen and is divided into 3 states
 * <ol>
 * <li>Video Playing State</li>
 * <li>Video Pause State</li>
 * <li>Video Play Completed State or Post Playback State</li>
 * </ol>
 */
public class VideoPlayerActivity extends AppCompatActivity implements EventHandler {

    FrameLayout container;

    private VideoPlayerFragment mVideoPlayerFragment;

    //This field can be queried for current Media Player state.
    private PlayerStateHolder mStateHolder = new PlayerStateHolder();
    private static Video video;

    //constants to define if media player has controller or not
    public static final int MEDIA_PLAYER_WITHOUT_CONTROLLER = 0;
    public static final int MEDIA_PLAYER_WITH_CONTROLLER = 1;

    public static final String PLAY_VIDEO_WITHOUT_CONTROLLER = "playVideoWithoutController";
    public static final String VIDEO_PLAYLIST = "playlist";

    //get the media player type
    private int playerType = MEDIA_PLAYER_WITH_CONTROLLER;

    public static void launch(Context context, Video video) {
        if (video == null || video.location == null || !new File(video.location).exists()) {
            Toast.makeText(context, "Invalid Video File", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(VIDEO_PLAYLIST, video);
        intent.putExtra(PLAY_VIDEO_WITHOUT_CONTROLLER, MEDIA_PLAYER_WITH_CONTROLLER);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    /**
     * Check for video player state and start video player fragment to it
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        container = (FrameLayout) findViewById(R.id.container);

        //Setting minimum height for video player
        int screenHeight = ResourceUtils.getScreenHeight(this);
        container.setMinimumHeight(screenHeight);
        /**
         * Need to trigger finish() on back press and video play completed so passing boolean value for it
         * @param true as we need to watch out for events
         * */
//        registerForEvents(true);
        //Getting the video detail from the intent being passed from the VideoListAdapter onclick of listed videos
        Intent vdoDetail = getIntent();

        //Getting the video detail from the intent being passed from the VideoListAdapter onclick of list item

        if (vdoDetail != null) {
            video = getIntent().getParcelableExtra(VIDEO_PLAYLIST);
            playerType = vdoDetail.getIntExtra(PLAY_VIDEO_WITHOUT_CONTROLLER, MEDIA_PLAYER_WITH_CONTROLLER);

            Timber.d("Touch Touch Intent %d", playerType);
        }

        if (savedInstanceState != null) {
            playVideo();
        } else {
            mStateHolder.set(PlayerStateHolder.States.PLAYING);
            playVideo();
        }
    }

    /**
     * Replace with the VideoPlayerFragment.
     * This invokes that the state is playing
     */
    private void playVideo() {

        // Start VideoPlayer Fragment
        // Start app main activity
        mStateHolder.set(PlayerStateHolder.States.PLAYING);
        mVideoPlayerFragment = (VideoPlayerFragment) VideoPlayerFragment.newInstance(video);
        mVideoPlayerFragment.setOnCompletionListener(mOnCompletionListener);
        mVideoPlayerFragment.setStateHolder(mStateHolder);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        //ft.setCustomAnimations(R.animator.fadein, R.animator.fadeout, R.animator.fadein, R.animator.fadeout);
        ft.replace(R.id.container, mVideoPlayerFragment);
        ft.commit();
    }

    /**
     * Media Player OnCompleteListener
     * If the video wasn't manually paused finish activity after completion of video
     */
    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            // If the video wasn't manually paused.
            if (mVideoPlayerFragment != null && !isPlayerPaused()) {
                finish();
                fadeOutTransition();
                // when video complete; start from beginning; update elapseTime of video object
                updateElapsedTime(0);
            }
        }
    };

    /**
     * Method to identify whether media is on paused state
     */
    private boolean isPlayerPaused() {
        return mStateHolder.get() == PlayerStateHolder.States.PAUSED;
    }

    /**
     * get the type of Media player that is going to be shown
     *
     * @return media player type
     */
    public int getPlayerType() {
        return playerType;
    }

    /**
     * Show Controller while touching the parent view,only if video player fragment is alive
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //show controller only when touch lifted
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {

            Timber.d("Touch Touch %d ", playerType);

            if (mVideoPlayerFragment != null) {
                if (playerType == MEDIA_PLAYER_WITH_CONTROLLER) {
                    mVideoPlayerFragment.showController(false);

                }
            }

        }

        return false;
    }

    /**
     * If the back button is pressed exit the activity
     */
    @Override
    public void onBackPressed() {
        finish();
        fadeOutTransition();
        // update elapseTime to current position
        updateElapsedTime(mVideoPlayerFragment.mediaPlayerManager.getCurrentPosition());

        super.onBackPressed();
    }

    /**
     * Listening to back press event called from MediaController back press button
     * in order to
     */
    @Override
    public void backPressed() {
        onBackPressed();
    }

    @Override
    public void updateElapsedTime(int duration) {
        video.elapsedTime = duration;
    }

    /**
     * Return elapsedTime of the video object to other classes.
     *
     * @return int time
     */
    public static int getElapsedTime() {
        return video == null ? 0 : video.elapsedTime;
    }

    /**
     * function to animate view when activity is finished with fade in and fade out animation
     */
    private void fadeOutTransition() {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
