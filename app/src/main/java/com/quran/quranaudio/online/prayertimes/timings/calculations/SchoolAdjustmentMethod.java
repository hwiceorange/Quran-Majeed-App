package com.quran.quranaudio.online.prayertimes.timings.calculations;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public enum SchoolAdjustmentMethod {

    SHAFII(0),
    HANAFI(1);

    private int value;

    SchoolAdjustmentMethod(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SchoolAdjustmentMethod getDefault() {
        return SHAFII;
    }
}
