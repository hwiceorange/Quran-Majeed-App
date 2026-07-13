package com.quran.quranaudio.online.quran_module.api

import com.quran.quranaudio.online.quran_module.api.models.AppUpdate
import com.quran.quranaudio.online.quran_module.api.models.AppUrls
import com.quran.quranaudio.online.quran_module.api.models.ResourcesVersions
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GithubApi {
    @GET("apis/versions/app_updates.json")
    suspend fun getAppUpdates(): List<AppUpdate>

    @GET("apis/versions/resources_versions.json")
    suspend fun getResourcesVersions(): ResourcesVersions

    @GET("apis/other/urls.json")
    suspend fun getAppUrls(): AppUrls

    @GET("apis/translations/available_translations_info.json")
    suspend fun getAvailableTranslations(): ResponseBody

    @GET("{path}")
    @Streaming
    suspend fun getTranslation(@Path("path") path: String):  Response<ResponseBody>

    @GET("apis/quran_scripts/{filename}")
    @Streaming
    suspend fun getQuranScript(@Path("filename") filename: String): Response<ResponseBody>

    @GET("apis/fonts/{scriptKey}/{part}")
    @Streaming
    suspend fun getKFQPCFont(
        @Path("scriptKey") scriptKey: String,
        @Path("part") part: String
    ): Response<ResponseBody>

    @GET("apis/recitations/available_recitations_info.json")
    suspend fun getAvailableRecitations(): ResponseBody

    @GET("apis/recitations/available_recitation_translations_info.json")
    suspend fun getAvailableRecitationTranslations(): ResponseBody

    @GET("apis/tafsirs/available_tafsirs_info.json")
    suspend fun getAvailableTafsirs(): ResponseBody
}

/**
 * 🌐 Quran Foundation API (备用API)
 * 用于获取古兰经翻译版本和经文内容
 */
interface QuranFoundationApi {
    /**
     * 获取所有可用的翻译版本
     * https://api.quran.com/api/v4/resources/translations
     */
    @GET("api/v4/resources/translations")
    suspend fun getTranslations(@Query("language") language: String? = null): ResponseBody
    
    /**
     * 获取特定翻译版本的经文
     * https://api.quran.com/api/v4/quran/translations/{translation_id}
     */
    @GET("api/v4/quran/translations/{translation_id}")
    suspend fun getQuranTranslation(@Path("translation_id") translationId: Int): ResponseBody

    /**
     * 逐词翻译：获取某节经文的逐词数据(阿拉伯文 + 词义 + 转写)。
     * ⚠️ 词义语言参数是 language(不是 word_translation_language，后者会被忽略、静默回退英文)。
     * https://api.quran.com/api/v4/verses/by_key/2:255?words=true&language=ur
     */
    @GET("api/v4/verses/by_key/{verse_key}")
    suspend fun getVerseWords(
        @Path("verse_key") verseKey: String,
        @Query("words") words: Boolean = true,
        @Query("language") language: String = "en",
        @Query("word_fields") wordFields: String = "text_uthmani,transliteration"
    ): ResponseBody
}
