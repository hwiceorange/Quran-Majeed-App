package com.quran.quranaudio.online.prayertimes.ui;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.quran.quranaudio.online.prayertimes.ui.home.HomeViewModel;

import javax.inject.Inject;

/**
 * Helper class to preload prayer data at app startup
 * Injected by Dagger to get ViewModelProvider.Factory
 */
public class PrayerDataPreloader {
    private static final String TAG = "PrayerDataPreloader";

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    public PrayerDataPreloader() {
        // Dagger will inject viewModelFactory
    }

    /**
     * Preload prayer data by creating HomeViewModel at Activity scope
     * This triggers background data fetch so data is ready when user opens Home page
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void preloadPrayerData(AppCompatActivity activity) {
        try {
            if (viewModelFactory == null) {
                Log.e(TAG, "ViewModelFactory is null, cannot preload");
                return;
            }

            // Create HomeViewModel at Activity scope - this triggers data loading
            HomeViewModel homeViewModel = new ViewModelProvider(activity, viewModelFactory)
                    .get(HomeViewModel.class);

            Log.d(TAG, "Prayer data preload initiated in background");

            // No need to observe - just creating the ViewModel triggers data fetch
            // Data will be available when FragMain requests it later

        } catch (Exception e) {
            Log.e(TAG, "Error preloading prayer data", e);
            // Non-critical error - app can still function
        }
    }
}

