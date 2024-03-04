package com.raiadnan.quranreader.Utils

import android.util.Log
import com.raiadnan.quranreader.Data.Model.Api.ApiClient
import com.raiadnan.quranreader.Data.Model.Api.ApiInterface

object Common {
    private const val GOOGLE_API_URL = "https://maps.googleapis.com/"
    private const val DEEPL_API_URL = " https://api-free.deepl.com/"
    private const val QURAN_API_URL = "http://api.alquran.cloud/"
    var USER = "user"
    var USERS = "users"

    var TAG = "FirebaseAuthAppTag"


    fun logErrorMessage(errorMessage: String?) {
        Log.d(TAG, errorMessage.toString())
    }


    val googleApiService: ApiInterface
        get() = ApiClient.getClient(GOOGLE_API_URL).create(ApiInterface::class.java)

    val deeplApiService: ApiInterface
        get() = ApiClient.getClient(DEEPL_API_URL).create(ApiInterface::class.java)

    val frenchQuranApiService: ApiInterface
        get() = ApiClient.getClient(QURAN_API_URL).create(ApiInterface::class.java)
}