package com.raiadnan.quranreader.prayertimes.prayers.model;

public class PrayItem {
    private int icon;
    private boolean select = false;
    private long time;
    private String title;

    public PrayItem(int i, String str, long j) {
        this.icon = i;
        this.title = str;
        this.time = j;
    }

    public boolean isSelect() {
        return this.select;
    }

    public void setSelect(boolean z) {
        this.select = z;
    }

    public int getIcon() {
        return this.icon;
    }

    public void setIcon(int i) {
        this.icon = i;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String str) {
        this.title = str;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long j) {
        this.time = j;
    }
}
