package com.quran.quranaudio.online.prayertimes.di.component;

import com.quran.quranaudio.online.prayertimes.di.module.AppModule;
import com.quran.quranaudio.online.prayertimes.di.module.NetworkModule;
import com.quran.quranaudio.online.prayertimes.di.module.WidgetModule;
import com.quran.quranaudio.online.prayertimes.notifier.NotificationDismissedReceiver;
import com.quran.quranaudio.online.prayertimes.notifier.NotifierActionReceiver;
import com.quran.quranaudio.online.prayertimes.notifier.NotifierReceiver;
import com.quran.quranaudio.online.prayertimes.notifier.ReminderReceiver;
import com.quran.quranaudio.online.prayertimes.notifier.SilenterReceiver;

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
                WidgetModule.class,
                NetworkModule.class
        })
public interface ReceiverComponent {

    void inject(NotificationDismissedReceiver notificationDismissedReceiver);

    void inject(NotifierActionReceiver notifierActionReceiver);

    void inject(NotifierReceiver notifierReceiver);

    void inject(ReminderReceiver beforeAdhanAlertReceiver);

    void inject(SilenterReceiver silenterReceiver);

}
