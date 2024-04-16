package com.quran.quranaudio.online.compass.view;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.quran.quranaudio.online.compass.helper.CompassUtils;
import com.quran.quranaudio.online.compass.helper.LocationMyMaker;
import com.quran.quranaudio.online.compass.helper.LocationSave;
import com.quran.quranaudio.online.R;

public class MyMaker extends LocationMyMaker {
    private Activity context;
    private SensorEventListener mSensorEventListener;
    private SensorManager mSensorManager;
    private GoogleMap map;
    private Marker marker;
    private MarkerOptions myMaker = new MarkerOptions();

    public MyMaker(Activity activity) {
        this.context = activity;
        this.myMaker.flat(true);
        this.myMaker.title("Your Location");
        this.myMaker.position(new LatLng(LocationSave.getLat(), LocationSave.getLon()));
        this.myMaker.anchor(0.5f, 0.5f);
        this.myMaker.icon(CompassUtils.getBitmapFromVectorDrawable(this.context, R.drawable.ic_navigation));
    }

    public void updateRotationMaker(Float f) {
        Marker marker2;
        if (this.map != null && (marker2 = this.marker) != null) {
            marker2.setRotation(f.floatValue());
        }
    }

    public void updateLocation(Location location) {
        GoogleMap googleMap = this.map;
        if (googleMap != null) {
            Marker marker2 = this.marker;
            if (marker2 != null) {
                marker2.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
            } else {
                this.marker = googleMap.addMarker(this.myMaker);
            }
        }
    }

    public void enableSensor(GoogleMap googleMap) {
        this.map = googleMap;
        this.mSensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.mSensorEventListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int i) {
            }

            public void onSensorChanged(SensorEvent sensorEvent) {
                MyMaker.this.updateRotationMaker(Float.valueOf(sensorEvent.values[0]));
            }
        };
        SensorManager sensorManager = this.mSensorManager;
        sensorManager.registerListener(this.mSensorEventListener, sensorManager.getDefaultSensor(3), 1);
    }

    public void removeSensor() {
        this.mSensorManager.unregisterListener(this.mSensorEventListener);
    }
}
