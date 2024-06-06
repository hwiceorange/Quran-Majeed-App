package com.quran.quranaudio.online.prayertimes.di.factory.worker;

import android.content.Context;

import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;


public interface ChildWorkerFactory {

    ListenableWorker create(Context appContext, WorkerParameters workerParameters);
}
