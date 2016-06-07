package com.droid.manasshrestha.video360;

import android.app.Activity;
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
import android.widget.TextView;
import android.widget.VideoView;

public class Test extends Activity implements SensorEventListener {
    private SensorManager mgr;
    private Sensor gyro;
    VideoView videoView;
    TextView textView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        gyro = mgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
//        videoView = (VideoView) findViewById(R.id.video_view);
        textView = (TextView) findViewById(R.id.tv);

        videoView.setVideoURI(Uri.parse(Environment.getExternalStorageDirectory().toString() + "/video.mp4"));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Log.e("duration", " " + videoView.getDuration());

                videoView.seekTo(videoView.getDuration() / 2);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        videoView.start();
                        videoView.pause();
                    }
                }, 10);


                Log.e("current position", " " + videoView.getCurrentPosition());
            }
        });
    }

    @Override
    protected void onResume() {
        mgr.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mgr.unregisterListener(this, gyro);
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    long lastUpdate;

    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        long curTime = System.currentTimeMillis();


//        if ((curTime - lastUpdate) > 50) {
        if ((y > 0.01)) {
            lastUpdate = curTime;
            int newTime = videoView.getCurrentPosition() + (80);
//            if (newTime > 0 && newTime < videoView.getDuration()) {
            textView.setText(y + "");
            videoView.seekTo(newTime);
            Log.e("updating", "video " + videoView.getCurrentPosition());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    videoView.start();
                    videoView.pause();
                }
            }, 0);

//            }
        }
    }
}