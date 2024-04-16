package com.quran.quranaudio.online.prayertimes.ui.timingtable.di;

import com.quran.quranaudio.online.prayertimes.ui.timingtable.TimingTableBaseFragment;

import dagger.Subcomponent;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Subcomponent(modules = {TimingTableModule.class})
public interface TimingTableComponent {

    @Subcomponent.Factory
    interface Factory {
        TimingTableComponent create();
    }

    void inject(TimingTableBaseFragment timingTableBaseFragment);
}
