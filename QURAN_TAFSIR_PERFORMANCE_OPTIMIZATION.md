# 🚀 古兰经与经文注释性能优化方案

## 📋 问题分析

### 当前状况

**问题**: 英语、乌尔都语、阿语古兰经与经文注释每次打开会有 **3-10 秒的延迟**

**原因分析**:

1. **文件 I/O 阻塞** (1-2 秒):
   ```kotlin
   // 每次都读取文件
   if (tafsirFile.length() > 0) {  // I/O 操作
       val read = tafsirFile.readText()  // I/O 操作，可能很慢
       val tafsir = JsonHelper.json.decodeFromString<TafsirModel>(read)  // JSON 解析
   }
   ```

2. **网络请求延迟** (2-5 秒):
   ```kotlin
   // 首次加载或缓存不存在时，需要网络请求
   RetrofitInstance.quran.getTafsir(slug, "$chapterNo:$verseNo")  // 网络延迟
   ```

3. **WebView 渲染延迟** (1-2 秒):
   ```kotlin
   // HTML 模板替换和 WebView 渲染
   binding.webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "utf-8", null)
   ```

4. **无内存缓存**: 每次打开都重新从文件加载，即使是同一章节

5. **无预加载机制**: 用户选择语言后，没有预先加载常用内容

### 目标

- ✅ **本地资源秒开** (< 500ms)
- ✅ **服务端资源预加载后秒开** (< 500ms)
- ✅ **首次加载优化** (< 2秒)

---

## 🛠️ 优化方案

### 阶段 1: 内存缓存层（立即生效）

#### 1.1 创建 Tafsir 内存缓存管理器

**文件**: `app/src/main/java/com/quran/quranaudio/online/quran_module/utils/tafsir/TafsirCacheManager.kt`

```kotlin
package com.quran.quranaudio.online.quran_module.utils.tafsir

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.quran.quranaudio.online.quran_module.api.RetrofitInstance
import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirModel
import com.quran.quranaudio.online.quran_module.utils.univ.FileUtils
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Tafsir 内存缓存管理器
 * 
 * 🚀 性能优化策略：
 * 1. 三级缓存：内存 → 文件 → 网络
 * 2. LRU 策略：最多缓存 50 个 Tafsir（约 5MB）
 * 3. 预加载：用户选择版本后，预加载常用章节
 * 4. 异步加载：不阻塞 UI 线程
 */
object TafsirCacheManager {
    private const val TAG = "TafsirCacheManager"
    private const val MAX_CACHE_SIZE = 50 // 最多缓存 50 个 Tafsir
    
    // LRU 内存缓存：key = "tafsirKey-chapterNo-verseNo", value = TafsirModel
    private val memoryCache = LruCache<String, TafsirModel>(MAX_CACHE_SIZE)
    
    // 预加载任务管理
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activePreloadJobs = mutableMapOf<String, Job>()
    
    /**
     * 生成缓存 key
     */
    private fun getCacheKey(tafsirKey: String, chapterNo: Int, verseNo: Int): String {
        return "$tafsirKey-$chapterNo-$verseNo"
    }
    
    /**
     * 从缓存获取 Tafsir（三级缓存）
     * 
     * @return Tafsir 数据，null 表示缓存不存在
     */
    suspend fun getTafsir(
        context: Context,
        tafsirKey: String,
        chapterNo: Int,
        verseNo: Int
    ): TafsirModel? = withContext(Dispatchers.IO) {
        val cacheKey = getCacheKey(tafsirKey, chapterNo, verseNo)
        val startTime = System.currentTimeMillis()
        
        // 🔥 第1级：内存缓存（最快，< 1ms）
        memoryCache.get(cacheKey)?.let {
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ [L1-内存] 命中缓存: $cacheKey (${elapsed}ms)")
            return@withContext it
        }
        
        // 🔥 第2级：文件缓存（较快，10-50ms）
        try {
            val fileUtils = FileUtils.newInstance(context)
            val tafsirFile = fileUtils.getTafsirFileSingleVerse(tafsirKey, chapterNo, verseNo)
            
            if (tafsirFile.exists() && tafsirFile.length() > 0) {
                val tafsir = Json.decodeFromString<TafsirModel>(tafsirFile.readText())
                
                // 写入内存缓存
                memoryCache.put(cacheKey, tafsir)
                
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ [L2-文件] 命中缓存: $cacheKey (${elapsed}ms)")
                return@withContext tafsir
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [L2-文件] 读取失败: ${e.message}")
        }
        
        // 🔥 第3级：网络加载（最慢，1-5秒）
        Log.d(TAG, "⏳ [L3-网络] 缓存不存在，需要网络加载: $cacheKey")
        return@withContext null
    }
    
    /**
     * 从网络加载 Tafsir 并缓存
     */
    suspend fun loadAndCacheTafsir(
        context: Context,
        tafsirKey: String,
        chapterNo: Int,
        verseNo: Int
    ): Result<TafsirModel> = withContext(Dispatchers.IO) {
        val cacheKey = getCacheKey(tafsirKey, chapterNo, verseNo)
        val startTime = System.currentTimeMillis()
        
        try {
            val slug = TafsirUtils.getTafsirSlugFromKey(tafsirKey)
            
            // 从网络加载
            val tafsir = when {
                slug.startsWith("id-") -> {
                    Log.d(TAG, "📥 [网络] 从自定义服务器加载: $slug")
                    RetrofitInstance.customTafsir.getTafsir(slug, "$chapterNo:$verseNo")["tafsir"]!!
                }
                else -> {
                    Log.d(TAG, "📥 [网络] 从 Quran.com 加载: $slug")
                    RetrofitInstance.quran.getTafsir(slug, "$chapterNo:$verseNo")["tafsir"]!!
                }
            }
            
            // 保存到文件缓存
            val fileUtils = FileUtils.newInstance(context)
            val tafsirFile = fileUtils.getTafsirFileSingleVerse(tafsirKey, chapterNo, verseNo)
            fileUtils.createFile(tafsirFile)
            tafsirFile.writeText(Json.encodeToString(TafsirModel.serializer(), tafsir))
            
            // 保存到内存缓存
            memoryCache.put(cacheKey, tafsir)
            
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ [网络] 加载并缓存成功: $cacheKey (${elapsed}ms)")
            
            Result.success(tafsir)
        } catch (e: Exception) {
            Log.e(TAG, "❌ [网络] 加载失败: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 预加载常用章节的 Tafsir
     * 
     * 策略：
     * 1. 短章节（Juz 30）：第 78-114 章
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
            1,   // Al-Fatihah
            2,   // Al-Baqarah (仅前10节)
            18,  // Al-Kahf
            36,  // Ya-Sin
            67,  // Al-Mulk
        ) + (78..114).toList() + recentChapters.take(5)
        
        // 启动预加载任务
        val job = preloadScope.launch {
            var loaded = 0
            var cached = 0
            var failed = 0
            
            for (chapterNo in commonChapters.distinct()) {
                try {
                    // 获取该章节的经文数量
                    val verseCount = getVerseCount(chapterNo)
                    val maxVerses = if (chapterNo == 2) 10 else minOf(verseCount, 5) // 长章节只预加载前5节
                    
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
     * 获取章节的经文数量
     */
    private fun getVerseCount(chapterNo: Int): Int {
        // 简化版本：可以从 QuranMeta 获取
        return when (chapterNo) {
            1 -> 7
            2 -> 286
            18 -> 110
            36 -> 83
            67 -> 30
            else -> if (chapterNo >= 78) 50 else 100 // 估算值
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
     * 取消所有预加载任务
     */
    fun cancelAllPreloading() {
        activePreloadJobs.values.forEach { it.cancel() }
        activePreloadJobs.clear()
        Log.d(TAG, "🛑 所有预加载任务已取消")
    }
}
```

#### 1.2 修改 ActivityTafsir 使用内存缓存

**修改文件**: `app/src/main/java/com/quran/quranaudio/online/quran_module/activities/ActivityTafsir.kt`

**修改 `loadContent()` 方法**:

```kotlin
private fun loadContent() {
    pageAlert.remove()
    binding.loader.visibility = View.VISIBLE
    
    CoroutineScope(Dispatchers.IO).launch {
        val startTime = System.currentTimeMillis()
        
        try {
            // 🔥 优化：使用三级缓存（内存 → 文件 → 网络）
            val tafsir = TafsirCacheManager.getTafsir(
                context = this@ActivityTafsir,
                tafsirKey = tafsirKey,
                chapterNo = chapterNo,
                verseNo = verseNo
            )
            
            if (tafsir != null) {
                // ✅ 缓存命中，立即渲染
                val elapsed = System.currentTimeMillis() - startTime
                android.util.Log.d("ActivityTafsir", "✅ Tafsir 从缓存加载成功 (${elapsed}ms)")
                renderData(tafsir)
                return@launch
            }
            
            // 🔥 缓存不存在，检查网络
            if (!NetworkStateReceiver.isNetworkConnected(this@ActivityTafsir)) {
                runOnUiThread { 
                    if (!isFinishing && !isDestroyed) {
                        noInternet() 
                    }
                }
                return@launch
            }
            
            // 🔥 从网络加载并缓存
            val result = TafsirCacheManager.loadAndCacheTafsir(
                context = this@ActivityTafsir,
                tafsirKey = tafsirKey!!,
                chapterNo = chapterNo,
                verseNo = verseNo
            )
            
            result.onSuccess { loadedTafsir ->
                val elapsed = System.currentTimeMillis() - startTime
                android.util.Log.d("ActivityTafsir", "✅ Tafsir 从网络加载成功 (${elapsed}ms)")
                renderData(loadedTafsir)
            }.onFailure { e ->
                android.util.Log.e("ActivityTafsir", "❌ Tafsir 加载失败: ${e.message}")
                fail("Failed to load tafsir.", true)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ActivityTafsir", "❌ Unexpected error: ${e.message}")
            fail("Failed to load tafsir.", true)
        }
    }
}
```

---

### 阶段 2: 预加载机制（用户选择后触发）

#### 2.1 在用户选择 Tafsir 版本后触发预加载

**修改文件**: `app/src/main/java/com/quran/quranaudio/online/quran_module/activities/readerSettings/FragSettingsTafsir.kt`（或相应的设置页面）

```kotlin
// 在用户选择 Tafsir 版本后
private fun onTafsirSelected(tafsirKey: String) {
    // 保存用户选择
    SPReader.setSavedTafsirKey(context, tafsirKey)
    
    // 🚀 触发预加载（后台异步）
    TafsirCacheManager.preloadCommonTafsirs(
        context = requireContext(),
        tafsirKey = tafsirKey,
        recentChapters = getRecentlyReadChapters() // 获取用户最近阅读的章节
    )
    
    Toast.makeText(context, "Tafsir version saved. Preloading common chapters...", Toast.LENGTH_SHORT).show()
}
```

#### 2.2 在应用启动时预加载

**修改文件**: `app/src/main/java/com/quran/quranaudio/online/App.java`

```java
private void initTafsirPreloading() {
    // 延迟 10 秒后开始预加载（避免影响启动性能）
    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
        try {
            String savedTafsirKey = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTafsirKey(this);
            if (savedTafsirKey != null && !savedTafsirKey.isEmpty()) {
                android.util.Log.d("App", "🚀 启动 Tafsir 预加载: " + savedTafsirKey);
                
                // 获取用户最近阅读的章节
                List<Integer> recentChapters = new ArrayList<>();
                // TODO: 从 SharedPreferences 或 Firestore 获取
                
                com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirCacheManager.INSTANCE.preloadCommonTafsirs(
                    this,
                    savedTafsirKey,
                    recentChapters
                );
            }
        } catch (Exception e) {
            android.util.Log.e("App", "❌ Tafsir 预加载失败: " + e.getMessage());
        }
    }, 10000); // 10 秒延迟
}
```

在 `onCreate()` 中调用:

```java
@Override
public void onCreate() {
    super.onCreate();
    
    // ...现有初始化代码...
    
    // 🚀 初始化 Tafsir 预加载
    initTafsirPreloading();
}
```

---

### 阶段 3: WebView 优化（减少渲染延迟）

#### 3.1 WebView 预热

**修改文件**: `app/src/main/java/com/quran/quranaudio/online/App.java`

```java
private void prewarmTafsirWebView() {
    // 在主线程空闲时预热 WebView
    android.os.Looper.myQueue().addIdleHandler(() -> {
        try {
            // 创建并立即销毁一个 WebView，触发 WebView 引擎初始化
            android.webkit.WebView webView = new android.webkit.WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("about:blank");
            webView.destroy();
            android.util.Log.d("App", "✅ WebView 预热完成");
        } catch (Exception e) {
            android.util.Log.e("App", "❌ WebView 预热失败: " + e.getMessage());
        }
        return false; // 只执行一次
    });
}
```

在 `onCreate()` 中调用:

```java
@Override
public void onCreate() {
    super.onCreate();
    
    // ...现有初始化代码...
    
    // 🚀 预热 WebView
    prewarmTafsirWebView();
}
```

#### 3.2 HTML 模板缓存

**修改 ActivityTafsir.kt 的 `getBoilerPlateHTML()` 方法**:

```kotlin
private var cachedBoilerPlateHTML: String? = null

private fun getBoilerPlateHTML(): String {
    // 🔥 优化：缓存 HTML 模板，避免每次都从 assets 读取
    if (cachedBoilerPlateHTML != null) {
        return cachedBoilerPlateHTML!!
    }
    
    val html = assets.open("reader/tafsir_single_verse.html")
        .bufferedReader()
        .use { it.readText() }
    
    cachedBoilerPlateHTML = html
    return html
}
```

---

## 📊 性能对比

### 修改前

| 场景 | 首次加载 | 二次加载（有文件缓存） |
|-----|---------|-------------------|
| **内存缓存** | ❌ 无 | ❌ 无 |
| **文件读取** | 0ms（无缓存） | 100-500ms |
| **网络请求** | 2000-5000ms | 0ms |
| **JSON 解析** | 0ms | 50-100ms |
| **WebView 渲染** | 500-1000ms | 500-1000ms |
| **总耗时** | **3-10 秒** | **1-2 秒** |

### 修改后

| 场景 | 首次加载 | 二次加载（有文件缓存） | 预加载后 |
|-----|---------|-------------------|---------|
| **内存缓存** | ❌ 无 | ✅ 命中 (< 1ms) | ✅ 命中 (< 1ms) |
| **文件读取** | 50-100ms | 0ms | 0ms |
| **网络请求** | 2000-5000ms | 0ms | 0ms |
| **JSON 解析** | 50-100ms | 0ms | 0ms |
| **WebView 渲染** | 200-500ms | 200-500ms | 200-500ms |
| **总耗时** | **2-5 秒** | **< 500ms** ✅ | **< 500ms** ✅ |

### 性能提升

- ✅ **二次加载**: 从 1-2 秒降低到 **< 500ms**（提升 **70-80%**）
- ✅ **预加载后**: **秒开**（< 500ms）
- ✅ **内存占用**: LRU 缓存最多 50 个 Tafsir（约 **5MB**）
- ✅ **用户体验**: **极大提升**

---

## 🧪 测试计划

### 测试场景

1. **首次安装**:
   - 选择 Tafsir 版本
   - 打开任意章节的 Tafsir
   - 预期：2-5 秒（网络加载）

2. **预加载后**:
   - 打开常用章节（第 1, 18, 36, 67 章）
   - 预期：< 500ms（内存缓存）

3. **二次打开**:
   - 打开之前阅读过的 Tafsir
   - 预期：< 500ms（内存缓存或文件缓存）

4. **网络断开**:
   - 断开网络
   - 打开已缓存的 Tafsir
   - 预期：< 500ms（内存/文件缓存）

5. **内存压力**:
   - 连续打开 60+ 个不同的 Tafsir
   - 预期：LRU 缓存自动清理，最旧的 10 个被移除

---

## 📝 实施步骤

### 第1步: 创建 TafsirCacheManager

1. 创建新文件 `TafsirCacheManager.kt`
2. 实现三级缓存逻辑
3. 实现预加载机制

**预计时间**: 2-3 小时

### 第2步: 修改 ActivityTafsir

1. 修改 `loadContent()` 方法
2. 使用 `TafsirCacheManager`
3. 添加性能日志

**预计时间**: 1 小时

### 第3步: 集成预加载

1. 在设置页面触发预加载
2. 在应用启动时触发预加载
3. 添加预加载进度提示（可选）

**预计时间**: 1-2 小时

### 第4步: WebView 优化

1. 添加 WebView 预热
2. 缓存 HTML 模板
3. 优化 WebView 设置

**预计时间**: 1 小时

### 第5步: 测试和调优

1. 测试各种场景
2. 性能监控
3. 调整缓存大小和预加载策略

**预计时间**: 2-3 小时

**总预计时间**: 7-10 小时

---

## ⚠️ 注意事项

### 1. 内存管理

- LRU 缓存大小设置为 50 个 Tafsir（约 5MB）
- 如果用户设备内存紧张，可以动态调整缓存大小

### 2. 网络使用

- 预加载在后台进行，每次请求间隔 200ms
- 用户可以在设置中禁用预加载（省流量模式）

### 3. 存储空间

- 文件缓存可能占用 100-500MB（取决于用户阅读习惯）
- 提供清除缓存选项

### 4. 兼容性

- 确保 WebView 预热在所有 Android 版本上正常工作
- 处理低内存设备的特殊情况

---

## 🎯 后续优化方向

### 短期（1-2周）

1. **智能预加载**: 根据用户阅读习惯，预测下一个可能阅读的章节
2. **离线模式**: 提供"下载整部 Tafsir"选项，完全离线使用
3. **增量更新**: 只下载更新的 Tafsir 内容

### 中期（1-2月）

1. **CDN 加速**: 将 Tafsir 数据托管到 CDN，加快下载速度
2. **数据压缩**: 使用 Gzip 压缩 Tafsir JSON，减少传输时间
3. **并行加载**: 同时加载多个 Tafsir，提高预加载效率

### 长期（3-6月）

1. **本地数据库**: 将 Tafsir 数据存储到 SQLite，加快查询速度
2. **P2P 分享**: 用户之间分享 Tafsir 缓存，减少服务器压力
3. **AI 推荐**: 基于用户行为，智能推荐和预加载 Tafsir

---

**创建日期**: 2026-01-08  
**创建人员**: AI Assistant  
**状态**: ✅ 方案完成，等待实施

**下一步**: 按照实施步骤，逐步实现优化方案，并进行测试验证！🚀

