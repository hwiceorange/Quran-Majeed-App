package com.quran.quranaudio.online.prayertimes.di.module;

import androidx.work.WorkerFactory;

import dagger.Binds;
import dagger.Module;


@Module
public abstract class WorkerFactoryModule {

    @Binds
    public abstract WorkerFactory bindWorkerFactory(WorkerFactory workerFactory);

}
