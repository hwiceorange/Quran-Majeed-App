package com.raiadnan.quranreader.prayertimes.prayers.helper;

import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.model.Tone;
import java.util.ArrayList;

public class ToneHelper {
    private static ToneHelper toneHelper;

    public int getToneResoureFromPosition(int i) {
        switch (i) {
            case 2:
                return 0;
            case 3:
                return R.raw.adhan_fajr_madina;
            case 4:
                return R.raw.adhan_fajr_madina;
            case 5:
                return R.raw.adhan_fajr_madina;
            case 6:
                return R.raw.adhan_fajr_madina;
            case 7:
                return R.raw.adhan_fajr_madina;
            case 8:
                return R.raw.adhan_fajr_madina;
            case 9:
                return R.raw.adhan_fajr_madina;
            default:
                return 0;
        }
    }

    public String getToneStringFromPosition(int i) {
        switch (i) {
            case 0:
                return "None";
            case 1:
                return "Mute";
            case 2:
                return "Default";
            case 3:
                return "Adhan Fajr Madina";
            case 4:
                return "Adhan Madina";
            case 5:
                return "Most Popular Adhan";
            case 6:
                return "Adhan Nasir Al-Qatami";
            case 7:
                return "Adhan Mansur Al-Zahrani";
            case 8:
                return "Mishary Rashid Al-Afsay";
            case 9:
                return "Adhan from Egypt";
            default:
                return "None";
        }
    }

    public static ToneHelper get() {
        if (toneHelper == null) {
            toneHelper = new ToneHelper();
        }
        return toneHelper;
    }

    public ArrayList<Tone> getTones() {
        ArrayList<Tone> arrayList = new ArrayList<>();
        arrayList.add(new Tone(R.drawable.ic_none, "None"));
        arrayList.add(new Tone(R.drawable.ic_mute, "Mute"));
        arrayList.add(new Tone(R.drawable.ic_muusic, "Default"));
        arrayList.add(new Tone(R.drawable.ic_muusic, "Adhan Fajr Madina"));
        arrayList.add(new Tone(R.drawable.ic_muusic, "Adhan Madina"));
        arrayList.add(new Tone(R.drawable.ic_muusic, "Most Popular Adhan"));
        arrayList.add(new Tone(R.drawable.ic_muusic, "Adhan Nasir Al-Qatami"));
        arrayList.add(new Tone(R.drawable.ic_muusic, "Adhan Mansur Al-Zahrani"));
        arrayList.add(new Tone(R.drawable.ic_muusic, "Mishary Rashid Al-Afsay"));
        arrayList.add(new Tone(R.drawable.ic_muusic, "Adhan from Egypt"));
        return arrayList;
    }

    public int getToneIconFromKey(String str) {
        switch (PrayerTimeSettingsPref.get().getTone(str)) {
            case 0:
                return R.drawable.ic_none;
            case 1:
                return R.drawable.ic_mute;
            default:
                return R.drawable.ic_muusic;
        }
    }
}
