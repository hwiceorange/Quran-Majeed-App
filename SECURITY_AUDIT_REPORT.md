# 🔐 Quran Majeed App - 安全审计报告

**审计日期**: 2025-10-24  
**审计范围**: 完整代码库安全审计  
**审计人员**: AI Assistant  

---

## 📋 执行摘要

本次安全审计对 Quran Majeed 应用进行了全面的安全检查，重点排查了第三方开发者留下的潜在安全风险、未授权的数据传输、以及恶意代码。

### 🎯 审计结论
- ✅ **未发现**动态代码加载 (DexClassLoader)
- ✅ **未发现**未授权的POST/PUT数据上传
- ⚠️ **发现并修复**第三方开发者信息和联系方式
- ⚠️ **需确认**部分第三方广告ID的所有权
- ✅ 混淆配置基本安全
- ✅ 通知系统正常，仅使用Firebase推送

---

## 1️⃣ 混淆配置审计

### 检查文件
- `app/proguard-rules.pro`
- `shaheendevelopersAds_SDK/consumer-rules.pro`
- `adlib/consumer-rules.pro`
- `quiz/consumer-rules.pro`
- `peacedesign/consumer-rules.pro`

### 审计结果
✅ **PASS** - 未发现异常keep规则

#### 关键发现
```proguard
-keep class com.adjust.sdk.** { *; }
-keep class com.quran.quranaudio.online.prayertimes.** {*;}
-keep class com.quran.quranaudio.online.quran_module.api.models.recitation.**{*;}
-keep class java.**{*;}
```

**评估**: 
- 所有keep规则都指向合法的类包
- 未发现针对不明第三方包的keep规则
- `com.adjust.sdk` 是合法的分析SDK

---

## 2️⃣ 动态代码加载审计

### 检查模式
搜索关键词: `DexClassLoader`, `PathClassLoader`, `.dex`, `loadClass`, `defineClass`

### 审计结果
✅ **PASS** - **未发现任何动态代码加载**

**结论**: 应用不使用动态代码加载技术，无此类安全风险。

---

## 3️⃣ Firebase/AdMob ID 审计

### 3.1 Firebase 配置

**文件**: `app/google-services.json`

| 配置项 | 值 | 状态 |
|--------|-----|------|
| Project ID | `quran-majeed-aa3d2` | ✅ 已确认 |
| Project Number | `517834286063` | ✅ 已确认 |
| AdMob App ID | `ca-app-pub-3966802724737141~7853010339` | ✅ 已确认 |

### 3.2 AdMob 广告单元 ID

**文件**: `app/src/main/java/com/quran/quranaudio/online/ads/data/Constant.java`

| 广告类型 | ID | 状态 |
|----------|-----|------|
| Banner | `ca-app-pub-3966802724737141/1386840185` | ✅ 属于您 |
| Interstitial | `ca-app-pub-3966802724737141/2182661506` | ✅ 属于您 |
| Native | `ca-app-pub-3966802724737141/1300824672` | ✅ 属于您 |
| App Open | `ca-app-pub-3966802724737141/3298687654` | ✅ 属于您 |

### 3.3 第三方广告平台 ID

⚠️ **需要您确认所有权**

#### Facebook Audience Network (FAN)
```java
FAN_BANNER_ID = "154641400992658_154641934325938"
FAN_INTERSTITIAL_ID = "154641400992658_154641894325942"  
FAN_NATIVE_ID = "154641400992658_154642014325930"
```

#### Unity Ads
```java
UNITY_GAME_ID = "4089993"
UNITY_BANNER_ID = "banner"
UNITY_INTERSTITIAL_ID = "video"
```

#### AppLovin
```java
APPLOVIN_BANNER_ID = "da17eff31ae69f15"
APPLOVIN_INTERSTITIAL_ID = "98f6a586ed642919"
APPLOVIN_NATIVE_MANUAL_ID = "87343269587e8998"
APPLOVIN_APP_OPEN_AP_ID = "de9f381d132b859a"
APPLOVIN_BANNER_ZONE_ID = "afb7122672e86340"
APPLOVIN_BANNER_MREC_ZONE_ID = "81287b697d935c32"
APPLOVIN_INTERSTITIAL_ZONE_ID = "b6eba8b976279ea5"
```

**⚠️ 建议**: 登录各广告平台确认这些ID是否属于您的账户。

---

## 4️⃣ 第三方调用排查

### 4.1 外部 URL 调用分析

#### ✅ 合法的URL调用

| URL | 用途 | 位置 |
|-----|------|------|
| `market://details?id=...` | Google Play评分 | MainActivity.java |
| `https://play.google.com/store/...` | Google Play链接 | MainActivity.java |
| `https://www.youtube.com/...` | YouTube视频播放 | LiveActivity.kt |
| `geo:0,0?q=...` | Google Maps导航 | Utils.java |
| `https://api.quran.com/` | 官方古兰经API | RetrofitInstance.kt |

#### ✅ 已修复的问题调用

| 原URL | 修复后 | 状态 |
|-------|--------|------|
| `https://wa.me/+923002375907` | `mailto:lecheng2019@gmail.com` | ✅ 已修复 |
| `https://github.com/unkn4wn/hadith-pro/releases` | 已删除 | ✅ 已修复 |
| `https://codecanyon.net/user/shaheendevelopers/portfolio` | 已删除 | ✅ 已修复 |

#### ✅ 您自己的URL（无问题）

| URL | 用途 |
|-----|------|
| `https://apis.dochubai.com/quran/` | 您的API服务器 |
| `https://www.dochubai.com/privacy` | 您的隐私政策页面 |

### 4.2 隐式 Intent 检查

**发现56处startActivity调用**，经检查均为：
- 应用内页面跳转
- 合法的系统Intent（分享、打开文件等）
- 已确认的外部URL（见上表）

✅ **PASS** - 无未授权的隐式Intent

---

## 5️⃣ 后台服务和数据上传审计

### 5.1 后台服务列表

| 服务名 | 用途 | 风险评估 |
|--------|------|---------|
| `MyFirebaseMessagingService` | Firebase推送通知 | ✅ 安全 |
| `RecitationService` | 古兰经朗诵播放 | ✅ 安全 |
| `RecitationChapterDownloadService` | 下载章节音频 | ✅ 安全 |
| `TranslationDownloadService` | 下载翻译文件 | ✅ 安全 |
| `KFQPCScriptFontsDownloadService` | 下载字体 | ✅ 安全 |
| `ListeningTimerService` | 听读计时器 | ✅ 安全 |

### 5.2 网络请求审计

#### ✅ GET 请求（下载数据）

所有GET请求均指向合法端点：
- `https://apis.dochubai.com/quran/` - 您的服务器
- `https://api.quran.com/` - 官方API
- `https://nominatim.openstreetmap.org/` - 地理位置服务
- `https://www.londonprayertimes.com/api/` - 祈祷时间API
- `https://api.openweathermap.org/` - 天气API

#### ✅ POST/PUT 请求

**搜索结果**: 未发现任何POST或PUT请求用于上传用户数据

**代码中的`.post()`调用**: 均为`Handler.post()`方法（UI线程操作），不是HTTP POST请求

✅ **PASS** - 无未授权的数据上传

---

## 6️⃣ 通知和弹窗审计

### 6.1 Firebase 推送通知

**文件**: `MyFirebaseMessagingService.kt`

```kotlin
fun showNotification(context: Context, title: String?, message: String?) {
    val ii = Intent(context, HomeActivity::class.java)
    ii.data = Uri.parse("custom://" + System.currentTimeMillis())
    // ... 标准通知实现
}
```

✅ **评估**: 
- 仅接收Firebase推送
- 点击通知打开应用首页
- 无异常行为

### 6.2 本地通知

发现的通知相关类：
- `PrayerNotification.java` - 祈祷时间提醒
- `ReminderNotification.java` - 自定义提醒
- `CannotScheduleExactAlarmNotification.java` - 系统权限通知
- `VotdReceiver.kt` - 每日经文通知

✅ **评估**: 所有通知均与应用功能相关，无异常通知。

---

## 7️⃣ 已修复的安全问题

### ✅ 删除的第三方开发者信息

1. **ApiConfig.kt**
   - ❌ 删除: Rai Adnan 开发者注释
   - ❌ 删除: WhatsApp +923002375907
   - ✅ 替换为: mailto:lecheng2019@gmail.com

2. **所有strings.xml文件** (7个语言版本)
   - ❌ 删除: GitHub Hadith Pro链接
   - ❌ 删除: "I am Rai Adnan" 开发者介绍
   - ✅ 替换为: 您的应用信息

3. **SettingsActivity.java**
   - ❌ 删除: GitHub更新链接

4. **ApiInterface.java**
   - ❌ 修改前: `Data-Agent: ShaheenDevelopers`
   - ✅ 修改后: `Data-Agent: QuranMajeed`

5. **关键源代码文件**
   - 删除了4+个关键文件的开发者头部注释

---

## 🚨 风险评估总结

### 🟢 低风险（已修复）
- ✅ 第三方开发者联系方式 - **已删除**
- ✅ 第三方GitHub链接 - **已删除**
- ✅ 网络请求标识 - **已修改**

### 🟡 中等风险（需确认）
- ⚠️ **Facebook Audience Network ID** - 需登录确认所有权
- ⚠️ **Unity Ads ID** - 需登录确认所有权
- ⚠️ **AppLovin Zone IDs** - 需登录确认所有权

### 🟢 无风险
- ✅ 无动态代码加载
- ✅ 无未授权数据上传
- ✅ Firebase/AdMob配置正确
- ✅ 所有服务合法
- ✅ 通知系统正常

---

## 📝 剩余任务清单

### 必须完成
1. ⚠️ **确认第三方广告平台ID所有权**
   - [ ] Facebook Audience Network
   - [ ] Unity Ads  
   - [ ] AppLovin

2. ⚠️ **批量删除XML文件中的开发者注释** (约200个文件)
   - 使用正则表达式全局替换
   - 或运行提供的Python脚本

### 建议完成
3. [ ] 测试所有修改后的功能
4. [ ] 验证崩溃报告邮箱是否工作
5. [ ] 检查"关于"页面显示是否正确

---

## 🎯 最终结论

**应用安全状态**: ✅ **基本安全**

经过全面审计，未发现严重的安全漏洞或恶意代码。第三方开发者的信息已被清理。需要确认部分广告平台ID的所有权以确保广告收益归您所有。

**建议优先级**:
1. 🔴 **高**: 确认所有广告平台ID
2. 🟡 **中**: 删除剩余的XML注释  
3. 🟢 **低**: 完成功能测试

---

**报告生成时间**: 2025-10-24  
**下次审计建议**: 每次更新前

