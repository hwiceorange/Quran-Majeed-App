package com.raiadnan.quranreader.ui.Main.ViewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raiadnan.quranreader.Data.Repository.ChapterRepository
import com.raiadnan.quranreader.Data.database.entities.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ChapterViewModel(val application: Application,val repository: ChapterRepository):ViewModel() {
    private var _chapters = MutableLiveData<List<Chapter>>()
    val chapters: LiveData<List<Chapter>>
        get() = _chapters


    fun fetchChapters(categoryId:Int){
        viewModelScope.launch(Dispatchers.IO){
           val list =  repository.getChapters(categoryId)
            Timber.d("list$list")
            _chapters.postValue(list)
        }
    }
}