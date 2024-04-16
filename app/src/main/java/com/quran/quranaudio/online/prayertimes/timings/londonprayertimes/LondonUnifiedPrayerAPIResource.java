package com.quran.quranaudio.online.prayertimes.timings.londonprayertimes;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public interface LondonUnifiedPrayerAPIResource {

    @GET("times")
    Call<LondonUnifiedTimingsResponse> getLondonTimings(
            @Query("date") String date,
            @Query("24hours") String twentyFourFormat,
            @Query("format") String format,
            @Query("key") String key
    );

    @GET("times")
    Call<LondonUnifiedCalendarResponse> getMonthlyCalendar(
            @Query("month") int month,
            @Query("year") int year,
            @Query("24hours") String twentyFourFormat,
            @Query("format") String format,
            @Query("key") String key
    );
}
