package com.raiadnan.quranreader.prayertimes.prayers.model;

public class PrayerTimeModel {
    private Double angleFajr;
    private Double angleIsha;
    private String convention = "MWL";
    private int conventionNumber = 3;
    private int conventionPosition;
    private int[] corrections = {-2, -1, 0, 0, 0, 4};
    private int dst = 0;
    private int hijri = 0;
    private String juristic = "Standard";
    private int juristicIndex = 1;

    public int getConventionPosition() {
        return this.conventionPosition;
    }

    public void setConventionPosition(int i) {
        this.conventionPosition = i;
    }

    public int getHijri() {
        return this.hijri;
    }

    public void setHijri(int i) {
        this.hijri = i;
    }

    public int getDst() {
        return this.dst;
    }

    public void setDst(int i) {
        this.dst = i;
    }

    public String getJuristic() {
        return this.juristic;
    }

    public void setJuristic(String str) {
        this.juristic = str;
    }

    public int getJuristicIndex() {
        return this.juristicIndex;
    }

    public void setJuristicIndex(int i) {
        this.juristicIndex = i;
    }

    public String getConvention() {
        return this.convention;
    }

    public void setConvention(String str) {
        this.convention = str;
    }

    public int getConventionNumber() {
        return this.conventionNumber;
    }

    public void setConventionNumber(int i) {
        this.conventionNumber = i;
    }

    public int[] getCorrections() {
        return this.corrections;
    }

    public void setCorrections(int[] iArr) {
        this.corrections = iArr;
    }

    public Double getAngleFajr() {
        return this.angleFajr;
    }

    public void setAngleFajr(Double d) {
        this.angleFajr = d;
    }

    public Double getAngleIsha() {
        return this.angleIsha;
    }

    public void setAngleIsha(Double d) {
        this.angleIsha = d;
    }
}
