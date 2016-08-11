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
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.Calendar;

/**
 * Created by ManasShrestha on 6/7/16.
 */
public class ThreeDimensPhotoView extends SurfaceView implements MediaPlayer.OnPreparedListener,
        SensorEventListener {

    private static final String TAG = "SurfaceSwitch";
    private MediaPlayer mediaPlayer;
    private SurfaceHolder mFirstSurface;
    private Uri mVideoUri;
    private Handler repeatUpdateHandler = new Handler();
    public int mValue;           //increment
    private boolean mAutoIncrement = false;          //for fast foward in real time
    private boolean mAutoDecrement = false;         // for rewind in real time
    private SensorManager mSensorManager;
    private WindowManager mWindowManager;
    long lastUpdate = 0;

    public ThreeDimensPhotoView(Context context) {
        this(context, null, 0);
    }

    public ThreeDimensPhotoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThreeDimensPhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPlayer();
    }


    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float y = event.values[1];

        if (y < -0.1) {
            if (Calendar.getInstance().getTimeInMillis() - lastUpdate > 100) {
                Log.e("# tilt right # ", y + "");
                lastUpdate = Calendar.getInstance().getTimeInMillis();
                mAutoDecrement = true;
                mAutoIncrement = false;
                repeatUpdateHandler.post(new RptUpdater());
            }
        }

        if (y > 0.1) {
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
                mValue += 15; //change this value to control how much to forward
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + mValue);
            } else if (mAutoDecrement) {
                mValue -= 15; //change this value to control how much to rewind
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - mValue);
            }

        }
    }

    private void setFitToFillAspectRatio(MediaPlayer mp, int videoWidth, int videoHeight) {
        if (mp != null) {
            Integer screenWidth = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getWidth();
            Integer screenHeight = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getHeight();
            android.view.ViewGroup.LayoutParams videoParams = getLayoutParams();

            if (videoWidth > videoHeight) {
                videoParams.width = screenWidth;
                videoParams.height = screenWidth * videoHeight / videoWidth;
            } else {
                videoParams.width = screenHeight * videoWidth / videoHeight;
                videoParams.height = screenHeight;
            }


            setLayoutParams(videoParams);
        }
    }


    public void initPlayer() {

        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        this.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                mFirstSurface = surfaceHolder;

                mVideoUri = Uri.parse(Environment.getExternalStorageDirectory() + "/video.mp4");

                mediaPlayer = MediaPlayer.create(getContext().getApplicationContext(),
                        mVideoUri, mFirstSurface);
                mediaPlayer.start();
                mediaPlayer.pause();

                mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        setFitToFillAspectRatio(mp, width, height);
                    }
                });

                Log.e("+++", "video width, height" + mediaPlayer.getVideoWidth() + " " + mediaPlayer.getVideoHeight());
                for (int i = 0; i < 8 * (mediaPlayer.getDuration() / 1000); i++) {
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

                mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        Log.e("seek complete", "seek complete");
//                        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 200);
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
    protected void onAttachedToWindow() {
        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.show();
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Processing");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSensorManager.registerListener(ThreeDimensPhotoView.this,
                        mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                        SensorManager.SENSOR_DELAY_GAME);
                Log.e("regostered", "registered");
                progressDialog.cancel();
            }
        }, 10000);

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        mSensorManager.unregisterListener(this);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDetachedFromWindow();
    }

}
