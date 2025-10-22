package com.quran.quranaudio.online.prayertimes.location.address;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.quran.quranaudio.online.prayertimes.exceptions.LocationException;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.location.osm.NominatimAPIService;
import com.quran.quranaudio.online.prayertimes.location.osm.NominatimReverseGeocodeResponse;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;


@Singleton
public class AddressHelper {

    private static final int MINIMUM_DISTANCE_FOR_OBSOLESCENCE = 1000; //1KM

    private final Context context;
    private final NominatimAPIService nominatimAPIService;
    private final PreferencesHelper preferencesHelper;

    @Inject
    public AddressHelper(Context context, NominatimAPIService nominatimAPIService, PreferencesHelper preferencesHelper) {
        this.context = context;
        this.nominatimAPIService = nominatimAPIService;
        this.preferencesHelper = preferencesHelper;
    }

    public Single<Address> getAddressFromLocation(final Location location) {

        return Single.create(emitter -> {
            boolean locationSetManually = preferencesHelper.isLocationSetManually();
            if (locationSetManually) {
                Address lastKnownAddress = preferencesHelper.getLastKnownAddress();
                emitter.onSuccess(lastKnownAddress);
            } else if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                Address lastKnownAddress = preferencesHelper.getLastKnownAddress();

                // CRITICAL FIX: Check if cached address is valid and not obsolete
                if (lastKnownAddress != null && !isAddressObsolete(lastKnownAddress, latitude, longitude)) {
                    Log.d("ADDRESS_HELPER", "Using cached address: " + lastKnownAddress.getLocality());
                    emitter.onSuccess(lastKnownAddress);
                } else {
                    Log.d("ADDRESS_HELPER", "Cached address obsolete or null, fetching new address");

                    // CRITICAL FIX: Geocoding must be done in background thread, but ensure proper callback
                    Thread thread = new Thread(() -> {
                        Log.d("ADDRESS_HELPER", "Starting address lookup for: " + latitude + ", " + longitude);
                        
                        // Try Geocoder first (works in most countries, but may fail in China)
                        Address geocoderAddress = getGeocoderAddresses(latitude, longitude, context);
                        if (geocoderAddress != null) {
                            Log.i(AddressHelper.class.getName(), "✅ Got address from Geocoder: " + geocoderAddress.getLocality());
                            emitter.onSuccess(geocoderAddress);
                            return;
                        }
                        
                        Log.d("ADDRESS_HELPER", "Geocoder failed, trying Nominatim...");
                        
                        // Try Nominatim API as fallback (requires internet)
                        Address nominatimAddress = getNominatimAddress(latitude, longitude);
                        if (nominatimAddress != null) {
                            Log.i(AddressHelper.class.getName(), "✅ Got address from Nominatim: " + nominatimAddress.getLocality());
                            emitter.onSuccess(nominatimAddress);
                            return;
                        }
                        
                        Log.d("ADDRESS_HELPER", "Nominatim failed, using offline address");
                        
                        // Fallback: offline address (coordinates only)
                        Address offlineAddress = getOfflineAddress(latitude, longitude);
                        Log.i(AddressHelper.class.getName(), "✅ Using offline address: " + offlineAddress.getLatitude() + ", " + offlineAddress.getLongitude());
                        emitter.onSuccess(offlineAddress);
                    });
                    thread.start();
                }
            } else {
                Log.e(AddressHelper.class.getName(), "Location is null");
                emitter.onError(new LocationException(context.getResources().getString(R.string.location_service_unavailable)));
            }
        });
    }

    private Address getGeocoderAddresses(double latitude, double longitude, Context context) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);

            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);

                if (address.getCountryName() != null && address.getLocality() != null) {
                    preferencesHelper.updateAddressPreferences(address);
                    return address;
                }
                return null;
            }
            return null;
        } catch (IOException e) {
            Log.e("ADDRESS_HELPER", "Cannot get address from Geocoder", e);
            return null;
        }
    }

    private Address getNominatimAddress(double latitude, double longitude) {
        try {
            NominatimReverseGeocodeResponse response = nominatimAPIService.getAddressFromLocation(latitude, longitude);

            if (response != null && response.getAddress() != null && response.getAddress().getCountry() != null && response.getAddress().getLocality() != null) {
                Address address = new Address(Locale.getDefault());
                address.setCountryName(response.getAddress().getCountry());
                address.setCountryCode(response.getAddress().getCountryCode());
                address.setAddressLine(1, response.getAddress().getState());
                address.setLocality(response.getAddress().getLocality());
                address.setPostalCode(response.getAddress().getPostcode());
                address.setLatitude(response.getLat());
                address.setLongitude(response.getLon());

                preferencesHelper.updateAddressPreferences(address);

                return address;
            }

            return null;
        } catch (IOException e) {
            Log.e("ADDRESS_HELPER", "Cannot get address from Nominatim", e);
            return null;
        }
    }

    private Address getOfflineAddress(double latitude, double longitude) {
        Address address = new Address(Locale.getDefault());
        address.setLatitude(latitude);
        address.setLongitude(longitude);
        
        // CRITICAL FIX: Set locality to coordinates for display (instead of showing "Offline")
        // Format: "40.05°N, 116.26°E" for better user experience
        String latDirection = latitude >= 0 ? "N" : "S";
        String lonDirection = longitude >= 0 ? "E" : "W";
        String coordinatesText = String.format(Locale.US, "%.2f°%s, %.2f°%s", 
            Math.abs(latitude), latDirection, 
            Math.abs(longitude), lonDirection);
        address.setLocality(coordinatesText);
        
        Log.d("ADDRESS_HELPER", "Created offline address with coordinates: " + coordinatesText);

        preferencesHelper.updateAddressPreferences(address);

        return address;
    }

    private boolean isAddressObsolete(Address lastKnownAddress, double latitude, double longitude) {
        // CRITICAL FIX: Always check for null first
        if (lastKnownAddress == null) {
            return true;  // Null address is obsolete
        }
        
        if (lastKnownAddress.getLocality() != null && lastKnownAddress.getCountryName() != null) {

            Location LastKnownLocation = new Location("");
            LastKnownLocation.setLatitude(lastKnownAddress.getLatitude());
            LastKnownLocation.setLongitude(lastKnownAddress.getLongitude());

            Location newLocation = new Location("");
            newLocation.setLatitude(latitude);
            newLocation.setLongitude(longitude);

            float distance = LastKnownLocation.distanceTo(newLocation);
            return distance > MINIMUM_DISTANCE_FOR_OBSOLESCENCE;
        }
        return true;
    }
}
