package com.quran.quranaudio.online.prayertimes.ui.calendar.di;

import com.quran.quranaudio.online.prayertimes.ui.calendar.CalendarActivity;

import dagger.Subcomponent;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Subcomponent
public interface CalendarComponent {

    @Subcomponent.Factory
    interface Factory {
        CalendarComponent create();
    }

    void inject(CalendarActivity calendarActivity);
}
