package com.quran.quranaudio.online.prayertimes.timings;

import com.quran.quranaudio.online.prayertimes.timings.calculations.CalculationMethodEnum;
import com.quran.quranaudio.online.prayertimes.timings.calculations.LatitudeAdjustmentMethod;
import com.quran.quranaudio.online.prayertimes.timings.calculations.MidnightModeAdjustmentMethod;
import com.quran.quranaudio.online.prayertimes.timings.calculations.SchoolAdjustmentMethod;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class TimingsPreferences {

    private CalculationMethodEnum method;
    private String tune;
    private LatitudeAdjustmentMethod latitudeAdjustmentMethod;
    private SchoolAdjustmentMethod schoolAdjustmentMethod;
    private MidnightModeAdjustmentMethod midnightModeAdjustmentMethod;
    private int hijriAdjustment;

    public TimingsPreferences(CalculationMethodEnum method,
                              String tune,
                              LatitudeAdjustmentMethod latitudeAdjustmentMethod,
                              SchoolAdjustmentMethod schoolAdjustmentMethod,
                              MidnightModeAdjustmentMethod midnightModeAdjustmentMethod,
                              int hijriAdjustment) {
        this.method = method;
        this.tune = tune;
        this.latitudeAdjustmentMethod = latitudeAdjustmentMethod;
        this.schoolAdjustmentMethod = schoolAdjustmentMethod;
        this.midnightModeAdjustmentMethod = midnightModeAdjustmentMethod;
        this.hijriAdjustment = hijriAdjustment;
    }

    public CalculationMethodEnum getMethod() {
        return method;
    }

    public String getTune() {
        return tune;
    }

    public LatitudeAdjustmentMethod getLatitudeAdjustmentMethod() {
        return latitudeAdjustmentMethod;
    }

    public SchoolAdjustmentMethod getSchoolAdjustmentMethod() {
        return schoolAdjustmentMethod;
    }

    public MidnightModeAdjustmentMethod getMidnightModeAdjustmentMethod() {
        return midnightModeAdjustmentMethod;
    }

    public int getHijriAdjustment() {
        return hijriAdjustment;
    }
}
