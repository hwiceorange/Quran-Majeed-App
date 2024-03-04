package com.raiadnan.quranreader.Data.Model.Pojo

import com.google.gson.annotations.SerializedName

data class Verset(
    @SerializedName("number")
    val verseNumber:Int,
    @SerializedName("ayahs")
    val verse:List<Verse>
)
