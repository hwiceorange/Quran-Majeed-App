package com.raiadnan.quranreader.prayertimes.prayers.helper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HGDate {
    private final double GREGORIAN_EPOCH;
    private final double HIGRI_EPOCH;
    private int day;
    private double julianDay;
    private int month;
    DT type;
    private int year;

    public enum DT {
        NONE,
        HIGRI,
        GREGORIAN
    }

    public HGDate() {
        this.HIGRI_EPOCH = 1948438.5d;
        this.GREGORIAN_EPOCH = 1721425.5d;
        this.type = DT.NONE;
        this.julianDay = 0.0d;
        this.day = 0;
        this.month = 0;
        this.year = 0;
        DateNow();
    }

    public HGDate(HGDate hGDate) {
        this.HIGRI_EPOCH = 1948438.5d;
        this.GREGORIAN_EPOCH = 1721425.5d;
        this.type = hGDate.type;
        this.julianDay = hGDate.julianDay;
        this.day = hGDate.day;
        this.month = hGDate.month;
        this.year = hGDate.year;
    }

    private Date DateNow() {
        Calendar instance = Calendar.getInstance();
        String[] split = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(instance.getTime()).split("-");
        setGregorian(Integer.parseInt(split[2].trim()), Integer.parseInt(split[1].trim()), Integer.parseInt(split[0].trim()));
        return instance.getTime();
    }

    public double higri_to_jd(int i, int i2, int i3) {
        double d = (double) i3;
        double d2 = (double) (i2 - 1);
        Double.isNaN(d2);
        double ceil = Math.ceil(d2 * 29.5d);
        Double.isNaN(d);
        double d3 = d + ceil;
        double d4 = (double) ((i - 1) * 354);
        Double.isNaN(d4);
        return (((d3 + d4) + Math.floor((double) (((i * 11) + 3) / 30))) + 1948438.5d) - 1.0d;
    }

    private boolean leap_gregorian(int i) {
        return i % 4 == 0 && (i % 100 != 0 || i % 400 == 0);
    }

    private long mod(long j, long j2) {
        double d = (double) j;
        double d2 = (double) j2;
        double floor = Math.floor((double) (j / j2));
        Double.isNaN(d2);
        Double.isNaN(d);
        return (long) (d - (d2 * floor));
    }

    private double gregorian_to_jd(int i, int i2, int i3) {
        int i4;
        int i5 = i - 1;
        double d = (double) (i5 * 365);
        Double.isNaN(d);
        double floor = d + 1721424.5d + Math.floor((double) (i5 / 4)) + (-Math.floor((double) (i5 / 100))) + Math.floor((double) (i5 / 400));
        int i6 = ((i2 * 367) - 362) / 12;
        if (i2 <= 2) {
            i4 = 0;
        } else {
            i4 = leap_gregorian(i) ? -1 : -2;
        }
        return floor + Math.floor((double) (i6 + i4 + i3));
    }

    public boolean setHigri(int i, int i2, int i3) {
        if (i < 1 || i2 < 1 || i3 < 1) {
            return false;
        }
        this.julianDay = higri_to_jd(i, i2, i3);
        toHigri();
        if (i == this.year && i2 == this.month && i3 == this.day) {
            return true;
        }
        this.type = DT.NONE;
        return false;
    }

    public boolean setGregorian(int i, int i2, int i3) {
        if (i < 622 || i2 < 1 || i3 < 1) {
            return false;
        }
        if (i == 622 && i2 < 7) {
            return false;
        }
        if (i == 622 && i2 == 7 && i3 < 18) {
            return false;
        }
        this.julianDay = gregorian_to_jd(i, i2, i3);
        toGregorian();
        if (i == this.year && i2 == this.month && i3 == this.day) {
            return true;
        }
        this.type = DT.NONE;
        return false;
    }

    public void toHigri() {
        double floor = Math.floor(this.julianDay) + 0.5d;
        this.year = (int) Math.floor((((floor - 1948438.5d) * 30.0d) + 10646.0d) / 10631.0d);
        this.month = (int) Math.min(12.0d, Math.ceil((floor - (higri_to_jd(this.year, 1, 1) + 29.0d)) / 29.5d) + 1.0d);
        this.day = (int) ((floor - higri_to_jd(this.year, this.month, 1)) + 1.0d);
        this.type = DT.HIGRI;
    }

    public void toGregorian() {
        double floor = Math.floor(this.julianDay - 0.5d) + 0.5d;
        double d = floor - 1721425.5d;
        double floor2 = Math.floor(d / 146097.0d);
        double mod = (double) mod((long) d, 146097);
        Double.isNaN(mod);
        double floor3 = Math.floor(mod / 36524.0d);
        double mod2 = (double) mod((long) mod, 36524);
        Double.isNaN(mod2);
        double floor4 = Math.floor(mod2 / 1461.0d);
        double mod3 = (double) mod((long) mod2, 1461);
        Double.isNaN(mod3);
        double floor5 = Math.floor(mod3 / 365.0d);
        this.year = (int) ((floor2 * 400.0d) + (100.0d * floor3) + (floor4 * 4.0d) + floor5);
        if (!(floor3 == 4.0d || floor5 == 4.0d)) {
            this.year++;
        }
        double gregorian_to_jd = (double) ((long) (floor - gregorian_to_jd(this.year, 1, 1)));
        double d2 = (double) (floor < gregorian_to_jd(this.year, 3, 1) ? 0 : leap_gregorian(this.year) ? 1 : 2);
        Double.isNaN(gregorian_to_jd);
        Double.isNaN(d2);
        this.month = (int) Math.floor((((gregorian_to_jd + d2) * 12.0d) + 373.0d) / 367.0d);
        this.day = (int) ((floor - gregorian_to_jd(this.year, this.month, 1)) + 1.0d);
        this.type = DT.GREGORIAN;
    }

    public int getDay() {
        return this.day;
    }

    public int getMonth() {
        return this.month;
    }

    public int getYear() {
        return this.year;
    }

    public DT getType() {
        return this.type;
    }

    public int weekDay() {
        return (int) mod((long) Math.floor(this.julianDay + 1.5d), 7);
    }

    public void nextDay() {
        this.julianDay += 1.0d;
        if (this.type == DT.HIGRI) {
            toHigri();
        } else if (this.type == DT.GREGORIAN) {
            toGregorian();
        }
    }

    public void previousDay() {
        this.julianDay -= 1.0d;
        if (this.type == DT.HIGRI) {
            toHigri();
        } else if (this.type == DT.GREGORIAN) {
            toGregorian();
        }
    }

    public String toString() {
        return "" + this.day + "/" + this.month + "/" + this.year;
    }
}
