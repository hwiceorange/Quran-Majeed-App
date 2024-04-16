package com.quran.quranaudio.online.prayertimes.location.osm;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public interface NominatimAPIResource {

    @GET("reverse")
    Call<NominatimReverseGeocodeResponse> getReverseGeocode(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("zoom") int zoom,
            @Query("addressdetails") int addressdetails,
            @Query("format") String format);
}
