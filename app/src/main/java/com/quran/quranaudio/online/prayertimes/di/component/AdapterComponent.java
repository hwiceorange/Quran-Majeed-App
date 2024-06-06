package com.quran.quranaudio.online.prayertimes.di.component;



import com.quran.quranaudio.online.prayertimes.di.module.AppModule;
import com.quran.quranaudio.online.prayertimes.di.module.NetworkModule;

import javax.inject.Singleton;

import dagger.Component;


@Singleton
@Component(modules =
        {
                AppModule.class,
                NetworkModule.class
        })
public interface AdapterComponent {

}
