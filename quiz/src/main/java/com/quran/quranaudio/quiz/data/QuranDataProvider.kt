package com.quran.quranaudio.quiz.data

/**
 * 为Quiz模块提供古兰经数据的接口
 * 
 * 由 app 模块实现，quiz 模块通过此接口获取数据
 */
interface QuranDataProvider {
    
    /**
     * 获取经文数据
     * 
     * @param surahId 章节号 (1-114)
     * @param ayahId Ayah号
     * @return 经文数据，如果失败返回null
     */
    fun getVerseData(surahId: Int, ayahId: Int): QuizVerseData?
    
    /**
     * 检查古兰经数据是否已初始化
     */
    fun isQuranDataReady(): Boolean
    
    /**
     * 初始化古兰经数据（如果未初始化）
     * 
     * @param onComplete 初始化完成回调
     */
    fun ensureQuranDataInitialized(onComplete: () -> Unit)
}

/**
 * Quiz模块使用的经文数据类
 * 
 * 简单、稳定、只包含必要字段
 */
data class QuizVerseData(
    val surahId: Int,
    val ayahId: Int,
    val arabicText: String,
    val translationText: String
)

