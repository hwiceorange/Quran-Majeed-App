package com.raiadnan.quranreader.calender.model;

public class CalendarCell {
    private int georgianDay;
    private int georgianMonth;
    private int georgianYear;
    private int hijriDay;
    private int hijriMonth;
    private boolean select = false;
    private int week;

    public CalendarCell(int i, int i2, int i3, int i4, int i5, int i6) {
        this.hijriDay = i;
        this.georgianDay = i2;
        this.hijriMonth = i3;
        this.georgianMonth = i4;
        this.week = i5;
        this.georgianYear = i6;
    }

    public boolean isSelect() {
        return this.select;
    }

    public void setSelect(boolean z) {
        this.select = z;
    }

    public int getHijriDay() {
        return this.hijriDay;
    }

    public void setHijriDay(int i) {
        this.hijriDay = i;
    }

    public int getGeorgianDay() {
        return this.georgianDay;
    }

    public void setGeorgianDay(int i) {
        this.georgianDay = i;
    }

    public int getGeorgianYear() {
        return this.georgianYear;
    }

    public void setGeorgianYear(int i) {
        this.georgianYear = i;
    }

    public int getHijriMonth() {
        return this.hijriMonth;
    }

    public void setHijriMonth(int i) {
        this.hijriMonth = i;
    }

    public int getGeorgianMonth() {
        return this.georgianMonth;
    }

    public void setGeorgianMonth(int i) {
        this.georgianMonth = i;
    }

    public int getWeek() {
        return this.week;
    }

    public void setWeek(int i) {
        this.week = i;
    }
}
