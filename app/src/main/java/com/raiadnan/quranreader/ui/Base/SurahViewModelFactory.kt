package com.raiadnan.quranreader.ui.Base

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.raiadnan.quranreader.ui.Main.ViewModel.SurahViewModel

class SurahViewModelFactory constructor( application: Application) : ViewModelProvider.Factory  {

    val mApplication = application

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SurahViewModel::class.java)) {
            return SurahViewModel(mApplication) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}