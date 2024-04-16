package com.quran.quranaudio.online.prayertimes.location.osm;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Singleton
public class NominatimAPIService {

    private final Retrofit retrofit;

    @Inject
    public NominatimAPIService(@Named("nominatim_api") Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public NominatimReverseGeocodeResponse getAddressFromLocation(double latitude, double longitude) throws IOException {
        NominatimAPIResource nominatimAPIResource = retrofit.create(NominatimAPIResource.class);

        Call<NominatimReverseGeocodeResponse> call
                = nominatimAPIResource.getReverseGeocode(latitude, longitude, 18, 1, "json");

        Response<NominatimReverseGeocodeResponse> response = call.execute();

        return response.body();
    }
}