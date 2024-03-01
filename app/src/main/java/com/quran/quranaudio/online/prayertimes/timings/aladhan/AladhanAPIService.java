package com.quran.quranaudio.online.prayertimes.timings.aladhan;

import androidx.annotation.NonNull;

import com.quran.quranaudio.online.prayertimes.timings.calculations.CalculationMethodEnum;
import com.quran.quranaudio.online.prayertimes.timings.calculations.LatitudeAdjustmentMethod;
import com.quran.quranaudio.online.prayertimes.timings.calculations.MidnightModeAdjustmentMethod;
import com.quran.quranaudio.online.prayertimes.timings.calculations.SchoolAdjustmentMethod;
import com.quran.quranaudio.online.prayertimes.utils.TimingUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Retrofit;


@Singleton
public class AladhanAPIService {

    private final Retrofit retrofit;

    @Inject
    public AladhanAPIService(@Named("adhan_api") Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public AladhanTodayTimingsResponse getTimingsByLatLong(
            final LocalDate localDate,
            final double latitude,
            final double longitude,
            final CalculationMethodEnum method,
            final LatitudeAdjustmentMethod latitudeAdjustmentMethod,
            SchoolAdjustmentMethod schoolAdjustmentMethod,
            MidnightModeAdjustmentMethod midnightModeAdjustmentMethod,
            final int adjustment,
            final String tune

    ) throws IOException {

        long epochSecond = TimingUtils.getTimestampsFromLocalDate(localDate, ZoneId.systemDefault());

        AladhanAPIResource aladhanAPIResource = retrofit.create(AladhanAPIResource.class);

        Call<AladhanTodayTimingsResponse> call
                = aladhanAPIResource
                .getTimingsByLatLong(String.valueOf(epochSecond), latitude, longitude, method.getMethodId(),
                        getMethodSettings(method),
                        latitudeAdjustmentMethod.getValue(),
                        schoolAdjustmentMethod.getValue(),
                        midnightModeAdjustmentMethod.getValue(),
                        adjustment, tune, ZoneId.systemDefault().getId());

        return call.execute().body();
    }

    public AladhanCalendarResponse getCalendarByLatLong(
            final double latitude,
            final double longitude,
            final int month, final int year,
            final CalculationMethodEnum method,
            final LatitudeAdjustmentMethod latitudeAdjustmentMethod,
            SchoolAdjustmentMethod schoolAdjustmentMethod,
            MidnightModeAdjustmentMethod midnightModeAdjustmentMethod,
            final int adjustment,
            final String tune) throws IOException {

        AladhanAPIResource aladhanAPIResource = retrofit.create(AladhanAPIResource.class);

        Call<AladhanCalendarResponse> call
                = aladhanAPIResource
                .getCalendarByLatLong(latitude, longitude, month, year, false, method.getMethodId(),
                        getMethodSettings(method),
                        latitudeAdjustmentMethod.getValue(),
                        schoolAdjustmentMethod.getValue(),
                        midnightModeAdjustmentMethod.getValue(),
                        adjustment,
                        tune,
                        ZoneId.systemDefault().getId()
                );

        return call.execute().body();
    }

    @NonNull
    private String getMethodSettings(CalculationMethodEnum method) {
        return method.getFajrAngle() + "," + method.getMaghribAngle() + "," + method.getIchaAngle();
    }
}
