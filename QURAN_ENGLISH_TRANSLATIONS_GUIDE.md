# 📖 古兰经英文翻译版本完整指南

## 🕌 当前应用使用的英文翻译

### 📋 预装版本（本地）

应用内预装了 **2 个英文翻译版本**，无需下载即可使用：

#### 1️⃣ **Sahih International**（默认英文版本）

| 属性 | 详情 |
|------|------|
| **ID** | 101 |
| **Slug** | `en_101_sahih-international` |
| **全名** | Sahih International |
| **译者** | Al-Muntada Al-Islami (Islamic Forum) |
| **文件位置** | `/app/src/main/assets/prebuilt_translations/en_saheeh_v1_1_1/` |
| **特点** | 现代英语，清晰流畅，广泛使用 |
| **默认使用** | ✅ 是（英语系统的默认翻译） |

**描述**：
- 最广泛使用的现代英语翻译之一
- 语言清晰、流畅、易懂
- 由沙特阿拉伯的 Islamic Forum 组织翻译
- 适合现代读者，特别是年轻人和初学者

---

#### 2️⃣ **The Clear Quran**

| 属性 | 详情 |
|------|------|
| **ID** | 102 |
| **Slug** | `en_102_the-clear-quran` |
| **全名** | The Clear Quran |
| **译者** | Dr. Mustafa Khattab |
| **文件位置** | `/app/src/main/assets/prebuilt_translations/en_the_clear_quran/` |
| **特点** | 现代英语，易于理解，带有语境注释 |

**描述**：
- 最新的现代英语翻译之一（2016年）
- 由加拿大伊斯兰学者 Dr. Mustafa Khattab 翻译
- 语言极其清晰，专注于可读性
- 包含大量脚注和上下文解释
- 适合英语为母语的读者

---

## 🌐 可通过 API 下载的英文版本

应用通过 **`https://apis.dochubai.com/quran/api/translations/`** 提供 **100+ 种语言的 200+ 个翻译版本**。

### 您提到的英文翻译版本

根据您的需求，以下是主流英文翻译的可获取性分析：

#### ✅ **1. Sahih International** 
- **状态**: ✅ **已预装**
- **ID**: 101
- **特点**: 现代英语，清晰流畅，广泛使用
- **使用方式**: 应用默认提供，无需下载

#### 🔍 **2. Pickthall (Marmaduke Pickthall)**
- **状态**: 🌐 **需要通过 API 下载**
- **特点**: 经典译本（1930年代），语言古典，文学性强
- **描述**: 第一个由英国穆斯林翻译的版本
- **适合**: 喜欢古典英语文学风格的读者

#### 🔍 **3. Yusuf Ali (Abdullah Yusuf Ali)**
- **状态**: 🌐 **需要通过 API 下载**
- **特点**: 广泛使用，带有详细注释
- **描述**: 最经典的英文翻译之一（1934年）
- **适合**: 需要详细解释和注释的读者

#### 🔍 **4. Muhsin Khan & Hilali (Dr. Muhsin Khan & Dr. Hilali)**
- **状态**: 🌐 **需要通过 API 下载**
- **特点**: 保守派倾向，带有丰富的圣训解释
- **描述**: 沙特阿拉伯官方认可的翻译
- **适合**: 寻求传统伊斯兰学术解释的读者

#### ✅ **5. Saheeh International (同 Sahih International)**
- **状态**: ✅ **已预装**
- **说明**: 与 #1 相同，拼写变体

---

## 🔧 如何获取更多英文翻译？

### 方法 1：应用内下载（推荐）

1. **打开应用**
2. **进入设置** → **Translations (翻译)** → **Download Translations (下载翻译)**
3. **选择 English** 分类
4. **浏览可用的英文翻译版本**
5. **点击下载**你需要的版本

### 方法 2：通过 API 手动获取

应用使用以下 API 获取翻译列表：

```
GET https://apis.dochubai.com/quran/api/translations/available_translations_info.json
```

**响应格式**：
```json
{
  "translations": [
    {
      "id": 101,
      "name": "Sahih International",
      "author": "...",
      "language": "en",
      "download_url": "..."
    }
  ]
}
```

### 方法 3：从 Quran.com API 获取

应用也集成了 `https://api.quran.com/api/v4/` API，可以获取更多翻译版本。

---

## 📚 英文翻译版本对比

| 翻译版本 | 时代 | 语言风格 | 可读性 | 注释 | 适合人群 | 可用性 |
|---------|------|---------|-------|------|---------|--------|
| **Sahih International** | 现代（1997） | 现代英语 | ⭐⭐⭐⭐⭐ | 少量 | 初学者、现代读者 | ✅ 预装 |
| **The Clear Quran** | 现代（2016） | 现代英语 | ⭐⭐⭐⭐⭐ | 丰富 | 英语母语者 | ✅ 预装 |
| **Pickthall** | 经典（1930） | 古典英语 | ⭐⭐⭐ | 少量 | 文学爱好者 | 🌐 可下载 |
| **Yusuf Ali** | 经典（1934） | 古典英语 | ⭐⭐⭐⭐ | 极丰富 | 学术研究者 | 🌐 可下载 |
| **Muhsin Khan** | 现代（1996） | 现代英语 | ⭐⭐⭐⭐ | 极丰富 | 保守派、学者 | 🌐 可下载 |

---

## 🎯 推荐方案：添加多版本支持

### 当前状态
- ✅ 已支持：Sahih International, The Clear Quran
- ⏳ 待添加：Pickthall, Yusuf Ali, Muhsin Khan & Hilali

### 实现方案

#### **方案 1：预装更多版本**

**优点**：
- ✅ 完全离线
- ✅ 加载速度快
- ✅ 用户体验好

**缺点**：
- ❌ 增加 APK 大小（每个版本 ~3-5 MB）
- ❌ 需要重新发布应用

**实施步骤**：
1. 从 API 下载所需翻译的 JSON 文件
2. 放入 `/app/src/main/assets/prebuilt_translations/` 目录
3. 创建对应的 `manifest.json` 文件
4. 在 `TranslUtils.java` 中添加新的 slug 常量
5. 重新编译应用

---

#### **方案 2：应用内下载（当前方案）**

**优点**：
- ✅ 不增加 APK 大小
- ✅ 用户按需选择
- ✅ 支持所有可用翻译（200+）

**缺点**：
- ⚠️ 首次使用需要网络
- ⚠️ 需要存储空间

**当前实现**：
- API 端点：`https://apis.dochubai.com/quran/api/translations/`
- 实现文件：`GithubApi.kt`, `TranslationDownloadService.kt`
- 缓存位置：`/data/data/com.quran.quranaudio.online/files/translations/`

---

## 🔍 技术实现细节

### 翻译文件结构

**Manifest 文件** (`manifest.json`):
```json
{
  "id": 101,
  "slug": "en_101_sahih-international",
  "book": "Sahih International",
  "author": "Al-Muntada Al-Islami",
  "display-name": "Sahih International",
  "lang-code": "en",
  "lang-name": "English"
}
```

**翻译数据文件** (`en_sahih_international.json`):
```json
[
  {
    "chapter": 1,
    "verse": 1,
    "text": "In the name of Allah, the Entirely Merciful, the Especially Merciful."
  },
  ...
]
```

### 代码中的使用

**获取翻译**：
```kotlin
// TranslationFactory 用法
val factory = QuranTranslationFactory(context)
val translation = factory.getTranslationsSingleSlugVerse(
    slug = "en_101_sahih-international",
    chapterNo = 1,
    verseNo = 1
)
```

**默认翻译选择**：
```java
// TranslUtils.java
public static final String TRANSL_SLUG_EN_SAHIH_INTERNATIONAL = "en_101_sahih-international";
public static final String TRANSL_SLUG_EN_THE_CLEAR_QURAN = "en_102_the-clear-quran";

// 默认规则：
// - 英语系统 → Sahih International
// - 印尼语系统 → Indonesian Ministry
// - 乌尔都语系统 → Junagarhi
// - 其他语言 → Sahih International (英语作为默认)
```

---

## 📊 数据来源分析

### 当前应用的翻译数据源

#### 1️⃣ **预装翻译**
- **来源**：应用 APK 内置
- **位置**：`/app/src/main/assets/prebuilt_translations/`
- **数量**：5 个（2 个英文 + 2 个乌尔都语 + 1 个印尼语）
- **优势**：完全离线，加载快

#### 2️⃣ **可下载翻译**
- **来源**：`https://apis.dochubai.com/quran/`
- **API 文件**：`GithubApi.kt`
- **支持数量**：200+ 个翻译，100+ 种语言
- **优势**：按需下载，不增加 APK 大小

#### 3️⃣ **备用来源**
- **Quran.com API v4**：`https://api.quran.com/api/v4/`
- **用于**：注释（Tafsir），元数据
- **集成**：`QuranApi.kt`

---

## 🚀 快速添加新英文翻译的方法

### 如果您想立即添加 Pickthall、Yusuf Ali、Muhsin Khan：

#### **方法 1：预装到应用（需要重新编译）**

1. **下载翻译文件**：
   ```bash
   # 从 API 获取可用翻译列表
   curl https://apis.dochubai.com/quran/api/translations/available_translations_info.json
   
   # 找到对应 ID 和下载链接
   # Pickthall: ID ~20
   # Yusuf Ali: ID ~22
   # Muhsin Khan: ID ~18
   ```

2. **创建目录和文件**：
   ```bash
   mkdir -p app/src/main/assets/prebuilt_translations/en_pickthall
   mkdir -p app/src/main/assets/prebuilt_translations/en_yusuf_ali
   mkdir -p app/src/main/assets/prebuilt_translations/en_muhsin_khan
   ```

3. **添加 manifest.json**：
   ```json
   {
     "id": 20,
     "slug": "en_20_pickthall",
     "book": "Pickthall",
     "author": "Marmaduke Pickthall",
     "display-name": "Pickthall",
     "lang-code": "en",
     "lang-name": "English"
   }
   ```

4. **更新代码**：
   ```java
   // TranslUtils.java
   public static final String TRANSL_SLUG_EN_PICKTHALL = "en_20_pickthall";
   public static final String TRANSL_SLUG_EN_YUSUF_ALI = "en_22_yusuf-ali";
   public static final String TRANSL_SLUG_EN_MUHSIN_KHAN = "en_18_muhsin-khan";
   
   String[] enTranslations = {
       TRANSL_SLUG_EN_SAHIH_INTERNATIONAL, 
       TRANSL_SLUG_EN_THE_CLEAR_QURAN,
       TRANSL_SLUG_EN_PICKTHALL,
       TRANSL_SLUG_EN_YUSUF_ALI,
       TRANSL_SLUG_EN_MUHSIN_KHAN
   };
   ```

5. **重新编译应用**。

---

#### **方法 2：用户自行下载（无需重新编译）**

用户可以通过应用内的 **Settings → Translations → Download Translations** 功能，自行选择和下载需要的英文翻译版本。

**优点**：
- 不需要修改代码
- 不增加 APK 大小
- 用户按需选择

---

## 📱 用户使用指南

### 如何在应用中切换英文翻译？

1. **打开应用**
2. **进入古兰经阅读页面**
3. **点击设置图标** ⚙️
4. **选择 "Translations" (翻译)**
5. **勾选**你想显示的翻译版本（最多 6 个）
6. **保存**设置

### 如何下载新的翻译版本？

1. **打开应用**
2. **进入 Settings (设置)**
3. **选择 "Download Translations" (下载翻译)**
4. **选择语言**（English）
5. **浏览并下载**所需版本

---

## 📊 统计信息

| 项目 | 数量 |
|------|------|
| **预装英文翻译** | 2 个 |
| **可下载英文翻译** | ~15-20 个 |
| **总可用翻译（所有语言）** | 200+ 个 |
| **支持语言** | 100+ 种 |
| **平均翻译文件大小** | 3-5 MB |

---

## 🎯 总结与建议

### 当前状态 ✅
- 应用已预装 **Sahih International**（您列表中的第1个）
- 应用已预装 **The Clear Quran**（现代清晰版本）
- 用户可以通过应用内下载功能获取更多英文翻译

### 建议方案 💡

#### **方案 A：保持现状**
- 优点：APK 大小适中，用户按需下载
- 缺点：需要网络才能获取其他版本

#### **方案 B：预装所有 5 个主流英文版本**
- 优点：完全离线，用户体验最佳
- 缺点：APK 增加 ~15-20 MB
- 适合：如果您的目标用户主要是英语用户

#### **方案 C：混合方案（推荐）** ✨
- 预装：Sahih International, The Clear Quran（已有）
- 提示用户下载：Pickthall, Yusuf Ali, Muhsin Khan
- 在设置页面添加"推荐英文翻译"部分
- 一键下载常用翻译包

---

## 📞 相关文档

- `DATA_SOURCES_ANALYSIS.md` - 完整数据源分析
- `QURAN_FIRESTORE_IMPORT_GUIDE.md` - Firestore 数据导入指南
- `TranslUtils.java` - 翻译工具类源码
- `QuranTranslationFactory.kt` - 翻译加载工厂类

---

**📖 希望这份指南能帮助您了解应用内的英文古兰经翻译！**

如需添加更多翻译版本或有任何问题，请随时告知。

