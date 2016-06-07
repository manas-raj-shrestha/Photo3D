package com.immersion.videoplayer.fragments;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.immersion.videoplayer.R;
import com.immersion.videoplayer.model.Video;
import com.immersion.videoplayer.widgets.MediaController;
import com.immersion.videoplayer.widgets.MediaPlayerManager;
import com.immersion.videoplayer.widgets.PlayerStateHolder;

import timber.log.Timber;

/**
 * Fragment
 * <ul>
 * <li>responsible for playing videos</li>
 * </ul>
 */
public class VideoPlayerFragment extends Fragment {

    private static final String TAG = VideoPlayerFragment.class.getSimpleName();
    private static final int ANIMATION_ALPHA_DURATION = 1200;

    SurfaceView vdoSurface;

    FrameLayout vdoSurfaceContainer;

    FrameLayout vdoPlaceHolder;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    public MediaPlayerManager mediaPlayerManager;
    // Object to hold the state of playback
    private PlayerStateHolder mStateHolder;

    private static Video videoDetail;


    public VideoPlayerFragment() {
    }

    /**
     * Creates new fragment
     * <p/>
     * //     * @param videoDetail : is a model and data is fetched by getter and setter method
     */
    public static Fragment newInstance(Video videoDetail) {
        Fragment f = new VideoPlayerFragment();
        VideoPlayerFragment.videoDetail = videoDetail;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videoplayer, container, false);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        //Pause media player if app is on pause state
        mediaPlayerManager.performPause();
    }

    /**
     * Initializing views, controller and fileDataSource for selected videos detail
     */
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setUpViews(view);

        ObjectAnimator.ofFloat(vdoPlaceHolder, "alpha", 1f, 0f).setDuration(ANIMATION_ALPHA_DURATION).start();
        //fileDataSource = new FileDataSource();
        initMediaPlayerManager();

        //show the controller for the first time
        mediaPlayerManager.registerCallback(new Runnable() {
            @Override
            public void run() {
                mediaPlayerManager.showController(false);
            }
        }, false, 1000);

        mediaPlayerManager.setSeekBarListener(new MediaController.SeekBarListener() {
            @Override
            public void onSeek(int position) {
                Timber.d("SeekBar Position: %d", position);
            }
        });

        mediaPlayerManager.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Timber.d("Player error : %s", "An error has been encountered.");
                return false;
            }
        });
    }

    private void setUpViews(View view) {
        vdoSurface = (SurfaceView) view.findViewById(R.id.videoSurface);
        vdoSurfaceContainer = (FrameLayout) view.findViewById(R.id.videoSurfaceContainer);
        vdoPlaceHolder = (FrameLayout) view.findViewById(R.id.placeholder);
    }

    /**
     * MediaPlayer completeListener i.e. after media playback has been completed
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        this.mOnCompletionListener = onCompletionListener;
    }


    /**
     * Setter to set the state of video playback
     *
     * @param stateHolder {@link PlayerStateHolder} to know state of video i.e playing , paused
     */
    public void setStateHolder(PlayerStateHolder stateHolder) {
        mStateHolder = stateHolder;
    }

    /**
     * Causes the MediaController to show UI controls.
     *
     * @param indefinitely Causes the controller to be shown until hide gets called.
     */
    public void showController(boolean indefinitely) {
        mediaPlayerManager.showController(indefinitely);
    }

    /**
     * @return boolean : to indicate whether the header/footer view or controller view is shown or not
     */
    public boolean isControllerShown() {
        return mediaPlayerManager.isControllerShown();
    }

    /**
     * Method to initialize MediaPlayerManager with
     * <ul>
     * <li>mViewSurfaceView:SurfaceView</li> {@link SurfaceView}
     * <li>mVideoSurfaceContainer:FrameLayout</li> {@link FrameLayout}
     * <li>mStateHolder:PlayerStateHolder</li> {@link PlayerStateHolder}
     * </ul>
     * along with setting title of current track and next track
     */
    private void initMediaPlayerManager() {


        mediaPlayerManager = new MediaPlayerManager(vdoSurface, vdoSurfaceContainer, mStateHolder) {
            @Override
            public MediaPlayer.OnCompletionListener getOnCompletionListener() {
                return mOnCompletionListener;
            }

            @Override
            public Video getVideo() {
                return videoDetail;
            }
        };
    }
}
