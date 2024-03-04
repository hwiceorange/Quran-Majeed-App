package com.raiadnan.quranreader.prayertimes.prayers.model;

public class PrayerCalendarItem {
    private String asr;
    private String dhuhr;
    private String fajr;
    private String isha;
    private String maghrib;
    private String sunrise;
    private String time;

    public PrayerCalendarItem() {
    }

    public PrayerCalendarItem(String str, String str2, String str3, String str4, String str5, String str6, String str7) {
        this.time = str;
        this.fajr = str2;
        this.sunrise = str3;
        this.dhuhr = str4;
        this.asr = str5;
        this.maghrib = str6;
        this.isha = str7;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String str) {
        this.time = str;
    }

    public String getFajr() {
        return this.fajr;
    }

    public void setFajr(String str) {
        this.fajr = str;
    }

    public String getSunrise() {
        return this.sunrise;
    }

    public void setSunrise(String str) {
        this.sunrise = str;
    }

    public String getDhuhr() {
        return this.dhuhr;
    }

    public void setDhuhr(String str) {
        this.dhuhr = str;
    }

    public String getAsr() {
        return this.asr;
    }

    public void setAsr(String str) {
        this.asr = str;
    }

    public String getMaghrib() {
        return this.maghrib;
    }

    public void setMaghrib(String str) {
        this.maghrib = str;
    }

    public String getIsha() {
        return this.isha;
    }

    public void setIsha(String str) {
        this.isha = str;
    }
}
