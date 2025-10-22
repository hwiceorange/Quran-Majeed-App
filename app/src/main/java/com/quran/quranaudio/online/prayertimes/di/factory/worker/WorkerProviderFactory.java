package com.quran.quranaudio.online.prayertimes.di.factory.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;


public class WorkerProviderFactory extends WorkerFactory {
    private static final String TAG = "WorkerProviderFactory";
    
    private final Map<Class<? extends ListenableWorker>, Provider<ChildWorkerFactory>> workersFactories;

    @Inject
    public WorkerProviderFactory(Map<Class<? extends ListenableWorker>, Provider<ChildWorkerFactory>> workersFactories) {
        this.workersFactories = workersFactories;
    }

    @Nullable
    @Override
    public ListenableWorker createWorker(@NonNull Context appContext, @NonNull String workerClassName, @NonNull WorkerParameters workerParameters) {
        try {
            Provider<ChildWorkerFactory> factoryProvider = getWorkerFactoryProviderByKey(workersFactories, workerClassName);
            
            if (factoryProvider == null) {
                Log.w(TAG, "No provider found for worker: " + workerClassName + ", using default factory");
                return null; // Let WorkerFactory use default creation
            }
            
            ChildWorkerFactory factory = factoryProvider.get();
            if (factory == null) {
                Log.w(TAG, "Factory provider returned null for worker: " + workerClassName);
                return null;
            }
            
            return factory.create(appContext, workerParameters);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create worker: " + workerClassName, e);
            return null; // Let WorkerFactory use default creation
        }
    }

    private Provider<ChildWorkerFactory> getWorkerFactoryProviderByKey(Map<Class<? extends ListenableWorker>, Provider<ChildWorkerFactory>> map, String key) {
        for (Map.Entry<Class<? extends ListenableWorker>, Provider<ChildWorkerFactory>> entry : map.entrySet()) {
            if (Objects.equals(key, entry.getKey().getName())) {
                return entry.getValue();
            }
        }
        return null;
    }
}

