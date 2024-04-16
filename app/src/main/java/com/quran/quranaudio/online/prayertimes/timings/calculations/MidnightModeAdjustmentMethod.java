package com.quran.quranaudio.online.prayertimes.timings.calculations;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public enum MidnightModeAdjustmentMethod {

    STANDARD(0),
    JAFARI(1);

    private int value;

    MidnightModeAdjustmentMethod(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MidnightModeAdjustmentMethod getDefault() {
        return STANDARD;
    }
}
