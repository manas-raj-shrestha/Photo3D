package com.droid.manasshrestha.video360;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Created by ManasShrestha on 5/31/16.
 */
public class GyroscopeActivity extends Activity implements SensorEventListener{
    private static final String TAG = GyroscopeActivity.class.getSimpleName();
    private SensorManager mSensorManager;
    private WindowManager mWindowManager;
    private float[] mAccelGravityData = new float[3];
    private float[] mGeomagneticData = new float[3];
    private float[] mRotationMatrix = new float[16];
    private float[] bufferedAccelGData = new float[3];
    private float[] bufferedMagnetData = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    protected void onStart() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME );
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);

        super.onStart();
    }

    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    private void loadNewSensorData(SensorEvent event) {
        final int type = event.sensor.getType();
        if (type == Sensor.TYPE_ACCELEROMETER) {
            //Smoothing the sensor data a bit
            mAccelGravityData[0]=(mAccelGravityData[0]*2+event.values[0])*0.33334f;
            mAccelGravityData[1]=(mAccelGravityData[1]*2+event.values[1])*0.33334f;
            mAccelGravityData[2]=(mAccelGravityData[2]*2+event.values[2])*0.33334f;
        }
        if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            //Smoothing the sensor data a bit
            mGeomagneticData[0]=(mGeomagneticData[0]*1+event.values[0])*0.5f;
            mGeomagneticData[1]=(mGeomagneticData[1]*1+event.values[1])*0.5f;
            mGeomagneticData[2]=(mGeomagneticData[2]*1+event.values[2])*0.5f;

            float x = mGeomagneticData[0];
            float y = mGeomagneticData[1];
            float z = mGeomagneticData[2];
            double field = Math.sqrt(x*x+y*y+z*z);
            if (field>25 && field<65){
                Log.e(TAG, "loadNewSensorData : wrong magnetic data, need a recalibration field = " + field);
            }
        }
    }

    private void rootMeanSquareBuffer(float[] target, float[] values) {

        final float amplification = 200.0f;
        float buffer = 20.0f;

        target[0] += amplification;
        target[1] += amplification;
        target[2] += amplification;
        values[0] += amplification;
        values[1] += amplification;
        values[2] += amplification;

        target[0] = (float) (Math
                .sqrt((target[0] * target[0] * buffer + values[0] * values[0])
                        / (1 + buffer)));
        target[1] = (float) (Math
                .sqrt((target[1] * target[1] * buffer + values[1] * values[1])
                        / (1 + buffer)));
        target[2] = (float) (Math
                .sqrt((target[2] * target[2] * buffer + values[2] * values[2])
                        / (1 + buffer)));

        target[0] -= amplification;
        target[1] -= amplification;
        target[2] -= amplification;
        values[0] -= amplification;
        values[1] -= amplification;
        values[2] -= amplification;
    }


    /*
     * Tablets have LANDSCAPE as default orientation, so screen rotation is 0 or 180 when the orientation is LANDSCAPE, and smartphones have PORTRAIT.
     * I use the next code to difference between tablets and smartphones:
     */
    public static int getScreenOrientation(Display display){
        int orientation;

        if(display.getWidth()==display.getHeight()){
            orientation = Configuration.ORIENTATION_SQUARE;
        }else{ //if width is less than height than it is portrait
            if(display.getWidth() < display.getHeight()){
                orientation = Configuration.ORIENTATION_PORTRAIT;
            }else{ // if it is not any of the above it will definitly be landscape
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

    private void debugSensorData(SensorEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append("--- SENSOR ---");
        builder.append("\nName: ");
        Sensor sensor = event.sensor;
        builder.append(sensor.getName());
        builder.append("\nType: ");
        builder.append(sensor.getType());
        builder.append("\nVendor: ");
        builder.append(sensor.getVendor());
        builder.append("\nVersion: ");
        builder.append(sensor.getVersion());
        builder.append("\nMaximum Range: ");
        builder.append(sensor.getMaximumRange());
        builder.append("\nPower: ");
        builder.append(sensor.getPower());
        builder.append("\nResolution: ");
        builder.append(sensor.getResolution());

        builder.append("\n\n--- EVENT ---");
        builder.append("\nAccuracy: ");
        builder.append(event.accuracy);
        builder.append("\nTimestamp: ");
        builder.append(event.timestamp);
        builder.append("\nValues:\n");
        for (int i = 0; i < event.values.length; i++) {
            // ...
            builder.append("   [");
            builder.append(i);
            builder.append("] = ");
            builder.append(event.values[i]);
            builder.append("\n");
        }

//        Log.d(TAG, builder.toString());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    /* Sensor Processing/Rotation Matrix
     * Each time a sensor update happens the onSensorChanged method is called.
     * This is where we receive the raw sensor data.
     * First of all we want to take the sensor data from the accelerometer and magnetometer and smooth it out to reduce jitters.
     * From there we can call the getRotationMatrix function with our smoothed accelerometer and magnetometer data.
     * The rotation matrix that this outputs is mapped to have the y axis pointing out the top of the phone, so when the phone is flat on a table facing north, it will read {0,0,0}.
     * We need it to read {0,0,0} when pointing north, but sitting vertical. To achieve this we simply remap the co-ordinates system so the X axis is negative.
     * The following code example shows how this is acheived.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        loadNewSensorData(event);
        int type=event.sensor.getType();

        if (mAccelGravityData != null && mGeomagneticData != null) {

            if ((type==Sensor.TYPE_MAGNETIC_FIELD) || (type==Sensor.TYPE_ACCELEROMETER)) {
                rootMeanSquareBuffer(bufferedAccelGData, mAccelGravityData);
                rootMeanSquareBuffer(bufferedMagnetData, mGeomagneticData);
                if (SensorManager.getRotationMatrix(mRotationMatrix, null, bufferedAccelGData, bufferedMagnetData)){

                    Display display = mWindowManager.getDefaultDisplay();
                    int orientation = getScreenOrientation(display);
                    int rotation = display.getRotation();

                    boolean dontRemapCoordinates  = (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0) ||
                            (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180) ||
                            (orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90) ||
                            (orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);

                    if( !dontRemapCoordinates){
                        SensorManager.remapCoordinateSystem(
                                mRotationMatrix,
                                SensorManager.AXIS_Y,
                                SensorManager.AXIS_MINUS_X,
                                mRotationMatrix);
                    }
                    debugSensorData(event);
                }
            }
        }

    }
}
