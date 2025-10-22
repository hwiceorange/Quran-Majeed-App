package com.quran.quranaudio.online.prayertimes.ui.home;

import android.app.Application;
import android.content.Context;
import android.location.Address;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.quran.quranaudio.online.prayertimes.location.address.AddressHelper;
import com.quran.quranaudio.online.prayertimes.location.tracker.LocationHelper;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.timings.TimingServiceFactory;
import com.quran.quranaudio.online.prayertimes.timings.TimingsService;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";
    
    // Default location: Mecca (if no cached location available)
    private static final double DEFAULT_LATITUDE = 21.4225;
    private static final double DEFAULT_LONGITUDE = 39.8262;
    private static final String DEFAULT_CITY = "Makkah";
    private static final String DEFAULT_COUNTRY = "Saudi Arabia";

    private final LocationHelper locationHelper;
    private final AddressHelper addressHelper;
    private final TimingServiceFactory timingServiceFactory;
    private final PreferencesHelper preferencesHelper;

    private final MutableLiveData<DayPrayer> mDayPrayers;
    private final MutableLiveData<String> mErrorMessage;
    private final LocalDate todayDate;
    private CompositeDisposable compositeDisposable;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Inject
    public HomeViewModel(@NonNull Application application,
                         @NonNull LocationHelper locationHelper,
                         @NonNull AddressHelper addressHelper,
                         @NonNull TimingServiceFactory timingServiceFactory,
                         @NonNull PreferencesHelper preferencesHelper
    ) {
        super(application);

        this.locationHelper = locationHelper;
        this.addressHelper = addressHelper;
        this.timingServiceFactory = timingServiceFactory;
        this.preferencesHelper = preferencesHelper;

        todayDate = LocalDate.now();
        mDayPrayers = new MutableLiveData<>();
        mErrorMessage = new MutableLiveData<>();
        setLiveData(application.getApplicationContext());
    }

    public LiveData<DayPrayer> getDayPrayers() {
        return mDayPrayers;
    }

    public LiveData<String> getError() {
        return mErrorMessage;
    }
    
    /**
     * Force refresh location after permission is granted
     * This method should be called when user grants location permission after initial app launch
     */
    public void forceRefreshLocation() {
        Log.d(TAG, "🔄 Force refreshing location after permission grant");
        
        TimingsService timingsService = timingServiceFactory.create(
            preferencesHelper.getCalculationMethod());
        
        // Force update location in background
        tryUpdateLocationInBackground(timingsService);
    }

    @Override
    protected void onCleared() {
        compositeDisposable.dispose();
        super.onCleared();
    }

    private void setLiveData(Context context) {
        TimingsService timingsService = timingServiceFactory.create(preferencesHelper.getCalculationMethod());
        compositeDisposable = new CompositeDisposable();

        // Try to get cached address first for immediate display
        Address cachedAddress = preferencesHelper.getLastKnownAddress();
        
        if (isValidAddress(cachedAddress)) {
            Log.d(TAG, "Loading prayer times from cached location: " + 
                cachedAddress.getLocality() + ", " + cachedAddress.getCountryName());
            
            // Load prayer times immediately from cached location
            loadPrayerTimesFromAddress(cachedAddress, timingsService);
            
            // Asynchronously try to get current location in background to update cache
            tryUpdateLocationInBackground(timingsService);
        } else {
            Log.d(TAG, "No valid cached location, using default location (Mecca)");
            
            // Use default location (Mecca) for immediate display
            Address defaultAddress = createDefaultAddress();
            loadPrayerTimesFromAddress(defaultAddress, timingsService);
            
            // Asynchronously try to get current location to update cache
            tryUpdateLocationInBackground(timingsService);
        }
    }
    
    /**
     * Check if address is valid (has non-zero coordinates)
     */
    private boolean isValidAddress(Address address) {
        if (address == null) {
            return false;
        }
        
        double lat = address.getLatitude();
        double lon = address.getLongitude();
        
        // Check if coordinates are valid (not 0,0)
        return lat != 0.0 || lon != 0.0;
    }
    
    /**
     * Create default address (Mecca) for fallback
     */
    private Address createDefaultAddress() {
        Address address = new Address(Locale.getDefault());
        address.setLatitude(DEFAULT_LATITUDE);
        address.setLongitude(DEFAULT_LONGITUDE);
        address.setLocality(DEFAULT_CITY);
        address.setCountryName(DEFAULT_COUNTRY);
        return address;
    }
    
    /**
     * Load prayer times from a given address
     */
    private void loadPrayerTimesFromAddress(Address address, TimingsService timingsService) {
        compositeDisposable.add(
            Single.just(address)
                .flatMap(addr -> timingsService.getTimingsByCity(todayDate, addr))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<DayPrayer>() {
                    @Override
                    public void onSuccess(@NotNull DayPrayer dayPrayer) {
                        Log.d(TAG, "Prayer times loaded successfully: " + dayPrayer.getCity());
                        mDayPrayers.postValue(dayPrayer);
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.e(TAG, "Error loading prayer times", e);
                        mErrorMessage.postValue(e.getMessage());
                    }
                })
        );
    }
    
    /**
     * Try to update location in background (if permission granted)
     * This will update cached location and refresh prayer times
     */
    private void tryUpdateLocationInBackground(TimingsService timingsService) {
        compositeDisposable.add(
            locationHelper.getLocation()
                .flatMap(addressHelper::getAddressFromLocation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    address -> {
                        Log.d(TAG, "Current location obtained: " + address.getLocality());
                        
                        // Update cached address in preferences
                        preferencesHelper.updateAddressPreferences(address);
                        
                        // Reload prayer times with updated location
                        loadPrayerTimesFromAddress(address, timingsService);
                    },
                    error -> {
                        Log.w(TAG, "Could not get current location (permission may not be granted): " + 
                            error.getMessage());
                        // Don't show error to user - we're already showing cached/default data
                    }
                )
        );
    }
}