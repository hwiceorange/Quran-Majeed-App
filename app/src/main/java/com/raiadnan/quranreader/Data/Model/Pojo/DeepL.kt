package com.raiadnan.quranreader.Data.Model.Pojo

import com.google.gson.annotations.SerializedName

data class DeepL(
    @SerializedName("translations")
    val translationResponse:List<Translation>
)
