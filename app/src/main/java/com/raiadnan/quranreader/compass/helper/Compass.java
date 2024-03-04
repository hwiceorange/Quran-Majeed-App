package com.raiadnan.quranreader.compass.helper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Compass implements SensorEventListener {
    private static final String TAG = "Compass";
    private float[] I = new float[9];
    private float[] R = new float[9];
    private float[] aData = new float[3];
    private Sensor asensor;
    private float azimuthFix;
    private CompassListener listener;
    private float[] mData = new float[3];
    private Sensor msensor;
    private SensorManager sensorManager;

    public interface CompassListener {
        void onAccuracyChanged(String str);

        void onNewAzimuth(float f);
    }

    public Compass(Context context) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.asensor = this.sensorManager.getDefaultSensor(1);
        this.msensor = this.sensorManager.getDefaultSensor(2);
    }

    public void start(Context context) {
        this.sensorManager.registerListener(this, this.asensor, 1);
        this.sensorManager.registerListener(this, this.msensor, 1);
        PackageManager packageManager = context.getPackageManager();
        boolean hasSystemFeature = packageManager.hasSystemFeature("android.hardware.sensor.accelerometer");
        boolean hasSystemFeature2 = packageManager.hasSystemFeature("android.hardware.sensor.compass");
        if (!hasSystemFeature || !hasSystemFeature2) {
            this.sensorManager.unregisterListener(this, this.asensor);
            this.sensorManager.unregisterListener(this, this.msensor);
            Log.e(TAG, "Device don't have enough sensors");
            dialogError(context);
        }
    }

    @SuppressLint("ResourceType")
    private void dialogError(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Alert");
        builder.setCancelable(false);
        builder.setIcon(17301543);
        builder.setMessage("Your device doesn't have the required sensors");
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    public void stop() {
        this.sensorManager.unregisterListener(this);
    }

    public void setAzimuthFix(float f) {
        this.azimuthFix = f;
    }

    public void resetAzimuthFix() {
        setAzimuthFix(0.0f);
    }

    public void setListener(CompassListener compassListener) {
        this.listener = compassListener;
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {
            if (sensorEvent.sensor.getType() == 1) {
                this.aData[0] = (this.aData[0] * 0.97f) + (sensorEvent.values[0] * 0.029999971f);
                this.aData[1] = (this.aData[1] * 0.97f) + (sensorEvent.values[1] * 0.029999971f);
                this.aData[2] = (this.aData[2] * 0.97f) + (sensorEvent.values[2] * 0.029999971f);
            }
            if (sensorEvent.sensor.getType() == 2) {
                this.mData[0] = (this.mData[0] * 0.97f) + (sensorEvent.values[0] * 0.029999971f);
                this.mData[1] = (this.mData[1] * 0.97f) + (sensorEvent.values[1] * 0.029999971f);
                this.mData[2] = (this.mData[2] * 0.97f) + (sensorEvent.values[2] * 0.029999971f);
            }
            if (SensorManager.getRotationMatrix(this.R, this.I, this.aData, this.mData)) {
                float[] fArr = new float[3];
                SensorManager.getOrientation(this.R, fArr);
                float degrees = ((((float) Math.toDegrees((double) fArr[0])) + this.azimuthFix) + 360.0f) % 360.0f;
                if (this.listener != null) {
                    this.listener.onNewAzimuth(degrees);
                }
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int i) {
        if (sensor.getType() == 2) {
            switch (i) {
                case 0:
                case 1:
                    CompassListener compassListener = this.listener;
                    if (compassListener != null) {
                        compassListener.onAccuracyChanged("Low");
                        return;
                    }
                    return;
                case 2:
                    CompassListener compassListener2 = this.listener;
                    if (compassListener2 != null) {
                        compassListener2.onAccuracyChanged("Medium");
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }
}
