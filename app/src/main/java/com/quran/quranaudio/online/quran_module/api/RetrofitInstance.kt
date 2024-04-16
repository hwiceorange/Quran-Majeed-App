package com.quran.quranaudio.online.quran_module.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.quran.quranaudio.online.quran_module.utils.Log
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit


@OptIn(ExperimentalSerializationApi::class)
object RetrofitInstance {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            Log.d(chain.request().url)
            return@addInterceptor chain.proceed(chain.request())
        }
        .build()

    val github: GithubApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.SHAHEEN_DEVELOPERS_URL)
            .addConverterFactory(
                JsonHelper.json.asConverterFactory("application/json".toMediaType())
            )
//            .client(client)
            .build()
            .create(GithubApi::class.java)
    }

    val quran: QuranApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.QURAN_API_ROOT_URL)
            .addConverterFactory(
                JsonHelper.json.asConverterFactory("application/json".toMediaType())
            )
//            .client(client)
            .build()
            .create(QuranApi::class.java)
    }
}
