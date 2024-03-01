package com.quran.quranaudio.online.prayertimes.di.component;

import com.quran.quranaudio.online.prayertimes.di.module.AppModule;
import com.quran.quranaudio.online.prayertimes.di.module.NetworkModule;
import com.quran.quranaudio.online.prayertimes.ui.BaseActivity;
import com.quran.quranaudio.online.prayertimes.ui.DefaultActivity;
import com.quran.quranaudio.online.prayertimes.ui.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Author: le cheng
 * Whatsapp: +4407803311518
 * Email: lecheng2019@gmail.com
 */
@Singleton
@Component(modules =
        {
                AppModule.class,
                NetworkModule.class
        })
public interface DefaultComponent {

    void inject(MainActivity mainActivity);

    void inject(DefaultActivity defaultActivity);

    void inject(BaseActivity baseActivity);
}
