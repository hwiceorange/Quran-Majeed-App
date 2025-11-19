# 📖 印尼语 Tafsir 内容 API 集成方案

## 🐛 崩溃原因分析

### 问题流程
1. ✅ **Tafsir 清单加载成功**
   ```
   ✅ Parsed 3 language groups
      - id: 1 tafsirs (印尼语)
   ```

2. ✅ **印尼语 Tafsir 自动选择**
   ```
   key: id-tafsir-kemenag
   slug: id-tafsir-kemenag
   ```

3. ❌ **尝试加载 Tafsir 内容 → 崩溃**
   ```kotlin
   // ActivityTafsir.kt:345
   val tafsir = RetrofitInstance.quran.getTafsir(slug, "$chapterNo:$verseNo")
   ```
   
   **问题：** 应用从 `api.quran.com` 加载 Tafsir，但印尼语 Tafsir 不在 Quran.com 上！

### 根本原因
- **应用期望：** 所有 Tafsir 都在 Quran.com API
- **实际情况：** 印尼语 Tafsir 在您的服务器（`tafsir_indonesian` 表，6,236 条）

---

## 🔧 解决方案 A: 服务器 API 适配（推荐）

### 步骤 1: 在服务器创建 Tafsir API

创建一个**兼容 Quran.com** 格式的 API 端点：

```
GET https://apis.dochubai.com/quran/api/v4/tafsirs/:slug/by_ayah/:ayahKey
```

**示例：**
```
GET https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

**响应格式（必须兼容 Quran.com）：**
```json
{
  "tafsir": {
    "text": "...印尼语注释内容..."
  }
}
```

或者 Quran.com 的实际格式：
```json
{
  "tafsir": {
    "resource_id": 999,
    "text": "...印尼语注释内容...",
    "verse_key": "1:1"
  }
}
```

### 步骤 2: PHP/Laravel 实现示例

```php
<?php
// routes/api.php
Route::get('/v4/tafsirs/{slug}/by_ayah/{ayahKey}', function($slug, $ayahKey) {
    // 只处理印尼语 Tafsir
    if ($slug !== 'id-tafsir-kemenag') {
        abort(404, 'Tafsir not found');
    }
    
    list($surahId, $ayahId) = explode(':', $ayahKey);
    
    // 从数据库查询
    $tafsir = DB::table('tafsir_indonesian')
        ->where('surah_id', $surahId)
        ->where('ayat_id', $ayahId)
        ->where('language', 'id')
        ->first();
    
    if (!$tafsir) {
        abort(404, 'Tafsir not found for this ayah');
    }
    
    // 返回兼容格式
    return response()->json([
        'tafsir' => [
            'resource_id' => 999,
            'text' => $tafsir->text,
            'verse_key' => $ayahKey
        ]
    ]);
});
```

### 步骤 3: 修改应用代码

修改 `ActivityTafsir.kt` 的 `loadContent()` 方法，让印尼语 Tafsir 从您的服务器加载：

```kotlin
// 第 344-349 行
try {
    val tafsir = if (slug == "id-tafsir-kemenag") {
        // 印尼语 Tafsir 从自定义服务器加载
        RetrofitInstance.customServer.getTafsir(slug, "$chapterNo:$verseNo")["tafsir"]!!
    } else {
        // 其他 Tafsir 从 Quran.com 加载
        RetrofitInstance.quran.getTafsir(slug, "$chapterNo:$verseNo")["tafsir"]!!
    }
    
    fileUtils.createFile(tafsirFile)
    tafsirFile.writeText(JsonHelper.json.encodeToString(tafsir))
    renderData(tafsir)
} catch (e: Exception) {
    Log.saveError(e, "ActivityTafsir")
    e.printStackTrace()
    fail("Failed to load tafsir.", true)
}
```

---

## 🔧 解决方案 B: 修改应用支持多 API 源（完整方案）

### 步骤 1: 创建 Custom API 接口

```kotlin
// app/.../api/CustomTafsirApi.kt
interface CustomTafsirApi {
    @GET("v4/tafsirs/{slug}/by_ayah/{ayahKey}")
    suspend fun getTafsir(
        @Path("slug") slug: String,
        @Path("ayahKey") ayahKey: String
    ): Map<String, TafsirModel>
}
```

### 步骤 2: 添加到 RetrofitInstance

```kotlin
// RetrofitInstance.kt
val customTafsir: CustomTafsirApi by lazy {
    Retrofit.Builder()
        .baseUrl("https://apis.dochubai.com/quran/api/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CustomTafsirApi::class.java)
}
```

### 步骤 3: 修改 loadContent 逻辑

```kotlin
private fun loadContent() {
    pageAlert.remove()
    binding.loader.visibility = View.VISIBLE

    CoroutineScope(Dispatchers.IO).launch {
        val tafsirFile = fileUtils.getTafsirFileSingleVerse(tafsirKey, chapterNo, verseNo)
        val slug = TafsirUtils.getTafsirSlugFromKey(tafsirKey)

        // 检查缓存
        if (tafsirFile.length() > 0) {
            val read = tafsirFile.readText()
            val tafsir = JsonHelper.json.decodeFromString<TafsirModel>(read)
            renderData(tafsir)
            return@launch
        }

        // 检查网络
        if (!NetworkStateReceiver.isNetworkConnected(this@ActivityTafsir)) {
            runOnUiThread { noInternet() }
            return@launch
        }

        try {
            // 根据 slug 选择 API 源
            val tafsir = when {
                slug.startsWith("id-") -> {
                    // 印尼语 Tafsir 从自定义服务器
                    RetrofitInstance.customTafsir.getTafsir(slug, "$chapterNo:$verseNo")["tafsir"]!!
                }
                else -> {
                    // 其他 Tafsir 从 Quran.com
                    RetrofitInstance.quran.getTafsir(slug, "$chapterNo:$verseNo")["tafsir"]!!
                }
            }

            fileUtils.createFile(tafsirFile)
            tafsirFile.writeText(JsonHelper.json.encodeToString(tafsir))
            renderData(tafsir)
        } catch (e: Exception) {
            android.util.Log.e("ActivityTafsir", "❌ Failed to load tafsir from ${if (slug.startsWith("id-")) "custom server" else "Quran.com"}: ${e.message}")
            Log.saveError(e, "ActivityTafsir")
            e.printStackTrace()
            fail("Failed to load tafsir.", true)
        }
    }
}
```

---

## 🚀 临时解决方案（当前已实施）

**已移除印尼语 Tafsir** 从清单，避免崩溃：

```json
{
  "tafsirs": {
    "en": [...],
    "ar": [...]
    // id: [...] ← 已移除
  }
}
```

**效果：**
- ✅ 应用不会崩溃
- ✅ 英语和阿拉伯语 Tafsir 正常工作
- ⚠️ 印尼语暂时不可用

---

## 📊 完整集成步骤

### 阶段 1: 服务器端（必须完成）

1. ✅ 上传印尼语 Tafsir 数据（已完成，6,236 条）
2. ⚠️ 创建 API 端点：
   ```
   GET /quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/:ayahKey
   ```
3. ⚠️ 返回兼容格式的 JSON

### 阶段 2: 应用端（待实施）

1. ⚠️ 添加 `CustomTafsirApi` 接口
2. ⚠️ 修改 `RetrofitInstance` 添加自定义服务器
3. ⚠️ 修改 `ActivityTafsir.loadContent()` 支持多 API 源
4. ⚠️ 重新添加印尼语 Tafsir 到清单
5. ⚠️ 编译测试

---

## 🧪 测试清单

### 服务器 API 测试

```bash
# 测试 API 端点
curl https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1

# 预期响应：
{
  "tafsir": {
    "resource_id": 999,
    "text": "...印尼语注释内容...",
    "verse_key": "1:1"
  }
}
```

### 应用测试

1. 清单加载：✅ 解析成功
2. 内容加载：⚠️ 需要服务器 API
3. 显示注释：⚠️ 需要服务器 API

---

## 📝 下一步操作

**立即可做：**
1. ✅ 重新编译应用（不包含印尼语 Tafsir，避免崩溃）
2. ✅ 测试英语/阿拉伯语 Tafsir 是否正常

**需要服务器端配合：**
1. ⚠️ 创建 Tafsir API 端点
2. ⚠️ 测试 API 响应格式
3. ⚠️ 修改应用代码支持自定义 API
4. ⚠️ 重新添加印尼语 Tafsir

---

## 💡 建议

### 选项 1: 完整实施（推荐）
- 创建服务器 API
- 修改应用支持多 API 源
- 完整支持印尼语 Tafsir

### 选项 2: 分阶段实施
- **第一阶段：** 先让应用不崩溃（当前已完成）
- **第二阶段：** 配置服务器 API
- **第三阶段：** 修改应用集成

### 选项 3: 仅支持 Quran.com Tafsir
- 不实施印尼语 Tafsir
- 仅使用 Quran.com 提供的 Tafsir
- 印尼语用户显示"无注释"

---

**您希望采用哪个方案？** 我可以帮您实现完整的代码修改！

