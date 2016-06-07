package com.droid.manasshrestha.video360;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.Calendar;

public class RewindForward extends Activity implements MediaPlayer.OnPreparedListener,
        SensorEventListener {

    private static final String TAG = "SurfaceSwitch";
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mFirstSurface;
    private Uri mVideoUri;
    private Handler repeatUpdateHandler = new Handler();
    public int mValue;           //increment
    private boolean mAutoIncrement = false;          //for fast foward in real time
    private boolean mAutoDecrement = false;         // for rewind in real time
    private SensorManager mSensorManager;
    private WindowManager mWindowManager;
    long lastUpdate = 0;
    ImageView imageView;

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (y < -0.05) {
            if (Calendar.getInstance().getTimeInMillis() - lastUpdate > 100) {
                Log.e("# tilt right # ", y + "");
                lastUpdate = Calendar.getInstance().getTimeInMillis();
                mAutoDecrement = true;
                mAutoIncrement = false;
                repeatUpdateHandler.post(new RptUpdater());
            }
        }

        if (y > 0.05) {
            if (Calendar.getInstance().getTimeInMillis() - lastUpdate > 100) {
                Log.e("# tilt right # ", y + "");
                lastUpdate = Calendar.getInstance().getTimeInMillis();
                mAutoIncrement = true;
                mAutoDecrement = false;
                repeatUpdateHandler.post(new RptUpdater());
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Not going to use this
    }

    private class RptUpdater implements Runnable {
        public void run() {
            if (mAutoIncrement) {
                mValue += 30; //change this value to control how much to forward
                mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + mValue);
//                repeatUpdateHandler.postDelayed(new RptUpdater(), 50);
            } else if (mAutoDecrement) {
                mValue -= 30; //change this value to control how much to rewind
                mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() - mValue);
//                repeatUpdateHandler.postDelayed(new RptUpdater(), 50);
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rewind_forward);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        SurfaceView first = (SurfaceView) findViewById(R.id.firstSurface);

        first.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "First surface created!");
                mFirstSurface = surfaceHolder;

                if (getIntent().hasExtra("video_name")) {
                    String timeStamp = getIntent().getStringExtra("video_name");
                    mVideoUri = Uri.parse(Environment.getExternalStorageDirectory() + timeStamp);

                    Log.e("file", String.valueOf(timeStamp));
                } else {
                    mVideoUri = Uri.parse(Environment.getExternalStorageDirectory() + "/video.mp4");
                    Log.e("file", "NA");
                }

                mMediaPlayer = MediaPlayer.create(getApplicationContext(),
                        mVideoUri, mFirstSurface);
                mMediaPlayer.start();
                mMediaPlayer.pause();
                Log.e("+++", "total duration " + mMediaPlayer.getDuration());
                for (int i = 0; i < 6 * (mMediaPlayer.getDuration() / 1000); i++) {
                    final int finalI = i;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            if (Calendar.getInstance().getTimeInMillis() - lastUpdate > 100) {
                                lastUpdate = Calendar.getInstance().getTimeInMillis();
                                mAutoIncrement = true;
                                mAutoDecrement = false;
                                repeatUpdateHandler.post(new RptUpdater());
                            }

                        }
                    }, i * 100);
                }

                mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        Log.e("seek complete", "seek complete");
//                        mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + 200);
                    }
                });

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

    @Override
    protected void onStart() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Processing");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSensorManager.registerListener(RewindForward.this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), SensorManager.SENSOR_DELAY_GAME);
                Log.e("regostered", "registered");
                progressDialog.cancel();
            }
        }, 10000);


        super.onStart();
    }

    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

}