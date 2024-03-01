package com.quran.quranaudio.online.prayertimes.di.component;

import com.quran.quranaudio.online.prayertimes.di.module.AppModule;
import com.quran.quranaudio.online.prayertimes.di.module.NetworkModule;
import com.quran.quranaudio.online.prayertimes.di.module.SubcomponentsModule;
import com.quran.quranaudio.online.prayertimes.di.module.ViewModelFactoryModule;
import com.quran.quranaudio.online.prayertimes.di.module.WorkerBindingModule;
import com.quran.quranaudio.online.prayertimes.ui.calendar.di.CalendarComponent;
import com.quran.quranaudio.online.prayertimes.ui.home.di.HomeComponent;
import com.quran.quranaudio.online.prayertimes.ui.settings.di.SettingsComponent;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.di.TimingTableComponent;
import com.quran.quranaudio.online.prayertimes.di.factory.worker.WorkerProviderFactory;

import javax.inject.Singleton;

import dagger.Component;


@Singleton
@Component(modules =
        {
                AppModule.class,
                NetworkModule.class,
                ViewModelFactoryModule.class,
                WorkerBindingModule.class,
                SubcomponentsModule.class
        })
public interface ApplicationComponent {

    HomeComponent.Factory homeComponent();


    TimingTableComponent.Factory timingTableComponent();

    CalendarComponent.Factory calendarComponent();


    SettingsComponent.Factory settingsComponent();

    WorkerProviderFactory workerProviderFactory();
}
