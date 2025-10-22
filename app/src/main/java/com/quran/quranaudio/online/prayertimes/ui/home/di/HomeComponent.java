package com.quran.quranaudio.online.prayertimes.ui.home.di;

import com.quran.quranaudio.online.prayertimes.ui.home.HomeFragment;
import com.quran.quranaudio.online.prayertimes.ui.home.PrayersFragment;
import com.quran.quranaudio.online.quran_module.frags.main.FragMain;

import dagger.Subcomponent;


@Subcomponent(modules = {HomeModule.class})
public interface HomeComponent {

    @Subcomponent.Factory
    interface Factory {
        HomeComponent create();
    }

    void inject(HomeFragment homeFragment);
    void inject(PrayersFragment prayersFragment);
    void inject(FragMain fragMain);  // Add support for main home page
    
    // Provide PrayerDataPreloader for background data preload
    com.quran.quranaudio.online.prayertimes.ui.PrayerDataPreloader getPrayerDataPreloader();
}
