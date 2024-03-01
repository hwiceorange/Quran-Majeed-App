package com.quran.quranaudio.online.prayertimes.timings.londonprayertimes;

import com.quran.quranaudio.online.BuildConfig;
import com.quran.quranaudio.online.prayertimes.utils.TimingUtils;

import java.io.IOException;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Retrofit;


@Singleton
public class LondonUnifiedPrayerAPIService {

    private final static String API_KEY = BuildConfig.LONDON_UNIFIED_TIMINGS_API_KEY;
    private final static String DEFAULT_FORMAT = "json";
    private final static String TWENTY_FOUR_FORMAT = "true";

    private final Retrofit retrofit;

    @Inject
    public LondonUnifiedPrayerAPIService(@Named("lut_api") Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public LondonUnifiedTimingsResponse getLondonTimings() throws IOException {
        String localDateString = TimingUtils.formatDateForLUTAPI(LocalDate.now());

        LondonUnifiedPrayerAPIResource londonUnifiedPrayerAPIResource =
                retrofit.create(LondonUnifiedPrayerAPIResource.class);

        Call<LondonUnifiedTimingsResponse> call
                = londonUnifiedPrayerAPIResource
                .getLondonTimings(localDateString, TWENTY_FOUR_FORMAT, DEFAULT_FORMAT, API_KEY);

        return call.execute().body();
    }

    public LondonUnifiedCalendarResponse getLondonCalendar(final int month, final int year) throws IOException {

        LondonUnifiedPrayerAPIResource londonUnifiedPrayerAPIResource =
                retrofit.create(LondonUnifiedPrayerAPIResource.class);

        Call<LondonUnifiedCalendarResponse> call
                = londonUnifiedPrayerAPIResource
                .getMonthlyCalendar(month, year, TWENTY_FOUR_FORMAT, DEFAULT_FORMAT, API_KEY);

        return call.execute().body();
    }
}
