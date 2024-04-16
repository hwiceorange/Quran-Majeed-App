package com.quran.quranaudio.online.prayertimes.di.factory.worker;

import android.content.Context;

import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public interface ChildWorkerFactory {

    ListenableWorker create(Context appContext, WorkerParameters workerParameters);
}
