package com.raiadnan.quranreader.prayertimes.prayers.helper;

import android.content.Context;
import android.telephony.TelephonyManager;
import com.bumptech.glide.load.Key;
import com.raiadnan.quranreader.prayertimes.prayers.model.PrayerTimeModel;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AutoSettingJsonParse {
    private static final String ANGLEFAJR = "angle-fajr";
    private static final String ANGLEISHA = "angle-isha";
    private static final String CONVENTION = "convention";
    private static final String CONVENTIONINDEX = "convention_index";
    private static final String CONVENTION_POSITION = "convention_position";
    private static final String CORRECTIONS = "corrections";
    private static final String DST = "dst";
    private static final String HIJRI = "hijri";
    private static final String JSON_FILE_NAME = "QiblaAutoSettings.json";
    private static final String JURISTIC = "juristic";
    private static final String JURISTICINDEX = "juristic_index";

    public PrayerTimeModel getAutoSettings(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String simCountryIso = telephonyManager.getSimCountryIso();
        if (simCountryIso.equals("")) {
            simCountryIso = telephonyManager.getNetworkCountryIso();
        }
        String lowerCase = simCountryIso.trim().toLowerCase();
        PrayerTimeModel prayerTimeModel = new PrayerTimeModel();
        try {
            JSONObject jSONObject = new JSONObject(loadJSONFromAsset(context)).getJSONObject(lowerCase.toUpperCase());
            if (jSONObject != null) {
                if (jSONObject.has(ANGLEFAJR)) {
                    prayerTimeModel.setAngleFajr(Double.valueOf(jSONObject.getDouble(ANGLEFAJR)));
                }
                if (jSONObject.has(ANGLEISHA)) {
                    prayerTimeModel.setAngleFajr(Double.valueOf(jSONObject.getDouble(ANGLEISHA)));
                }
                JSONArray jSONArray = jSONObject.getJSONArray(CORRECTIONS);
                int[] iArr = new int[6];
                for (int i = 0; i < jSONArray.length(); i++) {
                    iArr[i] = jSONArray.getInt(i);
                }
                prayerTimeModel.setCorrections(iArr);
                prayerTimeModel.setConvention(jSONObject.getString(CONVENTION));
                prayerTimeModel.setConventionNumber(jSONObject.getInt(CONVENTIONINDEX));
                prayerTimeModel.setDst(jSONObject.getInt(DST));
                prayerTimeModel.setHijri(jSONObject.getInt(HIJRI));
                prayerTimeModel.setJuristicIndex(jSONObject.getInt(JURISTICINDEX));
                prayerTimeModel.setJuristic(jSONObject.getString(JURISTIC));
                prayerTimeModel.setConventionPosition(jSONObject.getInt(CONVENTION_POSITION));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return prayerTimeModel;
    }

    private String loadJSONFromAsset(Context context) {
        try {
            InputStream open = context.getAssets().open(JSON_FILE_NAME);
            byte[] bArr = new byte[open.available()];
            open.read(bArr);
            open.close();
            return new String(bArr, Key.STRING_CHARSET_NAME);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
