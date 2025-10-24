# 广告展示策略分析与修复计划

## 📊 当前广告展示策略分析

### 1. 广告系统架构

应用中使用了 **新的广告系统 (`adlib`模块)**，通过 `AdFactory` 进行统一管理。

#### 广告ID配置 (`AdConfig.kt`)
- `AD_APPOPEN` - 开屏广告
- `AD_NEW_USER_INTERS` - 新用户专属插屏广告 (高价值)
- `AD_INTERS` - 普通插屏广告
- `AD_NATIVE` - 原生广告
- `AD_BANNER` - 横幅广告
- `AD_QUIZ_INTERS` - Quiz模块插屏广告
- `AD_QUIZ_REWARD` - Quiz模块激励广告

---

### 2. 当前广告展示场景汇总

#### ✅ 场景1：**开屏广告** (`SplashScreenActivity`)
- **位置**: `SplashScreenActivity.java` 第63行和第117-118行
- **触发时机**: 应用启动时
- **广告类型**: 开屏广告 (`AdConfig.AD_APPOPEN`)
- **实现逻辑**:
  ```java
  // 加载广告
  AdFactory.INSTANCE.loadAppOpenAd(this, AdConfig.AD_APPOPEN, null);
  
  // 展示广告
  if(AdFactory.INSTANCE.hasAppOpenAd(AdConfig.AD_APPOPEN)){
     AdFactory.INSTANCE.showAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, callback);
  }
  ```
- **当前状态**: ✅ **已实现，正常运行**

---

#### ✅ 场景2：**退出应用时的提示对话框** (`MainActivity`)
- **位置**: `MainActivity.java` 第193-222行
- **触发时机**: 用户按返回键退出应用时
- **内容**: 评分对话框 (无广告，仅引导评分)
- **当前状态**: ✅ **无广告展示，符合要求**

---

#### ✅ 场景3：**主界面 (FragMain)** 
- **位置**: `FragMain.java`
- **当前状态**: ✅ **广告代码已移除** (第93行注释显示: "Ad-related variables removed")
- **原先**: 可能有横幅广告
- **现状**: **无广告展示**

---

#### ✅ 场景4：**古兰经阅读页面** (`ActivityReader`)
- **位置**: `ActivityReader.java`
- **当前状态**: ✅ **未发现任何广告代码**
- **检查结果**: **无广告展示，符合要求**

---

#### ✅ 场景5：**Hadith活动页面** (`HadithActivity`)
- **位置**: `HadithActivity.java` 第109行
- **当前状态**: ✅ **横幅广告方法已移除** (注释: `// loadBannerAd方法已移除`)
- **现状**: **无广告展示**

---

#### ✅ 场景6：**Zakat计算器** (`ZakatFragment.kt`)
- **位置**: `ZakatFragment.kt` 第101行
- **当前状态**: ✅ **横幅广告方法已移除** (注释: `// loadBannerAd方法已移除`)
- **现状**: **无广告展示**

---

#### ⚠️ 场景7：**Quiz模块插屏广告**
- **位置**: `quiz/src/main/java/com/quran/quranaudio/quiz/extension/AdActivityExtension.kt`
- **触发时机**: 
  - Quiz答题完成后
  - 查看提示时
  - 其他Quiz相关操作
- **广告类型**: 
  - 插屏广告 (`AD_QUIZ_INTERS`)
  - 激励广告 (`AD_QUIZ_REWARD`)
- **当前状态**: ⚠️ **已实现，正常运行，需要检查新用户策略**

---

### 3. 新用户检测机制

#### 📍 新用户标识逻辑
- **文件**: `quiz/src/main/java/com/quran/quranaudio/quiz/utils/UserInfoUtils.kt`
- **实现**:
  ```kotlin
  object UserInfoUtils {
      fun isNewUser(): Boolean = SPTools.getBoolean(Constants.KEY_IS_NEW_USER, true)
      fun setOldUser() {
          SPTools.put(Constants.KEY_IS_NEW_USER, false)
      }
  }
  ```
- **首次安装检测**: `quiz/src/main/java/com/quran/quranaudio/quiz/utils/AppConfig.kt`
  ```kotlin
  val isInstallFirstDay: Boolean
      get() {
          val installDate = TimeUtils.millis2String(firstInstallTime, "yyyyMMdd")
          return installDate == getTodayDate()
      }
  ```

#### 📍 Discover模块位置 ✅ 已确认
- **名称**: "99 Names of Allah" (Discover) - 显示为 "Discover"
- **底部导航**: `bottom_nav_menu.xml` 中的 `@+id/nav_name_99`
- **实际Fragment**: `com.quran.quranaudio.quiz.fragments.QuranQuestionFragment`
- **说明**: ⚠️ **Discover按钮实际打开的是Quiz模块，而非原来的99 Names功能**
- **导航配置**: `nav_graphmain.xml` 第12-14行

---

## 🎯 用户需求分析

### 需求1: 新用户首次安装当天的广告策略
**要求**: 新用户首次安装应用，当天除了Discover模块，其他功能界面及开屏都不展示广告，同时也不请求广告，只有用户在进入了Discover模块后再触发请求正常展示。

**影响范围**:
- ❌ 开屏广告 (SplashScreenActivity) - 需要禁用
- ✅ 主界面 (FragMain) - 已无广告
- ✅ 古兰经阅读页 (ActivityReader) - 已无广告
- ⚠️ Quiz模块广告 - 需要添加首日检测
- ⚠️ Discover模块 - 需要定位并确认广告逻辑

---

### 需求2: 老用户广告策略
**要求**: 老用户都正常展示及启动正常请求不影响。

**实现**: ✅ 保持现有逻辑即可

---

### 需求3: 古兰经内容页广告
**要求**: 古兰经内容页不展示广告，如果有广告则移除。

**检查结果**: ✅ **已确认无广告代码**

---

### 需求4: 插屏广告展示策略
**要求**: 检查目前代码中的插屏广告展示策略及时机，按场景说明。

**检查结果**: 
- ✅ **主应用模块**: 未发现插屏广告调用
- ⚠️ **Quiz模块**: 存在插屏广告，详见"场景7"

---

## 🔧 修复计划

### 修复1: 开屏广告新用户首日禁用
**文件**: `app/src/main/java/com/quran/quranaudio/online/SplashScreenActivity.java`

**修改逻辑**:
```java
// 在 onCreate 方法中，修改广告加载逻辑
import com.quran.quranaudio.quiz.utils.AppConfig;
import com.quran.quranaudio.quiz.utils.UserInfoUtils;

// 只在非新用户首日才加载开屏广告
if (!UserInfoUtils.INSTANCE.isNewUser() || !AppConfig.INSTANCE.isInstallFirstDay()) {
    AdFactory.INSTANCE.loadAppOpenAd(this, AdConfig.AD_APPOPEN, null);
}

// 在展示广告前也检查
if (!UserInfoUtils.INSTANCE.isNewUser() || !AppConfig.INSTANCE.isInstallFirstDay()) {
    if(AdFactory.INSTANCE.hasAppOpenAd(AdConfig.AD_APPOPEN)){
       AdFactory.INSTANCE.showAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, callback);
    }
}
```

---

### 修复2: Quiz模块广告新用户首日策略
**文件**: `quiz/src/main/java/com/quran/quranaudio/quiz/extension/AdActivityExtension.kt`

**修改逻辑**: 在所有广告展示方法中添加首日检测：
```kotlin
fun Activity.showInterAdByPoolNew(
    adPosition: String,
    functionTag: String,
    level: Int = 0,
    beforeShowCallbacks: ((Boolean) -> Unit)?,
    callbacks: Function1<Boolean, Unit>,
    showCallback: (() -> Unit)? = null
) {
    // ⭐ 新增：新用户首日不展示广告
    if (UserInfoUtils.isNewUser() && AppConfig.isInstallFirstDay) {
        callbacks.invoke(false)
        return
    }
    
    // 原有逻辑...
    var canShow = CloudManager.adShowPercent(level)
    // ...
}
```

同样修改 `showRewardAd` 和 `showGemAd` 方法。

---

### 修复3: Discover模块（Quiz）触发广告 ✅ 已确认
**文件**: `quiz/src/main/java/com/quran/quranaudio/quiz/fragments/QuranQuestionFragment.kt`

**说明**: Discover模块实际上就是Quiz模块（`QuranQuestionFragment`）

**修改逻辑**: 在用户首次进入Quiz界面时，标记为老用户并开始广告预加载
```kotlin
override fun onResume() {
    super.onResume()
    
    // ⭐ 新增：用户进入Discover模块后，标记为老用户，后续可以展示广告
    if (UserInfoUtils.isNewUser()) {
        UserInfoUtils.setOldUser()
        
        // 预加载广告，供下次使用
        activity?.let {
            AdFactory.loadInterstitialAd(it, AdConfig.AD_INTERS, null)
            AdFactory.loadAppOpenAd(it, AdConfig.AD_APPOPEN, null)
        }
    }
}
```

**关键点**:
1. 只有当用户主动打开Discover（Quiz）界面后，才将其标记为老用户
2. 标记后立即预加载广告，但**当前会话不展示广告**
3. 下次应用启动时，由于已不是新用户，开屏广告会正常展示

---

## 📝 关键确认信息

1. ✅ **Discover模块位置**: `QuranQuestionFragment` (Quiz模块)
2. ✅ **新用户定义**: 按首次安装当天计算 (`isInstallFirstDay`)
3. ✅ **Discover触发时机**: 用户打开Discover页面（底部导航第3个按钮）时触发

## 🔍 关键技术细节

### 新用户状态转换流程
```
首次安装 → isNewUser=true, isInstallFirstDay=true
      ↓
打开应用（当天）→ 开屏广告被禁止，其他界面也不展示广告
      ↓
点击Discover按钮 → 进入QuizFragment，调用setOldUser()
      ↓
后续操作 → isNewUser=false，但isInstallFirstDay仍为true
      ↓
第二天打开应用 → isNewUser=false, isInstallFirstDay=false
      ↓
正常展示所有广告
```

### 广告展示规则总结
| 场景 | 新用户首日（未进Discover） | 新用户首日（已进Discover） | 老用户 / 第二天 |
|------|---------------------------|---------------------------|----------------|
| 开屏广告 | ❌ 不展示，不请求 | ❌ 不展示，不请求 | ✅ 正常展示 |
| Quiz插屏 | ❌ 不展示，不请求 | ❌ 不展示，不请求 | ✅ 正常展示 |
| Quiz激励 | ❌ 不展示，不请求 | ❌ 不展示，不请求 | ✅ 正常展示 |
| 古兰经阅读 | ✅ 无广告 | ✅ 无广告 | ✅ 无广告 |
| 其他界面 | ✅ 无广告 | ✅ 无广告 | ✅ 无广告 |

---

## ✅ 下一步行动

**准备完毕，等待您的确认：**

### 需要修改的文件清单：
1. ✅ `SplashScreenActivity.java` - 开屏广告新用户首日禁用
2. ✅ `AdActivityExtension.kt` - Quiz广告新用户首日禁用
3. ✅ `QuranQuestionFragment.kt` - Discover触发标记为老用户

### 修改影响评估：
- ✅ **风险等级**: 低
- ✅ **影响范围**: 仅影响新用户首日的广告展示逻辑
- ✅ **老用户影响**: 无影响
- ✅ **功能完整性**: 不影响任何现有功能

---

**请确认是否开始实施修复？**

