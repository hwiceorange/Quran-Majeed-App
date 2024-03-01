package com.quran.quranaudio.online.prayertimes.di.module;


import com.quran.quranaudio.online.prayertimes.ui.calendar.di.CalendarComponent;
import com.quran.quranaudio.online.prayertimes.ui.home.di.HomeComponent;
import com.quran.quranaudio.online.prayertimes.ui.settings.di.SettingsComponent;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.di.TimingTableComponent;

import dagger.Module;


@Module(subcomponents = {
        HomeComponent.class,
        TimingTableComponent.class,
    //    QiblaComponent.class,
        CalendarComponent.class,
    //    QuranComponent.class,
        SettingsComponent.class
})
public class SubcomponentsModule {

}
