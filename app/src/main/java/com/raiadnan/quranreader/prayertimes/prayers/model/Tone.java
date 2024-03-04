package com.raiadnan.quranreader.prayertimes.prayers.model;

public class Tone {
    private int icon;
    private String title;

    public Tone(int i, String str) {
        this.icon = i;
        this.title = str;
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
}
