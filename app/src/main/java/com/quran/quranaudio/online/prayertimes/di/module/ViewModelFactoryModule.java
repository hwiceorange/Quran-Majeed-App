package com.quran.quranaudio.online.prayertimes.di.module;

import androidx.lifecycle.ViewModelProvider;

import com.quran.quranaudio.online.prayertimes.di.factory.viewmodel.ViewModelProviderFactory;

import dagger.Binds;
import dagger.Module;


@Module
public abstract class ViewModelFactoryModule {


    @Binds
    public abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelProviderFactory viewModelProviderFactory);

}
