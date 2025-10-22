package com.quran.quranaudio.online.prayertimes.location.tracker;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


public class GPSTracker extends Service implements LocationListener {

    private static final String TAG = "GPSTracker";
    private final Context mContext;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;
    Location location;
    double latitude;
    double longitude;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 100 * 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 15;

    protected LocationManager locationManager;

    public GPSTracker(Context context) {
        this.mContext = context;
        location = getLocation();
    }

    @SuppressLint("MissingPermission")
    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            if (locationManager != null) {
                isGPSEnabled = locationManager
                        .isProviderEnabled(LocationManager.GPS_PROVIDER);
                isNetworkEnabled = locationManager
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }

            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.d(TAG, "No provider enabled");
            } else {
                this.canGetLocation = true;
                // CRITICAL FIX: Use getLastKnownLocation() instead of requestLocationUpdates()
                // getLastKnownLocation() can be safely called from any thread (no Handler needed)
                // requestLocationUpdates() requires Main/Looper thread (crashes on RxJava background threads)
                
                if (isNetworkEnabled) {
                    if (locationManager != null) {
                        try {
                            Log.d(TAG, "Network Enabled - getting last known location");
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                Log.d(TAG, "Got location from NETWORK: " + latitude + ", " + longitude);
                            } else {
                                Log.d(TAG, "Network provider returned null location");
                            }
                        } catch (SecurityException e) {
                            Log.d(TAG, "Cannot get location from NETWORK provider: No permission found!", e);
                        }
                    }
                }
                if (isGPSEnabled && location == null) {
                    if (locationManager != null) {
                        try {
                            Log.d(TAG, "GPS Enabled - getting last known location");
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                Log.d(TAG, "Got location from GPS: " + latitude + ", " + longitude);
                            } else {
                                Log.d(TAG, "GPS provider returned null location");
                            }
                        } catch (SecurityException e) {
                            Log.d(TAG, "Cannot get location from providers : No permission found !", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot get location from providers", e);
        }
        return location;
    }


    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

