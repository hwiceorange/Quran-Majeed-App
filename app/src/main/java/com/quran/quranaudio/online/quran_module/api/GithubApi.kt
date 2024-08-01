package com.quran.quranaudio.online.quran_module.api

import com.quran.quranaudio.online.quran_module.api.models.AppUpdate
import com.quran.quranaudio.online.quran_module.api.models.AppUrls
import com.quran.quranaudio.online.quran_module.api.models.ResourcesVersions
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface GithubApi {
    @GET("apis/versions/app_updates.json")
    suspend fun getAppUpdates(): List<AppUpdate>

    @GET("apis/versions/resources_versions.json")
    suspend fun getResourcesVersions(): ResourcesVersions

    @GET("apis/other/urls.json")
    suspend fun getAppUrls(): AppUrls

    @GET("api/translations/available_translations_info.json")
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
