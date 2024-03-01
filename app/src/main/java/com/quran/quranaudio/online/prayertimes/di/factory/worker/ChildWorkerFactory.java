package com.quran.quranaudio.online.prayertimes.di.factory.worker;

import android.content.Context;

import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

/**
 * Author: le cheng
 * Whatsapp: +4407803311518
 * Email: lecheng2019@gmail.com
 */
public interface ChildWorkerFactory {

    ListenableWorker create(Context appContext, WorkerParameters workerParameters);
}
