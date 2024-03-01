package com.quran.quranaudio.online.features.utils.extensions

object DoubleExtensions {
    fun Double.limitTo2Decimal():Double{
        return String.format("%.2f", this).toDouble()
    }

}