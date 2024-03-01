package com.quran.quranaudio.online.prayertimes.di.module;

import com.quran.quranaudio.online.prayertimes.di.factory.worker.ChildWorkerFactory;
import com.quran.quranaudio.online.prayertimes.di.factory.worker.WorkerKey;
import com.quran.quranaudio.online.prayertimes.job.InstantPrayerScheduler;
import com.quran.quranaudio.online.prayertimes.job.PrayerUpdater;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public interface WorkerBindingModule {
    @Binds
    @IntoMap
    @WorkerKey(PrayerUpdater.class)
    ChildWorkerFactory bindPrayerUpdaterWorker(PrayerUpdater.Factory factory);

    @Binds
    @IntoMap
    @WorkerKey(InstantPrayerScheduler.class)
    ChildWorkerFactory bindInstantPrayerSchedulerWorker(InstantPrayerScheduler.Factory factory);
}
