package com.quran.quranaudio.online.prayertimes.ui.home.di;

import com.quran.quranaudio.online.prayertimes.ui.home.HomeFragment;
import com.quran.quranaudio.online.prayertimes.ui.home.PrayersFragment;

import dagger.Subcomponent;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Subcomponent(modules = {HomeModule.class})
public interface HomeComponent {

    @Subcomponent.Factory
    interface Factory {
        HomeComponent create();
    }

    void inject(HomeFragment homeFragment);
    void inject(PrayersFragment prayersFragment);
}
