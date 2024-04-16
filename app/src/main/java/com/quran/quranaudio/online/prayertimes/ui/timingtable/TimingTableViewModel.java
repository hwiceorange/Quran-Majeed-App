package com.quran.quranaudio.online.prayertimes.ui.timingtable;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
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
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class TimingTableViewModel extends AndroidViewModel {

    private final LocationHelper locationHelper;
    private final AddressHelper addressHelper;
    private final PreferencesHelper preferencesHelper;
    private final TimingServiceFactory timingServiceFactory;
    private final MutableLiveData<List<DayPrayer>> mCalendar;
    private CompositeDisposable compositeDisposable;

    @Inject
    public TimingTableViewModel(@NonNull Application application,
                                @NonNull LocationHelper locationHelper,
                                @NonNull AddressHelper addressHelper,
                                @NonNull TimingServiceFactory timingServiceFactory,
                                @NonNull PreferencesHelper preferencesHelper
    ) {
        super(application);

        this.timingServiceFactory = timingServiceFactory;
        this.locationHelper = locationHelper;
        this.addressHelper = addressHelper;
        this.preferencesHelper = preferencesHelper;

        this.mCalendar = new MutableLiveData<>();
    }

    public void processData(LocalDate todayDate, Context context) {
        setLiveData(todayDate, context);
    }

    LiveData<List<DayPrayer>> getCalendar() {
        return mCalendar;
    }

    @Override
    protected void onCleared() {
        compositeDisposable.dispose();
        super.onCleared();
    }

    private void setLiveData(LocalDate todayDate, Context context) {
        TimingsService timingsService = timingServiceFactory.create(preferencesHelper.getCalculationMethod());

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
                locationHelper.getLocation()
                        .flatMap(addressHelper::getAddressFromLocation)
                        .flatMap(address ->
                                timingsService.getCalendarByCity(address, todayDate.getMonthValue(), todayDate.getYear()))
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<List<DayPrayer>>() {
                            @Override
                            public void onSuccess(@NotNull List<DayPrayer> calendar) {
                                mCalendar.postValue(calendar);
                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                //TODO
                                //mErrorMessage.setValue(e.getMessage());
                            }
                        }));
    }
}