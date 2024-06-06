package com.quran.quranaudio.online.prayertimes.ui.calendar.di;

import com.quran.quranaudio.online.prayertimes.ui.calendar.CalendarActivity;

import dagger.Subcomponent;


@Subcomponent
public interface CalendarComponent {

    @Subcomponent.Factory
    interface Factory {
        CalendarComponent create();
    }

    void inject(CalendarActivity calendarActivity);
}
