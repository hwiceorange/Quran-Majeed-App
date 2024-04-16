package com.quran.quranaudio.online.prayertimes.calendar;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Retrofit;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Singleton
public class CalendarAPIService {

    private final Retrofit retrofit;

    @Inject
    public CalendarAPIService(@Named("adhan_api") Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public CalendarAPIResponse getHijriCalendar(int month,
                                                int year,
                                                int adjustment) throws IOException {


        CalendarAPIResource calendarAPIResource = retrofit.create(CalendarAPIResource.class);

        Call<CalendarAPIResponse> call = calendarAPIResource.getHijriCalendar(month, year, adjustment);

        return call.execute().body();
    }
}
