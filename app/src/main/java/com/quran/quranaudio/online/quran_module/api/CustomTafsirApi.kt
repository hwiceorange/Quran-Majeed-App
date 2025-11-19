package com.quran.quranaudio.online.quran_module.api

import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirModel
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 自定义 Tafsir API 接口
 * 用于从 dochubai.com 服务器加载印尼语等自定义 Tafsir
 * 
 * Base URL: https://apis.dochubai.com/quran/apis/tafsirs/
 * 服务器目录: /public_html/quran/apis/tafsirs/
 */
interface CustomTafsirApi {
    
    /**
     * 获取指定经文的 Tafsir（注释）
     * 
     * 完整 URL 示例:
     * https://apis.dochubai.com/quran/apis/tafsirs/index.php?slug=id-tafsir-kemenag&ayah=1:1
     * 
     * @param slug Tafsir slug (例如: id-tafsir-kemenag)
     * @param ayahKey 经文键 (格式: surahId:ayahId, 例如: 1:1)
     * @return Map<String, TafsirModel> 键为 "tafsir"，值为 Tafsir 内容
     */
    @GET("index.php")
    suspend fun getTafsir(
        @Query("slug") slug: String,
        @Query("ayah") ayahKey: String
    ): Map<String, TafsirModel>
}

