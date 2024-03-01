package com.quran.quranaudio.online.prayertimes.location.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.quran.quranaudio.online.prayertimes.exceptions.LocationException;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.utils.UserPreferencesUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

import static android.content.Context.MODE_PRIVATE;


@Singleton
public class LocationHelper {

    private final Context context;

    @Inject
    public LocationHelper(Context context) {
        this.context = context;
    }

    public Single<Location> getLocation() {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.LOCATION, MODE_PRIVATE);

        final double lastKnownLatitude = UserPreferencesUtils.getDouble(sharedPreferences, PreferencesConstants.LAST_KNOWN_LATITUDE, 0);
        final double lastKnownLongitude = UserPreferencesUtils.getDouble(sharedPreferences, PreferencesConstants.LAST_KNOWN_LONGITUDE, 0);

        GPSTracker gpsTracker = new GPSTracker(context);

        return Single.create(emitter -> {
            if (gpsTracker.canGetLocation()) {
                Location newLocation = gpsTracker.getLocation();

                if (newLocation != null) {
                    Log.i(LocationHelper.class.getName(), "Get location from tracker");

                    emitter.onSuccess(newLocation);
                } else if (lastKnownLatitude != 0.0 && lastKnownLongitude != 0.0) {
                    Log.w(LocationHelper.class.getName(), "Cannot get location from tracker, use last known location");

                    Location lastKnownLocation = getLastKnownLocation(lastKnownLatitude, lastKnownLongitude);
                    emitter.onSuccess(lastKnownLocation);
                } else {
                    emitter.onError(new LocationException(context.getResources().getString(R.string.location_service_unavailable)));
                    Log.e(LocationHelper.class.getName(), "Location not available");
                }
            } else if (lastKnownLatitude != 0.0 && lastKnownLongitude != 0.0) {

                Location lastKnownLocation = getLastKnownLocation(lastKnownLatitude, lastKnownLongitude);
                emitter.onSuccess(lastKnownLocation);

                Log.w(LocationHelper.class.getName(), "Location tracker not available, using last known location");
            } else {
                emitter.onError(new LocationException(context.getResources().getString(R.string.location_service_unavailable)));
                Log.e(LocationHelper.class.getName(), "Location tracker not available");
            }
        });
    }

    @NonNull
    private Location getLastKnownLocation(double lastKnownLatitude, double lastKnownLongitude) {
        Location lastKnownLocation = new Location("");
        lastKnownLocation.setLatitude(lastKnownLatitude);
        lastKnownLocation.setLongitude(lastKnownLongitude);
        return lastKnownLocation;
    }
}
