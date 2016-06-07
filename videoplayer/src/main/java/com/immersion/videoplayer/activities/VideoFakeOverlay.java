package com.immersion.videoplayer.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.immersion.videoplayer.R;
import com.immersion.videoplayer.model.Video;

import java.io.File;

/**
 * Fake activity which gets loaded first before loading {@link VideoPlayerActivity}<p/>
 * The reason for it is that theme contains windowIsTranslucent set to true<p/>
 * which will take it's underlying activity view at first and thus results
 * in somewhat unpleasant behaviour while opening the activity<p/>
 * So this activity's background will come to rescue.
 * TODO
 * All this mechanism could well be implemented with Single Activity and two fragments
 */
public class VideoFakeOverlay extends AppCompatActivity {

    private static final int DELAY = 1200;
    private Handler handler = new Handler();

    /**
     * Runnable to be executed after DELAY time whose task is to simply finish this activity
     */
    private Runnable videoCallRunnable = new Runnable() {
        @Override
        public void run() {
            finishOverlayActivity();
        }
    };

    public static void launch(Context context, Video video) {
        if (video == null || video.location == null || !new File(video.location).exists()) {
            Toast.makeText(context, R.string.videoplayer_error_message, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, VideoFakeOverlay.class);
        intent.putExtra(VideoPlayerActivity.VIDEO_PLAYLIST, video);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    /**
     * Navigate to {@link VideoPlayerActivity} and call an handler with DELAY
     * to finish the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_fake_overlay);
        VideoPlayerActivity.launch(this, (Video) getIntent().getParcelableExtra(VideoPlayerActivity.VIDEO_PLAYLIST));
        handler.postDelayed(videoCallRunnable, DELAY);
    }

    /**
     * Called by runnable after DELAY time and will simply finish the activity
     */
    private void finishOverlayActivity() {
        this.finish();
    }
}
