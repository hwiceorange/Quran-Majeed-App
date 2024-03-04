package com.raiadnan.quranreader.Data.Repository

import com.raiadnan.quranreader.Data.database.QuranDao
import com.raiadnan.quranreader.Data.database.entities.Ayah

class AyahRepository(private val quranDao: QuranDao) {

   /* val service = Common.frenchQuranApiService
    var _translation = MutableLiveData<List<Verse>>()
    val translation: LiveData<List<Verse>>
        get() = _translation*/

    fun getAyah(surahNumber: Int): List<Ayah> {
        return quranDao.getAyah(surahNumber)
    }

    fun updateAyah(text:String,ayahId:Long){
        quranDao.updateAyah(text,ayahId)
    }

    fun getRandomAyah(surahNumber: Int):Ayah{
        return quranDao.getRandomAyah(surahNumber)
    }

   /* fun getFrenchSurah(ayahId: Int) {
        service.getFrenchSurah(ayahId).enqueue(object : Callback<Root> {
            override fun onResponse(call: Call<Root>, response: Response<Root>) {
                if (response.code() == 200) {
                    _translation.value = response.body()!!.data.verse
                   *//* GlobalScope.launch(Dispatchers.IO){
                        quranDao.updateAyah(response.body()!!.data.verse,ayahId)
                    }*//*

                    Log.d("Ayah", "OnResponse OK : " + response.body()!!.data.verse + " "+ayahId)
                } else {

                    Log.d("Ayah", "OnResponse Fail : ")
                }
            }

            override fun onFailure(call: Call<Root>, t: Throwable) {

                Log.d("Ayah", "OnResponse Fail : ")
            }

        })
    }*/
}