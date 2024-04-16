package com.quran.quranaudio.online.prayertimes.di.module;

import androidx.work.WorkerFactory;

import dagger.Binds;
import dagger.Module;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
@Module
public abstract class WorkerFactoryModule {

    @Binds
    public abstract WorkerFactory bindWorkerFactory(WorkerFactory workerFactory);

}
