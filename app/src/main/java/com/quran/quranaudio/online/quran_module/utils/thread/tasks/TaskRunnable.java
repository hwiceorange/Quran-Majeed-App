package com.quran.quranaudio.online.quran_module.utils.thread.tasks;

import androidx.annotation.NonNull;

public interface TaskRunnable {
    void preExecute();

    void runTask() throws Exception;

    void onProgress(long currentLength, long totalLength);

    void onComplete();

    void onFailed(@NonNull Exception e);

    void postExecute();

    boolean isDone();

    void setDone(boolean done);
}
