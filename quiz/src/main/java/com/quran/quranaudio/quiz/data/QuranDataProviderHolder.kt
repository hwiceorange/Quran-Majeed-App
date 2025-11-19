package com.quran.quranaudio.quiz.data

/**
 * 单例持有者 - 用于依赖注入
 * 
 * app 模块通过此类注入 QuranDataProvider 的实现
 */
object QuranDataProviderHolder {
    
    @Volatile
    private var instance: QuranDataProvider? = null
    
    /**
     * 设置实现（由 app 模块调用）
     */
    fun setInstance(provider: QuranDataProvider) {
        instance = provider
    }
    
    /**
     * 获取实现（由 quiz 模块调用）
     */
    fun getInstance(): QuranDataProvider {
        return instance ?: throw IllegalStateException(
            "QuranDataProvider not initialized! " +
            "Please call QuranDataProviderHolder.setInstance() in app module."
        )
    }
}

