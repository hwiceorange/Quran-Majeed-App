package com.quran.quranaudio.online.prayertimes.timings.aladhan;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class AladhanDate {

    private long timestamp;
    private AladhanDateType hijri;
    private AladhanDateType gregorian;

    public AladhanDate() {
    }

    public AladhanDate(AladhanDateType hijri, AladhanDateType gregorian) {
        this.hijri = hijri;
        this.gregorian = gregorian;
    }

    public AladhanDateType getHijri() {
        return hijri;
    }

    public void setHijri(AladhanDateType hijri) {
        this.hijri = hijri;
    }

    public AladhanDateType getGregorian() {
        return gregorian;
    }

    public void setGregorian(AladhanDateType gregorian) {
        this.gregorian = gregorian;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
