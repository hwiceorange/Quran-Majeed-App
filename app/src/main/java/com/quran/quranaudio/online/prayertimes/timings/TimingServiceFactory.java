package com.quran.quranaudio.online.prayertimes.timings;

import com.quran.quranaudio.online.prayertimes.timings.aladhan.AladhanTimingsService;
import com.quran.quranaudio.online.prayertimes.timings.calculations.CalculationMethodEnum;
import com.quran.quranaudio.online.prayertimes.timings.londonprayertimes.LondonUnifiedPrayerTimingsService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Singleton
public class TimingServiceFactory {

    private final LondonUnifiedPrayerTimingsService londonUnifiedPrayerTimingsService;
    private final AladhanTimingsService aladhanTimingsService;

    @Inject
    public TimingServiceFactory(LondonUnifiedPrayerTimingsService londonUnifiedPrayerTimingsService,
                                AladhanTimingsService aladhanTimingsService) {

        this.londonUnifiedPrayerTimingsService = londonUnifiedPrayerTimingsService;
        this.aladhanTimingsService = aladhanTimingsService;
    }

    public TimingsService create(CalculationMethodEnum calculationMethodEnum) {
        if (CalculationMethodEnum.LONDON_UNIFIED_PRAYER_TIMES.equals(calculationMethodEnum)) {
            return londonUnifiedPrayerTimingsService;
        }
        return aladhanTimingsService;
    }
}
