package com.droid.manasshrestha.video360;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PlayerActivity extends Activity implements MediaPlayer.OnPreparedListener {

    private static final int PICK_VIDEO_REQUEST = 1001;
    private static final String TAG = "SurfaceSwitch";
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mFirstSurface;
    private Uri mVideoUri;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + 400);
            Log.e("####", String.valueOf(mMediaPlayer.getCurrentPosition()));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e("####", String.valueOf(mMediaPlayer.getCurrentPosition()));
                }
            }, 10);

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView first = (SurfaceView) findViewById(R.id.firstSurface);
        first.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "First surface created!");
                mFirstSurface = surfaceHolder;
                mVideoUri = Uri.parse(Environment.getExternalStorageDirectory() + "/video.mp4");
                mMediaPlayer = MediaPlayer.create(getApplicationContext(),
                        mVideoUri, mFirstSurface);
                mMediaPlayer.start();
                mMediaPlayer.pause();

                mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition()+200);
                mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        Log.e("seek complete","seek complete");
//                        mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + 200);
                    }
                });

                testSeekOnMainThread();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "First surface destroyed!");
            }
        });


    }

    private void testSeekOnMainThread() {
        for (int i = 0; i < 20; i++)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition()+200);
                }
            }, i*150);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoUri = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.e("prepared", "prepared");
        mMediaPlayer.start();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK) {
//            Log.d(TAG, "Got video " + data.getData());
//            mVideoUri = data.getData();
//
//        }
//    }

//    public void doStartStop(View view) {
//        if (mMediaPlayer == null) {
//            Intent pickVideo = new Intent(Intent.ACTION_PICK);
//            pickVideo.setTypeAndNormalize("video/*");
//            startActivityForResult(pickVideo, PICK_VIDEO_REQUEST);
//        } else {
//            mMediaPlayer.stop();
//            mMediaPlayer.release();
//            mMediaPlayer = null;
//        }
//    }

    public class SeekThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    Thread.sleep(200);
                    handler.sendEmptyMessage(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}