package com.quran.quranaudio.online.prayertimes.openweathermap;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface OpenWeatherMapAPIResource {

    @GET("weather")
    Call<OpenWeatherMapResponse> getWeatherData(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("appid") String appKey,
            @Query("units") String units,
            @Query("exclude") String excludePart,
            @Query("lang")  String language);
}
