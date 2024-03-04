package com.raiadnan.quranreader.ui.Base

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.raiadnan.quranreader.Data.Repository.ChapterRepository
import com.raiadnan.quranreader.ui.Main.ViewModel.ChapterViewModel

class ChapterViewModelFactory(application: Application,chapterRepository: ChapterRepository)
    :ViewModelProvider.Factory {

    val mApplication = application
    val repository = chapterRepository

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChapterViewModel::class.java)) {
            return ChapterViewModel(mApplication,repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}