package com.quran.quranaudio.online.prayertimes.ui.timingtable.di;


import androidx.lifecycle.ViewModel;

import com.quran.quranaudio.online.prayertimes.di.factory.viewmodel.ViewModelKey;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.TimingTableViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Module
public abstract class TimingTableModule {

    @Binds
    @IntoMap
    @ViewModelKey(TimingTableViewModel.class)
    public abstract ViewModel bindsTimingTableViewModel(TimingTableViewModel timingTableViewModel);
}
