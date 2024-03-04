package com.raiadnan.quranreader.calender.model;

public class Event {
    private String eventName;
    private String hejriDate;
    private String url;

    public Event(String str, String str2, String str3) {
        this.eventName = str;
        this.hejriDate = str2;
        this.url = str3;
    }

    public String getEventName() {
        return this.eventName;
    }

    public void setEventName(String str) {
        this.eventName = str;
    }

    public String getHejriDate() {
        return this.hejriDate;
    }

    public void setHejriDate(String str) {
        this.hejriDate = str;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String str) {
        this.url = str;
    }
}
