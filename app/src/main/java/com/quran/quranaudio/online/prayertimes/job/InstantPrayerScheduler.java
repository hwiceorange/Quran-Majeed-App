package com.quran.quranaudio.online.prayertimes.job;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.google.gson.Gson;
import com.quran.quranaudio.online.prayertimes.notifier.PrayerAlarmScheduler;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.di.factory.worker.ChildWorkerFactory;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;

import io.reactivex.rxjava3.core.Single;


public class InstantPrayerScheduler extends RxWorker {

    private static final String TAG = "INST_PRAYER_SCHEDULER";
    private final PrayerAlarmScheduler prayerAlarmScheduler;

    @Inject
    public InstantPrayerScheduler(@NonNull Context context,
                                  @NonNull WorkerParameters params,
                                  @NonNull PrayerAlarmScheduler prayerAlarmScheduler
    ) {
        super(context, params);
        this.prayerAlarmScheduler = prayerAlarmScheduler;

        Log.i(TAG, "Instant Prayer Scheduler Initialized");
    }

    @NonNull
    @Override
    public Single<Result> createWork() {
        Log.i(TAG, "Starting Create Instant Prayer Scheduler Work");

        String dayPrayerString = getInputData().getString("DAY_PRAYER_PARAM");
        Gson gson = new Gson();

        try {
            DayPrayer dayPrayer = gson.fromJson(dayPrayerString, DayPrayer.class);
            prayerAlarmScheduler.scheduleAlarmsAndReminders(Objects.requireNonNull(dayPrayer));
            return Single.just(Result.success());
        } catch (Exception e) {
            return Single.just(Result.failure());
        }
    }

    public static class Factory implements ChildWorkerFactory {

        private final Provider<PrayerAlarmScheduler> prayerAlarmSchedulerProvider;

        @Inject
        public Factory(Provider<PrayerAlarmScheduler> prayerAlarmSchedulerProvider) {
            this.prayerAlarmSchedulerProvider = prayerAlarmSchedulerProvider;
        }

        @Override
        public ListenableWorker create(Context context, WorkerParameters workerParameters) {
            return new InstantPrayerScheduler(context, workerParameters, prayerAlarmSchedulerProvider.get());
        }
    }
}
