package com.raiadnan.quranreader.Data.Model.Pojo

import com.google.gson.annotations.SerializedName

data class Translation(
    @SerializedName("detected_source_language")
    val srcLg:String,
    @SerializedName("text")
    val textTranslation:String
)
