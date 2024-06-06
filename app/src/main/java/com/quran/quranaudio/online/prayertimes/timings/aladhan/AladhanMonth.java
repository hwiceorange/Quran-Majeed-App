package com.quran.quranaudio.online.prayertimes.timings.aladhan;


public class AladhanMonth {

    private String number;
    private String en;
    private String ar;

    public AladhanMonth() {
    }

    public AladhanMonth(int number) {
        this.number = String.valueOf(number);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getEn() {
        return en;
    }

    public void setEn(String en) {
        this.en = en;
    }

    public String getAr() {
        return ar;
    }

    public void setAr(String ar) {
        this.ar = ar;
    }
}
