package com.raiadnan.quranreader.prayertimes.prayers.helper;

import android.util.Log;
import com.raiadnan.quranreader.prayertimes.prayers.fragment.PrayerFragment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class PrayTime {
    public int Algeria;
    public int AngleBased;
    public int Custom;
    public int Egypt;
    public int EgyptBis;
    public int FixedIsha;
    public int Floating;
    public int Hanafi;
    public int ISNA;
    private String InvalidTime;
    public int JAKIM;
    private double JDate;
    public int Jafari;
    public int KEMENAG;
    public int Karachi;
    public int MUIS;
    public int MWL;
    public int Makkah;
    public int MidNight;
    public int None;
    public int OneSeventh;
    public int Shafii;
    public int Tehran;
    public int Time12;
    public int Time12NS;
    public int Time24;
    private int adjustHighLats;
    private int asrJuristic;
    private int calcMethod;
    private int dhuhrMinutes;
    private double lat;
    private double lng;
    private HashMap<Integer, double[]> methodParams;
    private int numIterations;
    private int[] offsets;
    private double[] prayerTimesCurrent;
    private int timeFormat;
    private ArrayList<String> timeNames = new ArrayList<>();
    private double timeZone;

    private double DegreesToRadians(double d) {
        return (d * 3.141592653589793d) / 180.0d;
    }

    private double radiansToDegrees(double d) {
        return (d * 180.0d) / 3.141592653589793d;
    }

    public PrayTime() {
        setCalcMethod(0);
        setAsrJuristic(0);
        setDhuhrMinutes(0);
        setAdjustHighLats(1);
        setTimeFormat(0);
        setJafari(0);
        setKarachi(1);
        setISNA(2);
        setMWL(3);
        setMakkah(4);
        setEgypt(5);
        setTehran(6);
        setEgyptBis(7);
        setKEMENAG(8);
        setMUIS(9);
        setFixedIsha(10);
        setJAKIM(11);
        setAlgeria(12);
        setCustom(13);
        setShafii(0);
        setHanafi(1);
        setNone(0);
        setMidNight(1);
        setOneSeventh(2);
        setAngleBased(3);
        setTime24(0);
        setTime12(1);
        setTime12NS(2);
        setFloating(3);
        this.timeNames.add(PrayerFragment.FAJR);
        this.timeNames.add(PrayerFragment.SUNRISE);
        this.timeNames.add(PrayerFragment.DHUHR);
        this.timeNames.add(PrayerFragment.ASR);
        this.timeNames.add("Sunset");
        this.timeNames.add(PrayerFragment.MAGHRIB);
        this.timeNames.add("Isha");
        this.InvalidTime = "-----";
        setNumIterations(1);
        this.offsets = new int[7];
        int[] iArr = this.offsets;
        iArr[0] = 0;
        iArr[1] = 0;
        iArr[2] = 0;
        iArr[3] = 0;
        iArr[4] = 0;
        iArr[5] = 0;
        iArr[6] = 0;
        this.methodParams = new HashMap<>();
        this.methodParams.put(Integer.valueOf(getJafari()), new double[]{16.0d, 0.0d, 4.0d, 0.0d, 14.0d});
        this.methodParams.put(Integer.valueOf(getKarachi()), new double[]{18.0d, 1.0d, 0.0d, 0.0d, 18.0d});
        this.methodParams.put(Integer.valueOf(getISNA()), new double[]{15.0d, 1.0d, 0.0d, 0.0d, 15.0d});
        this.methodParams.put(Integer.valueOf(getMWL()), new double[]{18.0d, 1.0d, 0.0d, 0.0d, 17.0d});
        this.methodParams.put(Integer.valueOf(getMakkah()), new double[]{18.5d, 1.0d, 0.0d, 1.0d, 90.0d});
        this.methodParams.put(Integer.valueOf(getEgypt()), new double[]{19.5d, 1.0d, 0.0d, 0.0d, 17.5d});
        this.methodParams.put(Integer.valueOf(getTehran()), new double[]{17.7d, 0.0d, 4.5d, 0.0d, 14.0d});
        double[] dArr = {20.0d, 1.0d, 0.0d, 0.0d, 18.0d};
        this.methodParams.put(Integer.valueOf(getEgyptBis()), dArr);
        double[] dArr2 = {20.0d, 1.0d, 0.0d, 0.0d, 18.0d};
        this.methodParams.put(Integer.valueOf(getKEMENAG()), dArr);
        double[] dArr3 = {20.0d, 1.0d, 0.0d, 0.0d, 18.0d};
        this.methodParams.put(Integer.valueOf(getMUIS()), dArr);
        double[] dArr4 = {19.5d, 1.0d, 0.0d, 1.0d, 90.0d};
        this.methodParams.put(Integer.valueOf(getFixedIsha()), dArr);
        double[] dArr5 = {20.0d, 1.0d, 0.0d, 0.0d, 18.0d};
        this.methodParams.put(Integer.valueOf(getJAKIM()), dArr);
        double[] dArr6 = {18.0d, 1.0d, 0.0d, 0.0d, 17.0d};
        this.methodParams.put(Integer.valueOf(getAlgeria()), dArr);
        this.methodParams.put(Integer.valueOf(getCustom()), new double[]{18.0d, 1.0d, 0.0d, 0.0d, 17.0d});
    }

    private double fixangle(double d) {
        double floor = d - (Math.floor(d / 360.0d) * 360.0d);
        return floor < 0.0d ? floor + 360.0d : floor;
    }

    private double fixhour(double d) {
        double floor = d - (Math.floor(d / 24.0d) * 24.0d);
        return floor < 0.0d ? floor + 24.0d : floor;
    }

    private double dsin(double d) {
        return Math.sin(DegreesToRadians(d));
    }

    private double dcos(double d) {
        return Math.cos(DegreesToRadians(d));
    }

    private double dtan(double d) {
        return Math.tan(DegreesToRadians(d));
    }

    private double darcsin(double d) {
        return radiansToDegrees(Math.asin(d));
    }

    private double darccos(double d) {
        return radiansToDegrees(Math.acos(d));
    }

    private double darctan(double d) {
        return radiansToDegrees(Math.atan(d));
    }

    private double darctan2(double d, double d2) {
        return radiansToDegrees(Math.atan2(d, d2));
    }

    private double darccot(double d) {
        return radiansToDegrees(Math.atan2(1.0d, d));
    }

    private double getTimeZone1() {
        double rawOffset = (double) TimeZone.getDefault().getRawOffset();
        Double.isNaN(rawOffset);
        return (rawOffset / 1000.0d) / 3600.0d;
    }

    private double getBaseTimeZone() {
        double rawOffset = (double) TimeZone.getDefault().getRawOffset();
        Double.isNaN(rawOffset);
        return (rawOffset / 1000.0d) / 3600.0d;
    }

    public int detectDaylightSaving() {
        return (TimeZone.getDefault().getDSTSavings() / 1000) / 60;
    }

    private double julianDate(int i, int i2, int i3) {
        if (i2 <= 2) {
            i--;
            i2 += 12;
        }
        double d = (double) i;
        Double.isNaN(d);
        double floor = Math.floor(d / 100.0d);
        double floor2 = (2.0d - floor) + Math.floor(floor / 4.0d);
        double d2 = (double) (i + 4716);
        Double.isNaN(d2);
        double floor3 = Math.floor(d2 * 365.25d);
        double d3 = (double) (i2 + 1);
        Double.isNaN(d3);
        double floor4 = floor3 + Math.floor(d3 * 30.6001d);
        double d4 = (double) i3;
        Double.isNaN(d4);
        return ((floor4 + d4) + floor2) - 1524.5d;
    }

    private double calcJD(int i, int i2, int i3) {
        double time = (double) new Date(i, i2 - 1, i3).getTime();
        Double.isNaN(time);
        return (Math.floor(time / 8.64E7d) + 2440588.0d) - 0.5d;
    }

    private double[] sunPosition(double d) {
        double d2 = d - 2451545.0d;
        double fixangle = fixangle((0.98560028d * d2) + 357.529d);
        double fixangle2 = fixangle((0.98564736d * d2) + 280.459d);
        double fixangle3 = fixangle((dsin(fixangle) * 1.915d) + fixangle2 + (dsin(fixangle * 2.0d) * 0.02d));
        double d3 = 23.439d - (d2 * 3.6E-7d);
        return new double[]{darcsin(dsin(d3) * dsin(fixangle3)), (fixangle2 / 15.0d) - fixhour(darctan2(dcos(d3) * dsin(fixangle3), dcos(fixangle3)) / 15.0d)};
    }

    private double equationOfTime(double d) {
        return sunPosition(d)[1];
    }

    private double sunDeclination(double d) {
        return sunPosition(d)[0];
    }

    private double computeMidDay(double d) {
        return fixhour(12.0d - equationOfTime(getJDate() + d));
    }

    private double computeTime(double d, double d2) {
        double sunDeclination = sunDeclination(getJDate() + d2);
        double computeMidDay = computeMidDay(d2);
        double darccos = darccos(((-dsin(d)) - (dsin(sunDeclination) * dsin(getLat()))) / (dcos(sunDeclination) * dcos(getLat()))) / 15.0d;
        if (d > 90.0d) {
            darccos = -darccos;
        }
        return computeMidDay + darccos;
    }

    private double computeAsr(double d, double d2) {
        return computeTime(-darccot(d + dtan(Math.abs(getLat() - sunDeclination(getJDate() + d2)))), d2);
    }

    private double timeDiff(double d, double d2) {
        return fixhour(d2 - d);
    }

    private ArrayList<String> getDatePrayerTimes(int i, int i2, int i3, double d, double d2, double d3) {
        setLat(d);
        setLng(d2);
        setTimeZone(d3);
        setJDate(julianDate(i, i2, i3));
        setJDate(getJDate() - (d2 / 360.0d));
        return computeDayTimes();
    }

    public ArrayList<String> getPrayerTimes(Calendar calendar, double d, double d2, double d3) {
        return getDatePrayerTimes(calendar.get(1), calendar.get(2) + 1, calendar.get(5), d, d2, d3);
    }

    public void setCustomParams(double[] dArr) {
        for (int i = 0; i < 5; i++) {
            if (dArr[i] == -1.0d) {
                dArr[i] = this.methodParams.get(Integer.valueOf(getCalcMethod()))[i];
                this.methodParams.put(Integer.valueOf(getCustom()), dArr);
            } else {
                this.methodParams.get(Integer.valueOf(getCustom()))[i] = dArr[i];
            }
        }
        setCalcMethod(getCustom());
    }

    public void setFajrAngle(double d) {
        setCustomParams(new double[]{d, -1.0d, -1.0d, -1.0d, -1.0d});
    }

    public void setMaghribAngle(double d) {
        setCustomParams(new double[]{-1.0d, 0.0d, d, -1.0d, -1.0d});
    }

    public void setIshaAngle(double d) {
        setCustomParams(new double[]{-1.0d, -1.0d, -1.0d, 0.0d, d});
    }

    public void setMaghribMinutes(double d) {
        setCustomParams(new double[]{-1.0d, 1.0d, d, -1.0d, -1.0d});
    }

    public void setIshaMinutes(double d) {
        setCustomParams(new double[]{-1.0d, -1.0d, -1.0d, 1.0d, d});
    }

    public String floatToTime24(double d) {
        if (Double.isNaN(d)) {
            return this.InvalidTime;
        }
        double fixhour = fixhour(d + 0.008333333333333333d);
        int floor = (int) Math.floor(fixhour);
        double d2 = (double) floor;
        Double.isNaN(d2);
        double floor2 = Math.floor((fixhour - d2) * 60.0d);
        if (floor >= 0 && floor <= 9 && floor2 >= 0.0d && floor2 <= 9.0d) {
            return "0" + floor + ":0" + Math.round(floor2);
        } else if (floor >= 0 && floor <= 9) {
            return "0" + floor + ":" + Math.round(floor2);
        } else if (floor2 < 0.0d || floor2 > 9.0d) {
            return floor + ":" + Math.round(floor2);
        } else {
            return floor + ":0" + Math.round(floor2);
        }
    }

    public String floatToTime12(double d, boolean z) {
        if (Double.isNaN(d)) {
            return this.InvalidTime;
        }
        double fixhour = fixhour(d + 0.008333333333333333d);
        int floor = (int) Math.floor(fixhour);
        double d2 = (double) floor;
        Double.isNaN(d2);
        double floor2 = Math.floor((fixhour - d2) * 60.0d);
        String str = floor >= 12 ? "pm" : "am";
        int i = (((floor + 12) - 1) % 12) + 1;
        if (!z) {
            if (i >= 0 && i <= 9 && floor2 >= 0.0d && floor2 <= 9.0d) {
                return "0" + i + ":0" + Math.round(floor2) + " " + str;
            } else if (i >= 0 && i <= 9) {
                return "0" + i + ":" + Math.round(floor2) + " " + str;
            } else if (floor2 < 0.0d || floor2 > 9.0d) {
                return i + ":" + Math.round(floor2) + " " + str;
            } else {
                return i + ":0" + Math.round(floor2) + " " + str;
            }
        } else if (i >= 0 && i <= 9 && floor2 >= 0.0d && floor2 <= 9.0d) {
            return "0" + i + ":0" + Math.round(floor2);
        } else if (i >= 0 && i <= 9) {
            return "0" + i + ":" + Math.round(floor2);
        } else if (floor2 < 0.0d || floor2 > 9.0d) {
            return i + ":" + Math.round(floor2);
        } else {
            return i + ":0" + Math.round(floor2);
        }
    }

    public String floatToTime12NS(double d) {
        return floatToTime12(d, true);
    }

    private double[] computeTimes(double[] dArr) {
        double[] dayPortion = dayPortion(dArr);
        return new double[]{computeTime(180.0d - this.methodParams.get(Integer.valueOf(getCalcMethod()))[0], dayPortion[0]), computeTime(179.167d, dayPortion[1]), computeMidDay(dayPortion[2]), computeAsr((double) (getAsrJuristic() + 1), dayPortion[3]), computeTime(0.833d, dayPortion[4]), computeTime(this.methodParams.get(Integer.valueOf(getCalcMethod()))[2], dayPortion[5]), computeTime(this.methodParams.get(Integer.valueOf(getCalcMethod()))[4], dayPortion[6])};
    }

    private ArrayList<String> computeDayTimes() {
        double[] dArr = {5.0d, 6.0d, 12.0d, 13.0d, 18.0d, 18.0d, 18.0d};
        for (int i = 1; i <= getNumIterations(); i++) {
            dArr = computeTimes(dArr);
        }
        return adjustTimesFormat(tuneTimes(adjustTimes(dArr)));
    }

    private double[] adjustTimes(double[] dArr) {
        for (int i = 0; i < dArr.length; i++) {
            dArr[i] = dArr[i] + (getTimeZone() - (getLng() / 15.0d));
        }
        double d = dArr[2];
        double dhuhrMinutes2 = (double) (getDhuhrMinutes() / 60);
        Double.isNaN(dhuhrMinutes2);
        dArr[2] = d + dhuhrMinutes2;
        if (this.methodParams.get(Integer.valueOf(getCalcMethod()))[1] == 1.0d) {
            dArr[5] = dArr[4] + (this.methodParams.get(Integer.valueOf(getCalcMethod()))[2] / 60.0d);
        }
        if (this.methodParams.get(Integer.valueOf(getCalcMethod()))[3] == 1.0d) {
            dArr[6] = dArr[5] + (this.methodParams.get(Integer.valueOf(getCalcMethod()))[4] / 60.0d);
        }
        return getAdjustHighLats() != getNone() ? adjustHighLatTimes(dArr) : dArr;
    }

    private ArrayList<String> adjustTimesFormat(double[] dArr) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (getTimeFormat() == getFloating()) {
            for (double valueOf : dArr) {
                arrayList.add(String.valueOf(valueOf));
            }
            return arrayList;
        }
        for (int i = 0; i < 7; i++) {
            if (getTimeFormat() == getTime12()) {
                arrayList.add(floatToTime12(dArr[i], false));
            } else if (getTimeFormat() == getTime12NS()) {
                arrayList.add(floatToTime12(dArr[i], true));
            } else {
                arrayList.add(floatToTime24(dArr[i]));
            }
        }
        return arrayList;
    }

    private double[] adjustHighLatTimes(double[] dArr) {
        double timeDiff = timeDiff(dArr[4], dArr[1]);
        double nightPortion = nightPortion(this.methodParams.get(Integer.valueOf(getCalcMethod()))[0]) * timeDiff;
        if (Double.isNaN(dArr[0]) || timeDiff(dArr[0], dArr[1]) > nightPortion) {
            dArr[0] = dArr[1] - nightPortion;
        }
        double nightPortion2 = nightPortion(this.methodParams.get(Integer.valueOf(getCalcMethod()))[3] == 0.0d ? this.methodParams.get(Integer.valueOf(getCalcMethod()))[4] : 18.0d) * timeDiff;
        if (Double.isNaN(dArr[6]) || timeDiff(dArr[4], dArr[6]) > nightPortion2) {
            dArr[6] = dArr[4] + nightPortion2;
        }
        double nightPortion3 = nightPortion(this.methodParams.get(Integer.valueOf(getCalcMethod()))[1] == 0.0d ? this.methodParams.get(Integer.valueOf(getCalcMethod()))[2] : 4.0d) * timeDiff;
        if (Double.isNaN(dArr[5]) || timeDiff(dArr[4], dArr[5]) > nightPortion3) {
            dArr[5] = dArr[4] + nightPortion3;
        }
        return dArr;
    }

    private double nightPortion(double d) {
        int i = this.adjustHighLats;
        if (i == this.AngleBased) {
            return d / 60.0d;
        }
        if (i == this.MidNight) {
            return 0.5d;
        }
        return i == this.OneSeventh ? 0.14286d : 0.0d;
    }

    private double[] dayPortion(double[] dArr) {
        for (int i = 0; i < 7; i++) {
            dArr[i] = dArr[i] / 24.0d;
        }
        return dArr;
    }

    public void tune(int[] iArr) {
        for (int i = 0; i < iArr.length; i++) {
            this.offsets[i] = iArr[i];
        }
    }

    private double[] tuneTimes(double[] dArr) {
        for (int i = 0; i < dArr.length; i++) {
            double d = dArr[i];
            double d2 = (double) this.offsets[i];
            Double.isNaN(d2);
            dArr[i] = d + (d2 / 60.0d);
        }
        return dArr;
    }

    public static void main(String[] strArr) {
        PrayTime prayTime = new PrayTime();
        prayTime.setTimeFormat(prayTime.Time12);
        prayTime.setCalcMethod(prayTime.Jafari);
        prayTime.setAsrJuristic(prayTime.Shafii);
        prayTime.setAdjustHighLats(prayTime.AngleBased);
        prayTime.tune(new int[]{0, 0, 0, 0, 0, 0, 0});
        Date date = new Date();
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        ArrayList<String> prayerTimes = prayTime.getPrayerTimes(instance, -37.823689d, 145.121597d, 10.0d);
        ArrayList<String> timeNames2 = prayTime.getTimeNames();
        for (int i = 0; i < prayerTimes.size(); i++) {
            Log.v("Timings", timeNames2.get(i) + " - " + prayerTimes.get(i));
        }
    }

    public int getCalcMethod() {
        return this.calcMethod;
    }

    public void setCalcMethod(int i) {
        this.calcMethod = i;
    }

    public int getAsrJuristic() {
        return this.asrJuristic;
    }

    public void setAsrJuristic(int i) {
        this.asrJuristic = i;
    }

    public int getDhuhrMinutes() {
        return this.dhuhrMinutes;
    }

    public void setDhuhrMinutes(int i) {
        this.dhuhrMinutes = i;
    }

    public int getAdjustHighLats() {
        return this.adjustHighLats;
    }

    public void setAdjustHighLats(int i) {
        this.adjustHighLats = i;
    }

    public int getTimeFormat() {
        return this.timeFormat;
    }

    public void setTimeFormat(int i) {
        this.timeFormat = i;
    }

    public double getLat() {
        return this.lat;
    }

    public void setLat(double d) {
        this.lat = d;
    }

    public double getLng() {
        return this.lng;
    }

    public void setLng(double d) {
        this.lng = d;
    }

    public double getTimeZone() {
        return this.timeZone;
    }

    public void setTimeZone(double d) {
        this.timeZone = d;
    }

    public double getJDate() {
        return this.JDate;
    }

    public void setJDate(double d) {
        this.JDate = d;
    }

    private int getJafari() {
        return this.Jafari;
    }

    private void setJafari(int i) {
        this.Jafari = i;
    }

    private int getKarachi() {
        return this.Karachi;
    }

    private void setKarachi(int i) {
        this.Karachi = i;
    }

    private int getISNA() {
        return this.ISNA;
    }

    private void setISNA(int i) {
        this.ISNA = i;
    }

    private int getMWL() {
        return this.MWL;
    }

    private void setMWL(int i) {
        this.MWL = i;
    }

    private int getMakkah() {
        return this.Makkah;
    }

    private void setMakkah(int i) {
        this.Makkah = i;
    }

    private int getEgypt() {
        return this.Egypt;
    }

    private void setEgypt(int i) {
        this.Egypt = i;
    }

    private int getEgyptBis() {
        return this.EgyptBis;
    }

    private void setEgyptBis(int i) {
        this.EgyptBis = i;
    }

    public int getKEMENAG() {
        return this.KEMENAG;
    }

    public void setKEMENAG(int i) {
        this.KEMENAG = i;
    }

    public int getMUIS() {
        return this.MUIS;
    }

    public void setMUIS(int i) {
        this.MUIS = i;
    }

    public int getFixedIsha() {
        return this.FixedIsha;
    }

    public void setFixedIsha(int i) {
        this.FixedIsha = i;
    }

    public int getJAKIM() {
        return this.JAKIM;
    }

    public void setJAKIM(int i) {
        this.JAKIM = i;
    }

    public int getAlgeria() {
        return this.Algeria;
    }

    public void setAlgeria(int i) {
        this.Algeria = i;
    }

    private int getCustom() {
        return this.Custom;
    }

    private void setCustom(int i) {
        this.Custom = i;
    }

    private int getTehran() {
        return this.Tehran;
    }

    private void setTehran(int i) {
        this.Tehran = i;
    }

    private int getShafii() {
        return this.Shafii;
    }

    private void setShafii(int i) {
        this.Shafii = i;
    }

    private int getHanafi() {
        return this.Hanafi;
    }

    private void setHanafi(int i) {
        this.Hanafi = i;
    }

    private int getNone() {
        return this.None;
    }

    private void setNone(int i) {
        this.None = i;
    }

    private int getMidNight() {
        return this.MidNight;
    }

    private void setMidNight(int i) {
        this.MidNight = i;
    }

    private int getOneSeventh() {
        return this.OneSeventh;
    }

    private void setOneSeventh(int i) {
        this.OneSeventh = i;
    }

    private int getAngleBased() {
        return this.AngleBased;
    }

    private void setAngleBased(int i) {
        this.AngleBased = i;
    }

    private int getTime24() {
        return this.Time24;
    }

    private void setTime24(int i) {
        this.Time24 = i;
    }

    private int getTime12() {
        return this.Time12;
    }

    private void setTime12(int i) {
        this.Time12 = i;
    }

    private int getTime12NS() {
        return this.Time12NS;
    }

    private void setTime12NS(int i) {
        this.Time12NS = i;
    }

    private int getFloating() {
        return this.Floating;
    }

    private void setFloating(int i) {
        this.Floating = i;
    }

    private int getNumIterations() {
        return this.numIterations;
    }

    private void setNumIterations(int i) {
        this.numIterations = i;
    }

    public ArrayList<String> getTimeNames() {
        return this.timeNames;
    }
}
