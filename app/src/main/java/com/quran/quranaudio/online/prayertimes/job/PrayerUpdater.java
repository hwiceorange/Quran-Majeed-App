package com.quran.quranaudio.online.prayertimes.job;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.quran.quranaudio.online.prayertimes.location.address.AddressHelper;
import com.quran.quranaudio.online.prayertimes.location.tracker.LocationHelper;
import com.quran.quranaudio.online.prayertimes.notifier.PrayerAlarmScheduler;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.timings.TimingServiceFactory;
import com.quran.quranaudio.online.prayertimes.timings.TimingsService;
import com.quran.quranaudio.online.prayertimes.di.factory.worker.ChildWorkerFactory;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Provider;

import io.reactivex.rxjava3.core.Single;


public class PrayerUpdater extends RxWorker {

    private static final String TAG = "PrayerUpdater";
    private final Context context;
    private final LocationHelper locationHelper;
    private final AddressHelper addressHelper;
    private final TimingServiceFactory timingServiceFactory;
    private final PrayerAlarmScheduler prayerAlarmScheduler;
    private final PreferencesHelper preferencesHelper;

    private int runAttemptCount = 0;

    @Inject
    public PrayerUpdater(@NonNull Context context,
                         @NonNull WorkerParameters params,
                         @NonNull LocationHelper locationHelper,
                         @NonNull AddressHelper addressHelper,
                         @NonNull TimingServiceFactory timingServiceFactory,
                         @NonNull PrayerAlarmScheduler prayerAlarmScheduler,
                         @NonNull PreferencesHelper PreferencesHelper
    ) {
        super(context, params);
        this.context = context;
        this.locationHelper = locationHelper;
        this.addressHelper = addressHelper;
        this.timingServiceFactory = timingServiceFactory;
        this.prayerAlarmScheduler = prayerAlarmScheduler;
        this.preferencesHelper = PreferencesHelper;

        Log.i(TAG, "Prayer Updater Initialized");
    }

    @NonNull
    @Override
    public Single<Result> createWork() {
        Log.i(TAG, "Starting Create Prayer Updater Work");

        TimingsService timingsService = timingServiceFactory.create(preferencesHelper.getCalculationMethod());

        Single<DayPrayer> dayPrayerSingle =
                locationHelper.getLocation()
                        .flatMap(addressHelper::getAddressFromLocation)
                        .flatMap(address ->
                                timingsService.getTimingsByCity(LocalDate.now(), address));

        return dayPrayerSingle
                .doOnSuccess(prayerAlarmScheduler::scheduleAlarmsAndReminders)
               // .doAfterSuccess(dayPrayer -> widgetUpdater.updateHomeScreenWidgets(context))
                .map(dayPrayer -> {
                    Log.i(TAG, "Prayers alarm updated successfully");
                    return Result.success();
                })
                .onErrorReturn(error -> {
                    Log.e(TAG, "Prayer Updater Failure", error);

                    if (runAttemptCount > 3) {
                        Log.e(TAG, "Cancel Prayer Updater and return failure");
                        return Result.failure();
                    } else {
                        runAttemptCount++;
                        Log.e(TAG, "Retry Prayer Updater, runAttemptCount=" + runAttemptCount);
                        return Result.retry();
                    }
                });
    }

    public static class Factory implements ChildWorkerFactory {

        private final Provider<LocationHelper> locationHelperProvider;
        private final Provider<AddressHelper> addressHelperProvider;
        private final Provider<TimingServiceFactory> timingServiceFactoryProvider;
        private final Provider<PrayerAlarmScheduler> prayerAlarmSchedulerProvider;
        private final Provider<PreferencesHelper> preferencesHelperProvider;

        @Inject
        public Factory(Provider<LocationHelper> locationHelperProvider,
                       Provider<AddressHelper> addressHelperProvider,
                       Provider<TimingServiceFactory> timingServiceFactoryProvider,
                       Provider<PrayerAlarmScheduler> prayerAlarmSchedulerProvider,
                       Provider<PreferencesHelper> preferencesHelperProvider
        ) {

            this.locationHelperProvider = locationHelperProvider;
            this.addressHelperProvider = addressHelperProvider;
            this.timingServiceFactoryProvider = timingServiceFactoryProvider;
            this.prayerAlarmSchedulerProvider = prayerAlarmSchedulerProvider;
            this.preferencesHelperProvider = preferencesHelperProvider;
        }

        @Override
        public ListenableWorker create(Context context, WorkerParameters workerParameters) {
            return new PrayerUpdater(context,
                    workerParameters,
                    locationHelperProvider.get(),
                    addressHelperProvider.get(),
                    timingServiceFactoryProvider.get(),
                    prayerAlarmSchedulerProvider.get(),
                    preferencesHelperProvider.get()
            );
        }
    }
}
