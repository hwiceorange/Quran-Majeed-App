package com.raiadnan.quranreader.prayertimes.locations.model;

public class City {
    private String city;
    private String country;
    private int id;
    private String lat;
    private String lon;
    private double timeZone;
    private String timeZoneName;

    public City() {
    }

    public City(int i, String str, double d, String str2, String str3, String str4, String str5) {
        this.id = i;
        this.city = str;
        this.timeZone = d;
        this.country = str2;
        this.lat = str3;
        this.lon = str4;
        this.timeZoneName = str5;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String str) {
        this.city = str;
    }

    public double getTimeZone() {
        return this.timeZone;
    }

    public void setTimeZone(double d) {
        this.timeZone = d;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String str) {
        this.country = str;
    }

    public String getLat() {
        return this.lat;
    }

    public void setLat(String str) {
        this.lat = str;
    }

    public String getLon() {
        return this.lon;
    }

    public void setLon(String str) {
        this.lon = str;
    }

    public String getTimeZoneName() {
        return this.timeZoneName;
    }

    public void setTimeZoneName(String str) {
        this.timeZoneName = str;
    }
}
