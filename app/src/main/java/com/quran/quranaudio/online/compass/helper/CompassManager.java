package com.quran.quranaudio.online.compass.helper;

import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.R;


public class CompassManager {
    public static int getCompass() {
        int i = App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).getInt("compass", R.drawable.compass_1);
        if (i == R.drawable.compass_1 || i == R.drawable.compass_2 || i == R.drawable.compass_3 || i == R.drawable.compass_4 || i == R.drawable.compass_5) {
            return i;
        }
        return R.drawable.compass_1;
    }

    public static int getCompassK() {
        int i = App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).getInt("compass_k", R.drawable.compass_1);
        return (i == R.drawable.compass_1_k || i == R.drawable.compass_2_k || i == R.drawable.compass_3_k || i == R.drawable.compass_4_k || i == R.drawable.compass_5_k) ? i : R.drawable.compass_1_k;
    }

    public static void setCompass(int i) {
        App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).edit().putInt("compass", i).apply();
    }

    public static void setCompassK(int i) {
        App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).edit().putInt("compass_k", i).apply();
    }
}
