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
            val request = chain.request()
            Log.d("🌐 API_REQUEST: ${request.method} ${request.url}")
            
            try {
                val response = chain.proceed(request)
                Log.d("✅ API_RESPONSE: ${response.code} ${request.url}")
                return@addInterceptor response
            } catch (ex: Exception) {
                android.util.Log.e("API_ERROR", "❌ ${ex.message}", ex)
                throw ex
            }
        }
        .build()

    val github: GithubApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.SHAHEEN_DEVELOPERS_URL)
            .addConverterFactory(
                JsonHelper.json.asConverterFactory("application/json".toMediaType())
            )
            .client(client)  // ✅ 启用日志拦截器
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
    
    /**
     * 🌐 Quran Foundation API (备用API)
     * 用于获取古兰经翻译版本和经文内容
     */
    val quranFoundation: QuranFoundationApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.quran.com/")
            .addConverterFactory(
                JsonHelper.json.asConverterFactory("application/json".toMediaType())
            )
            .client(client)
            .build()
            .create(QuranFoundationApi::class.java)
    }
}
