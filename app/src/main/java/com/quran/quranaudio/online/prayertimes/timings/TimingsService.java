package com.quran.quranaudio.online.prayertimes.timings;

import android.location.Address;

import java.time.LocalDate;
import java.util.List;

import io.reactivex.rxjava3.core.Single;


public interface TimingsService {

    Single<DayPrayer> getTimingsByCity(final LocalDate localDate, final Address address);

    Single<List<DayPrayer>> getCalendarByCity(final Address address, int month, int year);
}
