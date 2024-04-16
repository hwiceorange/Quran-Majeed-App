package com.quran.quranaudio.online.prayertimes.di.module;


import android.content.Context;

import com.quran.quranaudio.online.prayertimes.location.address.AddressHelper;
import com.quran.quranaudio.online.prayertimes.location.osm.NominatimAPIService;
import com.quran.quranaudio.online.prayertimes.location.tracker.LocationHelper;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.timings.TimingServiceFactory;
import com.quran.quranaudio.online.prayertimes.timings.aladhan.AladhanTimingsService;
import com.quran.quranaudio.online.prayertimes.timings.londonprayertimes.LondonUnifiedPrayerTimingsService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Module
public class WidgetModule {

    @Singleton
    @Provides
    public LocationHelper providesLocationHelper(Context context) {
        return new LocationHelper(context);
    }

    @Singleton
    @Provides
    public AddressHelper providesAddressHelper(Context context, NominatimAPIService nominatimAPIService, PreferencesHelper preferencesHelper) {
        return new AddressHelper(context, nominatimAPIService, preferencesHelper);
    }

    @Singleton
    @Provides
    public TimingServiceFactory providesTimingServiceFactory(LondonUnifiedPrayerTimingsService londonUnifiedPrayerTimingsService,
                                                             AladhanTimingsService aladhanTimingsService) {
        return new TimingServiceFactory(londonUnifiedPrayerTimingsService, aladhanTimingsService);
    }
}
