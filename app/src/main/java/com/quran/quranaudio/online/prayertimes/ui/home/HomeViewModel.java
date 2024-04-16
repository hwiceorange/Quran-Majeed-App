package com.quran.quranaudio.online.prayertimes.ui.home;

import android.app.Application;
import android.content.Context;
import android.os.Build;

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
public class HomeViewModel extends AndroidViewModel {

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

    LiveData<DayPrayer> getDayPrayers() {
        return mDayPrayers;
    }

    LiveData<String> getError() {
        return mErrorMessage;
    }

    @Override
    protected void onCleared() {
        compositeDisposable.dispose();
        super.onCleared();
    }

    private void setLiveData(Context context) {
        TimingsService timingsService = timingServiceFactory.create(preferencesHelper.getCalculationMethod());

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
                locationHelper.getLocation()
                        .flatMap(addressHelper::getAddressFromLocation)
                        .flatMap(address ->
                                timingsService.getTimingsByCity(todayDate, address))
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<DayPrayer>() {
                            @Override
                            public void onSuccess(@NotNull DayPrayer dayPrayer) {
                                mDayPrayers.postValue(dayPrayer);
                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                mErrorMessage.postValue(e.getMessage());
                            }
                        }));
    }
}