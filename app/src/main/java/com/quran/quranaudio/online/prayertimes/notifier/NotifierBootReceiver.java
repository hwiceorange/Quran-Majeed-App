package com.quran.quranaudio.online.prayertimes.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.prayertimes.di.factory.worker.WorkerProviderFactory;
import com.quran.quranaudio.online.prayertimes.job.WorkCreator;

import java.util.Objects;


/**
 * BroadcastReceiver for handling BOOT_COMPLETED event
 * 
 * ⚠️ CRITICAL FIX: WorkManager must be initialized before use
 * 
 * Problem:
 * System triggers this receiver before Application.onCreate() completes,
 * causing WorkManager to be uninitialized.
 * 
 * Solution:
 * Initialize WorkManager in onReceive() if not already initialized.
 */
public class NotifierBootReceiver extends BroadcastReceiver {

    private static final String TAG = "NotifierBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Objects.equals(action, Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "BOOT_COMPLETED received");
            
            try {
                // ✅ CRITICAL: Ensure WorkManager is initialized before use
                ensureWorkManagerInitialized(context);
                
                // Schedule periodic prayer updater
                WorkCreator.schedulePeriodicPrayerUpdater(context);
                
                Log.d(TAG, "✅ Periodic prayer updater scheduled successfully");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error scheduling prayer updater after boot", e);
                // Don't crash - prayer notifications will be scheduled when app is opened
            }
        }
    }
    
    /**
     * Ensure WorkManager is initialized before use
     * 
     * This is necessary because BroadcastReceiver can be triggered by the system
     * before Application.onCreate() completes, especially after device boot.
     */
    private void ensureWorkManagerInitialized(Context context) {
        try {
            // Try to get WorkManager instance
            WorkManager.getInstance(context);
            Log.d(TAG, "✅ WorkManager already initialized");
        } catch (IllegalStateException e) {
            // WorkManager not initialized yet, initialize it now
            Log.w(TAG, "⚠️ WorkManager not initialized, initializing now...");
            
            try {
                // Get App instance to access WorkerProviderFactory
                App app = (App) context.getApplicationContext();
                WorkerProviderFactory factory = app.appComponent.workerProviderFactory();
                
                Configuration config = new Configuration.Builder()
                        .setWorkerFactory(factory)
                        .setMinimumLoggingLevel(Log.INFO)
                        .build();
                
                WorkManager.initialize(context, config);
                Log.d(TAG, "✅ WorkManager initialized successfully in receiver");
            } catch (Exception initError) {
                Log.e(TAG, "❌ Failed to initialize WorkManager in receiver", initError);
                throw initError;
            }
        }
    }
}
