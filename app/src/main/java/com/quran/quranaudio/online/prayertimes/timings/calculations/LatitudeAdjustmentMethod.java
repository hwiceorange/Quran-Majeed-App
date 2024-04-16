package com.quran.quranaudio.online.prayertimes.timings.calculations;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public enum LatitudeAdjustmentMethod {

    MIDDLE_OF_THE_NIGHT(1),
    ONE_SEVENTH(2),
    ANGLE_BASED(3);

    private int value;

    LatitudeAdjustmentMethod(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LatitudeAdjustmentMethod getDefault() {
        return ANGLE_BASED;
    }
}
