package com.quran.quranaudio.online.prayertimes.location.photon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public interface PhotonAPIResource {

    @GET(".")
    Call<PhotonAPIResponse> search(@Query("q") String str,
                                   @Query("limit") int limit,
                                   @Query("lang") String lang);
}
