package com.raiadnan.quranreader.tasbih.helper;


import com.raiadnan.quranreader.prayertimes.App;

public class TasbihManager {
    private static TasbihManager tasbihManager;

    public static TasbihManager get() {
        if (tasbihManager == null) {
            tasbihManager = new TasbihManager();
        }
        return tasbihManager;
    }

    public int getSpeak() {
        return App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).getInt("speak", 0);
    }

    public void putSpeak(int i) {
        App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).edit().putInt("speak", i).apply();
    }

    public boolean is33() {
        return App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).getBoolean("33", true);
    }

    public void put33(boolean z) {
        App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).edit().putBoolean("33", z).apply();
    }

    public int getTotal() {
        return App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).getInt("total", 0);
    }

    public void putTotal(int i) {
        App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).edit().putInt("total", i).apply();
    }
}
