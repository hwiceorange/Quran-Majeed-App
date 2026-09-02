package com.quran.quranaudio.online.quran_module.utils.tafsir

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.quran.quranaudio.online.quran_module.api.RetrofitInstance
import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirModel
import com.quran.quranaudio.online.quran_module.utils.univ.FileUtils
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Tafsir 内存缓存管理器
 * 
 * 🚀 性能优化策略：
 * 1. 三级缓存：内存 → 文件 → 网络
 * 2. LRU 策略：最多缓存 50 个 Tafsir（约 5MB）
 * 3. 预加载：用户选择版本后，预加载常用章节
 * 4. 异步加载：不阻塞 UI 线程
 * 
 * 性能对比：
 * - 内存缓存：< 1ms
 * - 文件缓存：10-50ms
 * - 网络加载：2000-5000ms
 */
object TafsirCacheManager {
    private const val TAG = "TafsirCacheManager"
    private const val MAX_CACHE_SIZE = 50 // 最多缓存 50 个 Tafsir
    
    // LRU 内存缓存：key = "tafsirKey-chapterNo-verseNo", value = TafsirModel
    private val memoryCache = LruCache<String, TafsirModel>(MAX_CACHE_SIZE)
    
    // 预加载任务管理
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activePreloadJobs = mutableMapOf<String, Job>()
    private val inFlightLoads = TafsirInFlightRegistry<Result<TafsirModel>>()
    
    // JSON 解析器
    private val json = Json { ignoreUnknownKeys = true }

    enum class CacheSource {
        MEMORY,
        FILE,
        NETWORK
    }

    data class TafsirLoadResult(
        val tafsir: TafsirModel,
        val source: CacheSource
    )
    
    /**
     * 生成缓存 key
     */
    private fun getCacheKey(tafsirKey: String?, chapterNo: Int, verseNo: Int): String {
        return "${tafsirKey ?: "unknown"}-$chapterNo-$verseNo"
    }
    
    /**
     * 从缓存获取 Tafsir（三级缓存）
     * 
     * @return Tafsir 数据，null 表示缓存不存在
     */
    suspend fun getTafsir(
        context: Context,
        tafsirKey: String?,
        chapterNo: Int,
        verseNo: Int
    ): TafsirModel? = getCachedTafsir(context, tafsirKey, chapterNo, verseNo)?.tafsir

    private suspend fun getCachedTafsir(
        context: Context,
        tafsirKey: String?,
        chapterNo: Int,
        verseNo: Int
    ): TafsirLoadResult? = withContext(Dispatchers.IO) {
        val cacheKey = getCacheKey(tafsirKey, chapterNo, verseNo)
        val startTime = System.currentTimeMillis()
        
        // 🔥 第1级：内存缓存（最快，< 1ms）
        memoryCache.get(cacheKey)?.let {
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ [L1-内存] 命中缓存: $cacheKey (${elapsed}ms)")
            return@withContext TafsirLoadResult(it, CacheSource.MEMORY)
        }
        
        // 🔥 第2级：文件缓存（较快，10-50ms）
        try {
            val fileUtils = FileUtils.newInstance(context)
            val tafsirFile = fileUtils.getTafsirFileSingleVerse(tafsirKey, chapterNo, verseNo)
            
            if (tafsirFile.exists() && tafsirFile.length() > 0) {
                val tafsir = json.decodeFromString<TafsirModel>(tafsirFile.readText())
                
                // 写入内存缓存
                memoryCache.put(cacheKey, tafsir)
                
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ [L2-文件] 命中缓存: $cacheKey (${elapsed}ms)")
                return@withContext TafsirLoadResult(tafsir, CacheSource.FILE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [L2-文件] 读取失败: ${e.message}")
        }
        
        // 🔥 第3级：网络加载（最慢，1-5秒）
        Log.d(TAG, "⏳ [L3-网络] 缓存不存在，需要网络加载: $cacheKey")
        return@withContext null
    }

    /**
     * Returns the fastest available content and merges all concurrent network requests for the
     * same Tafsir/Surah/Ayah into one shared deferred request.
     */
    suspend fun getOrLoadTafsir(
        context: Context,
        tafsirKey: String,
        chapterNo: Int,
        verseNo: Int
    ): Result<TafsirLoadResult> {
        getCachedTafsir(context, tafsirKey, chapterNo, verseNo)?.let {
            return Result.success(it)
        }

        return loadAndCacheTafsir(context, tafsirKey, chapterNo, verseNo).map {
            TafsirLoadResult(it, CacheSource.NETWORK)
        }
    }
    
    /**
     * 从网络加载 Tafsir 并缓存
     */
    suspend fun loadAndCacheTafsir(
        context: Context,
        tafsirKey: String,
        chapterNo: Int,
        verseNo: Int
    ): Result<TafsirModel> {
        val cacheKey = getCacheKey(tafsirKey, chapterNo, verseNo)
        getCachedTafsir(context, tafsirKey, chapterNo, verseNo)?.let {
            return Result.success(it.tafsir)
        }

        val acquisition = inFlightLoads.getOrStart(cacheKey) {
            preloadScope.async {
                fetchAndCacheTafsir(context.applicationContext, tafsirKey, chapterNo, verseNo)
            }
        }
        Log.d(
            TAG,
            if (acquisition.started) "🚀 [SingleFlight] started: $cacheKey"
            else "🤝 [SingleFlight] joined: $cacheKey"
        )
        return acquisition.deferred.await()
    }

    private suspend fun fetchAndCacheTafsir(
        context: Context,
        tafsirKey: String,
        chapterNo: Int,
        verseNo: Int
    ): Result<TafsirModel> = withContext(Dispatchers.IO) {
        val cacheKey = getCacheKey(tafsirKey, chapterNo, verseNo)
        val startTime = System.currentTimeMillis()

        val slug = resolveTafsirRequestSlug(
            TafsirUtils.getTafsirSlugFromKey(tafsirKey),
            tafsirKey
        )
            ?: return@withContext Result.failure(IllegalArgumentException("Blank Tafsir key"))

        var lastError: Throwable? = null
        repeat(MAX_NETWORK_ATTEMPTS) { zeroBasedAttempt ->
            val attempt = zeroBasedAttempt + 1
            try {
                val response = when {
                    slug.startsWith("id-") -> {
                        Log.d(TAG, "📥 [网络] 从自定义服务器加载: $slug, attempt=$attempt")
                        RetrofitInstance.customTafsir.getTafsir(slug, "$chapterNo:$verseNo")
                    }
                    else -> {
                        Log.d(TAG, "📥 [网络] 从 Quran.com 加载: $slug, attempt=$attempt")
                        RetrofitInstance.quran.getTafsir(slug, "$chapterNo:$verseNo")
                    }
                }
                val tafsir = response["tafsir"]
                    ?.takeIf { it.text.isNotBlank() }
                    ?: throw IOException("Tafsir response did not contain readable content")

                val fileUtils = FileUtils.newInstance(context)
                val tafsirFile = fileUtils.getTafsirFileSingleVerse(tafsirKey, chapterNo, verseNo)
                fileUtils.createFile(tafsirFile)
                tafsirFile.writeText(json.encodeToString(TafsirModel.serializer(), tafsir))
                memoryCache.put(cacheKey, tafsir)

                Log.d(TAG, "✅ [网络] 加载并缓存成功: $cacheKey (${System.currentTimeMillis() - startTime}ms), attempt=$attempt")
                return@withContext Result.success(tafsir)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                lastError = error
                val retry = attempt < MAX_NETWORK_ATTEMPTS && isRetryableFetchFailure(error)
                Log.e(TAG, "❌ [网络] 加载失败: ${error.message}, attempt=$attempt, retry=$retry")
                if (!retry) {
                    return@withContext Result.failure(error)
                }
                delay(NETWORK_RETRY_DELAY_MS)
            }
        }
        Result.failure(lastError ?: IOException("Tafsir request failed"))
    }

    private fun isRetryableFetchFailure(error: Exception): Boolean = when (error) {
        is IOException -> true
        is SerializationException -> true
        is HttpException -> error.code() == 408 || error.code() == 429 || error.code() >= 500
        else -> false
    }

    private const val MAX_NETWORK_ATTEMPTS = 2
    private const val NETWORK_RETRY_DELAY_MS = 450L

    fun prefetchVerse(context: Context, tafsirKey: String?, chapterNo: Int, verseNo: Int) {
        if (tafsirKey.isNullOrBlank() || chapterNo < 1 || verseNo < 1) return
        preloadScope.launch {
            getOrLoadTafsir(context.applicationContext, tafsirKey, chapterNo, verseNo)
        }
    }

    fun prefetchAdjacent(
        context: Context,
        tafsirKey: String,
        chapterNo: Int,
        verseNo: Int,
        verseCount: Int
    ) {
        listOf(verseNo - 1, verseNo + 1)
            .filter { it in 1..verseCount }
            .forEach { adjacentVerse ->
                prefetchVerse(context, tafsirKey, chapterNo, adjacentVerse)
            }
    }
    
    /**
     * 预加载常用章节的 Tafsir
     * 
     * 策略：
     * 1. 短章节（Juz 30）：第 78-114 章（前3节）
     * 2. 常读章节：第 1, 2, 18, 36, 67 章
     * 3. 用户最近阅读的章节
     */
    fun preloadCommonTafsirs(
        context: Context,
        tafsirKey: String,
        recentChapters: List<Int> = emptyList()
    ) {
        Log.d(TAG, "🚀 开始预加载 Tafsir: $tafsirKey")
        
        // 取消之前的预加载任务
        activePreloadJobs[tafsirKey]?.cancel()
        
        // 定义预加载章节
        val commonChapters = listOf(
            1,   // Al-Fatihah (完整)
            2,   // Al-Baqarah (仅前10节)
            18,  // Al-Kahf (前5节)
            36,  // Ya-Sin (前5节)
            67,  // Al-Mulk (前5节)
        ) + (78..114).toList() + recentChapters.take(5) // Juz 30 + 最近阅读
        
        // 启动预加载任务（低优先级，不影响用户操作）
        val job = preloadScope.launch {
            var loaded = 0
            var cached = 0
            var failed = 0
            
            for (chapterNo in commonChapters.distinct()) {
                try {
                    // 获取该章节的经文数量
                    val verseCount = getVerseCount(chapterNo)
                    val maxVerses = when (chapterNo) {
                        1 -> 7      // Al-Fatihah 完整
                        2 -> 10     // Al-Baqarah 前10节
                        in 78..114 -> minOf(verseCount, 3)  // Juz 30 前3节
                        else -> minOf(verseCount, 5)  // 其他章节前5节
                    }
                    
                    for (verseNo in 1..maxVerses) {
                        // 检查是否已缓存
                        val existing = getTafsir(context, tafsirKey, chapterNo, verseNo)
                        if (existing != null) {
                            cached++
                            continue
                        }
                        
                        // 加载并缓存
                        val result = loadAndCacheTafsir(context, tafsirKey, chapterNo, verseNo)
                        if (result.isSuccess) {
                            loaded++
                            Log.d(TAG, "✅ 预加载成功: $chapterNo:$verseNo (已加载: $loaded, 已缓存: $cached)")
                        } else {
                            failed++
                        }
                        
                        // 间隔 200ms，避免过度占用网络
                        delay(200)
                        
                        // 检查任务是否被取消
                        if (!isActive) {
                            Log.d(TAG, "⚠️ 预加载任务已取消")
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 预加载章节 $chapterNo 失败: ${e.message}")
                    failed++
                }
            }
            
            Log.d(TAG, "✅ 预加载完成: 新加载 $loaded, 已缓存 $cached, 失败 $failed")
        }
        
        activePreloadJobs[tafsirKey] = job
    }
    
    /**
     * 获取章节的经文数量（简化版本）
     */
    private fun getVerseCount(chapterNo: Int): Int {
        // 简化版本：可以从 QuranMeta 获取精确值
        return when (chapterNo) {
            1 -> 7
            2 -> 286
            3 -> 200
            4 -> 176
            5 -> 120
            6 -> 165
            7 -> 206
            18 -> 110
            36 -> 83
            67 -> 30
            else -> if (chapterNo >= 78) {
                // Juz 30 章节通常较短
                when (chapterNo) {
                    78 -> 40; 79 -> 46; 80 -> 42; 81 -> 29; 82 -> 19
                    83 -> 36; 84 -> 25; 85 -> 22; 86 -> 17; 87 -> 19
                    88 -> 26; 89 -> 30; 90 -> 20; 91 -> 15; 92 -> 21
                    93 -> 11; 94 -> 8; 95 -> 8; 96 -> 19; 97 -> 5
                    98 -> 8; 99 -> 8; 100 -> 11; 101 -> 11; 102 -> 8
                    103 -> 3; 104 -> 9; 105 -> 5; 106 -> 4; 107 -> 7
                    108 -> 3; 109 -> 6; 110 -> 3; 111 -> 5; 112 -> 4
                    113 -> 5; 114 -> 6
                    else -> 20
                }
            } else 100 // 其他章节估算值
        }
    }
    
    /**
     * 清除内存缓存
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
        Log.d(TAG, "🗑️ 内存缓存已清除")
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): String {
        return "内存缓存: ${memoryCache.size()}/$MAX_CACHE_SIZE"
    }
    
    /**
     * 取消所有预加载任务
     */
    fun cancelAllPreloading() {
        activePreloadJobs.values.forEach { it.cancel() }
        activePreloadJobs.clear()
        Log.d(TAG, "🛑 所有预加载任务已取消")
    }
}
