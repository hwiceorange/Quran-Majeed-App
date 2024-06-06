package com.quran.quranaudio.online.prayertimes.ui.home.di;


import androidx.lifecycle.ViewModel;

import com.quran.quranaudio.online.prayertimes.di.factory.viewmodel.ViewModelKey;
import com.quran.quranaudio.online.prayertimes.ui.home.HomeViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;


@Module
public abstract class HomeModule {

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel.class)
    public abstract ViewModel bindsHomeViewModel(HomeViewModel homeViewModel);
}
