package com.quran.quranaudio.online.prayertimes.calendar;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public interface CalendarAPIResource {

    @GET("gToHCalendar/{month}/{year}")
    Call<CalendarAPIResponse> getHijriCalendar(
            @Path("month") int month,
            @Path("year") int year,
            @Query("adjustment") int adjustment
    );
}
