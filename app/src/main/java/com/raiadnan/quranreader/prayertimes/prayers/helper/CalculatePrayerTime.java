package com.raiadnan.quranreader.prayertimes.prayers.helper;

import android.content.Context;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.raiadnan.quranreader.prayertimes.prayers.model.PrayerTimeModel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class CalculatePrayerTime {
    private int adjustment = 1;
    private Context context;
    private int juristic = 1;
    private int method = 1;
    private PrayTime prayers = new PrayTime();
    private double timezone;

    public CalculatePrayerTime(Context context2) {
        this.context = context2;
    }

    public PrayTime getPrayers() {
        return this.prayers;
    }

    public ArrayList<String> NamazTimings(HGDate hGDate, double d, double d2) {
        this.timezone = LocationSave.getTimeZone();
        HashMap<String, Integer> settings = PrayerTimeSettingsPref.get().getSettings();
        PrayTime prayTime = this.prayers;
        prayTime.setTimeFormat(prayTime.Time24);
        if (PrayerTimeSettingsPref.get().isAutoSettings()) {
            PrayerTimeModel autoSettings = new AutoSettingJsonParse().getAutoSettings(this.context);
            this.juristic = autoSettings.getJuristicIndex();
            this.method = autoSettings.getConventionNumber();
            this.prayers.setAsrJuristic(this.juristic - 1);
            this.prayers.setCalcMethod(this.method);
            PrayTime prayTime2 = this.prayers;
            prayTime2.setAdjustHighLats(prayTime2.AngleBased);
            PrayerTimeSettingsPref.get().setCalculationMethod(this.method);
            int detectDaylightSaving = this.prayers.detectDaylightSaving();
            int i = autoSettings.getCorrections()[3];
            if (PrayerTimeSettingsPref.get().isDaylightSaving()) {
                this.prayers.tune(new int[]{autoSettings.getCorrections()[0] + detectDaylightSaving, autoSettings.getCorrections()[1]
                        + detectDaylightSaving,
                        autoSettings.getCorrections()[2] + detectDaylightSaving, i + detectDaylightSaving,
                        autoSettings.getCorrections()[4] + detectDaylightSaving, autoSettings.getCorrections()[4] +
                        detectDaylightSaving, detectDaylightSaving + autoSettings.getCorrections()[5]});
            } else {
                this.prayers.tune(new int[]{autoSettings.getCorrections()[0], autoSettings.getCorrections()[1], autoSettings.getCorrections()[2], i, autoSettings.getCorrections()[4], autoSettings.getCorrections()[4], autoSettings.getCorrections()[5]});
            }
        } else {
            this.juristic = settings.get(PrayerTimeSettingsPref.JURISTIC).intValue();
            this.method = settings.get(PrayerTimeSettingsPref.CALCULATION_METHOD).intValue();
            this.adjustment = settings.get(PrayerTimeSettingsPref.LATITUDE_ADJUSTMENT).intValue();
            int correctionsFajr = PrayerTimeSettingsPref.get().getCorrectionsFajr();
            int correctionsSunrize = PrayerTimeSettingsPref.get().getCorrectionsSunrize();
            int correctionsZuhar = PrayerTimeSettingsPref.get().getCorrectionsZuhar();
            int correctionsAsar = PrayerTimeSettingsPref.get().getCorrectionsAsar();
            int correctionsMaghrib = PrayerTimeSettingsPref.get().getCorrectionsMaghrib();
            int correctionsIsha = PrayerTimeSettingsPref.get().getCorrectionsIsha();
            this.prayers.setAsrJuristic(this.juristic - 1);
            this.prayers.setCalcMethod(this.method);
            PrayTime prayTime3 = this.prayers;
            prayTime3.setAdjustHighLats(prayTime3.AngleBased);
            if (PrayerTimeSettingsPref.get().isDaylightSaving()) {
                int i2 = correctionsMaghrib + 60;
                this.prayers.tune(new int[]{correctionsFajr + 60, correctionsSunrize + 60, correctionsZuhar + 60, correctionsAsar + 60, i2, i2, correctionsIsha + 60});
            } else {
                this.prayers.tune(new int[]{correctionsFajr, correctionsSunrize, correctionsZuhar, correctionsAsar, correctionsMaghrib, correctionsMaghrib, correctionsIsha});
            }
        }
        Calendar instance = Calendar.getInstance();
        instance.set(1, hGDate.getYear());
        instance.set(2, hGDate.getMonth() - 1);
        instance.set(5, hGDate.getDay());
        ArrayList<String> prayerTimes = this.prayers.getPrayerTimes(instance, d, d2, this.timezone);
        prayerTimes.remove(4);
        return prayerTimes;
    }
}
