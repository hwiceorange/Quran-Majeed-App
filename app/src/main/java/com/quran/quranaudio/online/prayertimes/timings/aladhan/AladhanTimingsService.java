package com.quran.quranaudio.online.prayertimes.timings.aladhan;

import android.location.Address;

import com.quran.quranaudio.online.prayertimes.database.PrayerRegistry;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.timings.offline.OfflineTimingsService;
import com.quran.quranaudio.online.prayertimes.timings.AbstractTimingsService;
import com.quran.quranaudio.online.prayertimes.timings.TimingsPreferences;

import java.io.IOException;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class AladhanTimingsService extends AbstractTimingsService {

    private final AladhanAPIService aladhanAPIService;
    protected String TAG = "AladhanTimingsService";

    @Inject
    public AladhanTimingsService(AladhanAPIService aladhanAPIService,
                                 PrayerRegistry prayerRegistry,
                                 OfflineTimingsService offlineTimingsService,
                                 PreferencesHelper preferencesHelper) {
        super(prayerRegistry, offlineTimingsService, preferencesHelper);
        this.aladhanAPIService = aladhanAPIService;
    }

    protected void retrieveAndSaveTimings(LocalDate localDate, Address address) throws IOException {
        TimingsPreferences timingsPreferences = getTimingsPreferences();

        AladhanTodayTimingsResponse timingsByCity =
                aladhanAPIService.getTimingsByLatLong(
                        localDate,
                        address.getLatitude(),
                        address.getLongitude(),
                        timingsPreferences.getMethod(),
                        timingsPreferences.getLatitudeAdjustmentMethod(),
                        timingsPreferences.getSchoolAdjustmentMethod(),
                        timingsPreferences.getMidnightModeAdjustmentMethod(),
                        timingsPreferences.getHijriAdjustment(),
                        timingsPreferences.getTune());

        if (timingsByCity != null && address.getLocality() != null) {
            prayerRegistry.savePrayerTiming(localDate,
                    address.getLocality(),
                    address.getCountryName(),
                    timingsPreferences.getMethod(),
                    timingsPreferences.getLatitudeAdjustmentMethod(),
                    timingsPreferences.getSchoolAdjustmentMethod(),
                    timingsPreferences.getMidnightModeAdjustmentMethod(),
                    timingsPreferences.getHijriAdjustment(),
                    timingsPreferences.getTune(),
                    timingsByCity.getData()
            );
        }
    }

    protected void retrieveAndSaveCalendar(Address address, int month, int year) throws IOException {
        TimingsPreferences timingsPreferences = getTimingsPreferences();

        AladhanCalendarResponse CalendarByCity =
                aladhanAPIService.getCalendarByLatLong(
                        address.getLatitude(),
                        address.getLongitude(),
                        month, year,
                        timingsPreferences.getMethod(),
                        timingsPreferences.getLatitudeAdjustmentMethod(),
                        timingsPreferences.getSchoolAdjustmentMethod(),
                        timingsPreferences.getMidnightModeAdjustmentMethod(),
                        timingsPreferences.getHijriAdjustment(),
                        timingsPreferences.getTune());

        if (CalendarByCity != null) {
            prayerRegistry.saveCalendar(
                    address.getLocality(),
                    address.getCountryName(),
                    timingsPreferences.getMethod(),
                    timingsPreferences.getLatitudeAdjustmentMethod(),
                    timingsPreferences.getSchoolAdjustmentMethod(),
                    timingsPreferences.getMidnightModeAdjustmentMethod(),
                    timingsPreferences.getHijriAdjustment(),
                    timingsPreferences.getTune(),
                    CalendarByCity.getData());
        }
    }
}
