# 构建和测试指南 (Build and Test Guide)

本指南提供了详细的构建和测试步骤，用于验证以下两个功能：
1. 订阅用户广告屏蔽
2. Qada计算规则统一

## 📋 前置准备

### 1. 设备准备
- Android设备或模拟器（API 21+）
- 启用USB调试
- 连接到电脑

### 2. 验证设备连接
```bash
adb devices
# 应该看到你的设备
```

### 3. 确保应用数据清空（可选，用于干净测试）
```bash
# 备份现有数据（如果需要）
adb backup -f backup.ab com.quran.quranaudio.online

# 清除应用数据
adb shell pm clear com.quran.quranaudio.online
```

---

## 🔨 构建步骤

### 方法一：使用Android Studio（推荐）

1. **打开项目**
   - 在Android Studio中打开项目
   - 等待Gradle同步完成

2. **构建Debug版本**
   - 菜单：Build → Make Project
   - 或：Build → Build Bundle(s) / APK(s) → Build APK(s)

3. **安装到设备**
   - 点击运行按钮 ▶️
   - 或：菜单 Run → Run 'app'

### 方法二：命令行构建（需要Java环境）

```bash
cd /Users/huwei/AndroidStudioProjects/quran0

# 清理
./gradlew clean

# 构建Debug APK
./gradlew assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 测试一：订阅用户广告屏蔽功能

### 测试场景1：免费用户（默认状态）

**步骤：**
1. 确保订阅状态为非订阅（清除数据或检查状态）
   ```bash
   # 检查订阅状态
   adb shell "run-as com.quran.quranaudio.online cat shared_prefs/subscription_prefs.xml" 2>/dev/null || echo "No subscription"
   ```

2. 启动日志监控
   ```bash
   adb logcat -c  # 清除旧日志
   adb logcat -v time *:S AdFactory:D SubscriptionChecker:D SimpleBannerAdListener:D
   ```

3. 打开应用并导航到有广告的页面：
   - Wudu Guide（底部横幅广告）
   - Qada Tracker（底部横幅广告）

**预期结果：**
```
SubscriptionChecker: 📊 Subscription check: false
AdFactory: 📢 loadBannerAd: position=BANNER_WUDU, functionTag=WuduGuide
AdFactory: 🚀 Banner ad request sent for WuduGuide
SimpleBannerAdListener: ✅ Banner ad loaded successfully for WuduGuide
SimpleBannerAdListener: 👁️ Banner container now VISIBLE
```

✅ **验证点：**
- [ ] 日志显示 `Subscription check: false`
- [ ] 看到广告请求被发送
- [ ] 横幅广告容器变为可见
- [ ] 实际看到广告展示

---

### 测试场景2：付费订阅用户

**步骤：**
1. 设置订阅状态为已订阅
   ```bash
   # 方法1：使用adb直接设置（测试用）
   adb shell "run-as com.quran.quranaudio.online sh -c 'mkdir -p shared_prefs && echo \"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
  <boolean name=\\\"is_subscribed\\\" value=\\\"true\\\" />
  <string name=\\\"product_id\\\">yearly_plan</string>
  <long name=\\\"last_check_time\\\" value=\\\"$(date +%s)000\\\" />
</map>\" > shared_prefs/subscription_prefs.xml'"
   
   # 验证设置成功
   adb shell "run-as com.quran.quranaudio.online cat shared_prefs/subscription_prefs.xml"
   ```

2. 重启应用（强制停止后重新打开）
   ```bash
   adb shell am force-stop com.quran.quranaudio.online
   adb shell am start -n com.quran.quranaudio.online/.online.home.HomeActivity
   ```

3. 启动日志监控
   ```bash
   adb logcat -c
   adb logcat -v time *:S AdFactory:D SubscriptionChecker:D SimpleBannerAdListener:D
   ```

4. 导航到有广告的页面：
   - Wudu Guide
   - Qada Tracker
   - 尝试观看激励广告解锁Tafsir

**预期结果：**
```
SubscriptionChecker: 📊 Subscription check: true
AdFactory: 🎁 User is subscribed, skipping banner ad for WuduGuide
AdFactory: 🎁 User is subscribed, skipping reward ad
```

✅ **验证点：**
- [ ] 日志显示 `Subscription check: true`
- [ ] 日志显示 `🎁 User is subscribed, skipping...`
- [ ] 没有广告请求被发送
- [ ] 广告容器保持隐藏（不占用空间）
- [ ] 界面干净，无广告展示
- [ ] 无激励广告按钮或自动解锁内容

---

### 测试场景3：订阅状态切换

**步骤：**
1. 从订阅用户切换到免费用户
   ```bash
   # 清除订阅状态
   adb shell "run-as com.quran.quranaudio.online rm -f shared_prefs/subscription_prefs.xml"
   
   # 重启应用
   adb shell am force-stop com.quran.quranaudio.online
   adb shell am start -n com.quran.quranaudio.online/.online.home.HomeActivity
   ```

2. 验证广告恢复显示

✅ **验证点：**
- [ ] 清除订阅后，广告恢复正常显示
- [ ] 日志显示状态从 `true` 变为 `false`

---

### 测试所有广告类型

访问以下页面，验证所有广告类型都受订阅状态控制：

| 页面 | 广告类型 | 位置 | 测试步骤 |
|------|---------|------|---------|
| Wudu Guide | Banner | 底部 | 打开Wudu Guide页面 |
| Qada Tracker (Weekly) | Banner | 底部 | 打开Qada Tracker，查看周视图 |
| Qada Tracker (Monthly) | Banner | 底部 | 切换到月视图 |
| Tafsir Content | Rewarded | 解锁按钮 | 点击锁定的Tafsir内容 |
| App Launch | App Open | 启动时 | 关闭应用后重新打开 |
| Between Actions | Interstitial | 转场 | 多次切换页面 |

**测试矩阵：**
```
✅ = 通过  ❌ = 失败  ⏭️ = 跳过

                      | 免费用户 | 订阅用户 |
---------------------|---------|---------|
Banner (Wudu)        |    ✅   |    ✅   |
Banner (Qada Week)   |    ✅   |    ✅   |
Banner (Qada Month)  |    ✅   |    ✅   |
Rewarded (Tafsir)    |    ✅   |    ✅   |
App Open             |    ✅   |    ✅   |
Interstitial         |    ✅   |    ✅   |
```

---

## 🧪 测试二：Qada计算规则统一

### 测试场景1：验证计算一致性

**前置条件：**
- 用户已登录
- 已设置Qada起始日期
- 有一些祷告记录（包括今天）

**步骤：**
1. 启动日志监控
   ```bash
   adb logcat -c
   adb logcat -v time *:S QadaDiagnosis:D PrayerLogRepository:D QadaTrackerActivity:D
   ```

2. 打开Salat页面（Home页面）
   - 观察Total Qada卡片显示的数字
   - 记录Outstanding数量

3. 点击Total Qada卡片，进入Qada Tracker
   - 默认显示周视图
   - 切换到月视图
   - 观察完成率

4. 分析日志输出

**预期日志输出：**

```
=== PrayerLogRepository计算 ===
PrayerLogRepository: ✅ 【统一计算规则】Computing Qada summary from 2025-11-05 to 2025-11-17
PrayerLogRepository:    📌 This calculation is shared between Salat page and Qada Tracker
PrayerLogRepository: 🔍 Processing TODAY (2025-11-17) prayers:
PrayerLogRepository:    todayPrayerTimes: ✅ Available
PrayerLogRepository:    todayPrayerTimes.timings: ✅ Available
PrayerLogRepository: ✅ Prayer: Fajr, Next: 12:30, Now: 14:00, Started: true
PrayerLogRepository:    ✅ COUNTED: Fajr -> ADA
PrayerLogRepository: ✅ Prayer: Dhuhr, Next: 15:30, Now: 14:00, Started: false
PrayerLogRepository:    ⏭️ SKIPPED: Dhuhr (window not started)
...

QadaDiagnosis: ═══════════════════════════════════════════════
QadaDiagnosis: 📊 【统一计算规则】Qada Summary (Used by Both Salat & QadaTracker)
QadaDiagnosis:    ✅ Calculation Source: PrayerLogRepository.getQadaSummary()
QadaDiagnosis:    📅 Date Range: 2025-11-05 to 2025-11-17
QadaDiagnosis:    📆 Total Days: 13
QadaDiagnosis:    🔢 Total Counted Prayers: 62
QadaDiagnosis:    ❌ Outstanding (Missed/Pending): 1
QadaDiagnosis:    ✅ Completed (Qada): 0
QadaDiagnosis:    📈 Expected for full period: 65 prayers
QadaDiagnosis:    ⏭️  Unstarted today: 3
QadaDiagnosis: ═══════════════════════════════════════════════

=== Salat页面显示 ===
QadaDiagnosis: ═══════════════════════════════════════════════
QadaDiagnosis: 📊 【统一计算规则】Salat Page - Total Qada Count
QadaDiagnosis:    ✅ Calculation Source: PrayerLogRepository.getQadaSummary()
QadaDiagnosis:    ❌ Outstanding (Missed+Pending): 1
QadaDiagnosis:    ✅ Completed (Qada'): 0
QadaDiagnosis:    🔢 Total: 1
QadaDiagnosis:    📌 This is the same calculation used by QadaTracker
QadaDiagnosis: ═══════════════════════════════════════════════

=== QadaTracker月视图 ===
QadaDiagnosis: ═══════════════════════════════════════════════
QadaDiagnosis: 📊 【统一计算规则】QadaTracker Monthly Tab - Completion Rate
QadaDiagnosis:    ✅ Prayer Window Check: PrayerLogRepository.hasPrayerWindowStarted()
QadaDiagnosis:    📅 Month Range: 2025-11-01 to 2025-11-30
QadaDiagnosis:    ✅ Completed (Ada'+Qada'): 59
QadaDiagnosis:    🔢 Total Prayers: 60
QadaDiagnosis:    📈 Completion Rate: 98%
QadaDiagnosis:    📌 Calculation Range: Current month only
QadaDiagnosis: ═══════════════════════════════════════════════
```

✅ **关键验证点：**
- [ ] 所有日志都包含 `【统一计算规则】` 标识
- [ ] PrayerLogRepository的计算只执行一次
- [ ] Salat页面和QadaTracker都使用相同的数据源
- [ ] Outstanding数量一致
- [ ] 今天未开始的祷告被正确跳过
- [ ] 实际祷告时间被使用（不是fallback）

---

### 测试场景2：今天祷告时间判断

**目的：** 验证今天的祷告是否按实际时间正确判断

**测试时间点：**

| 当前时间 | 应该计入的祷告 | 不应计入的祷告 |
|---------|--------------|--------------|
| 08:00 AM | 无 | Fajr, Dhuhr, Asr, Maghrib, Isha |
| 01:00 PM | Fajr | Dhuhr, Asr, Maghrib, Isha |
| 04:00 PM | Fajr, Dhuhr | Asr, Maghrib, Isha |
| 07:00 PM | Fajr, Dhuhr, Asr | Maghrib, Isha |
| 09:00 PM | Fajr, Dhuhr, Asr, Maghrib | Isha |
| 11:00 PM | 全部 | 无 |

**测试步骤：**
1. 在不同时间点打开应用
2. 检查日志中的祷告判断
3. 验证 `⏭️ SKIPPED` 和 `✅ COUNTED` 日志

**日志示例（下午2:00）：**
```
PrayerLogRepository: ✅ Prayer: Fajr, Next: 12:30, Now: 14:00, Started: true
PrayerLogRepository:    ✅ COUNTED: Fajr -> [状态]
PrayerLogRepository: ✅ Prayer: Dhuhr, Next: 15:30, Now: 14:00, Started: false
PrayerLogRepository:    ⏭️ SKIPPED: Dhuhr (window not started)
```

✅ **验证点：**
- [ ] 已过时间的祷告被计入
- [ ] 未开始的祷告被跳过
- [ ] 使用实际祷告时间而非固定时间

---

### 测试场景3：数据一致性验证

**步骤：**
1. 记录Salat页面的Outstanding数量: `______`
2. 进入Qada Tracker月视图
3. 计算未完成数量：`(Total Prayers - Completed) = ______`
4. 对比两个数字

**计算示例：**
```
Salat页面：
  Outstanding = 5

Qada Tracker月视图：
  Total Prayers = 65
  Completed = 60
  未完成 = 65 - 60 = 5
  
✅ 一致！
```

✅ **验证点：**
- [ ] 两个数字完全一致
- [ ] 如果包含今天，确保计算范围一致
- [ ] 日志显示使用相同的计算源

---

## 📊 测试报告模板

### 功能一：订阅用户广告屏蔽

**测试环境：**
- 设备：_____________
- Android版本：_______
- 应用版本：_________
- 测试日期：_________

**测试结果：**
```
□ 免费用户看到广告
□ 订阅用户不看到广告  
□ 订阅用户无广告请求
□ 所有广告类型都受控制
□ 状态切换正常工作
```

**问题记录：**
_________________________

---

### 功能二：Qada计算统一

**测试环境：**
- 设备：_____________
- 测试日期：_________
- 测试时间：_________

**测试结果：**
```
□ 日志包含【统一计算规则】标识
□ Salat和QadaTracker数据一致
□ 今天祷告时间判断正确
□ 使用实际祷告时间
□ 未开始的祷告被跳过
```

**数据验证：**
```
Salat页面 Outstanding: ______
QadaTracker 未完成: ______
是否一致: □ 是  □ 否
```

**问题记录：**
_________________________

---

## 🐛 常见问题排查

### 问题1：广告仍然显示（订阅用户）

**检查：**
```bash
# 1. 验证订阅状态
adb shell "run-as com.quran.quranaudio.online cat shared_prefs/subscription_prefs.xml"

# 2. 检查日志
adb logcat -v time | grep -E "SubscriptionChecker|User is subscribed"

# 3. 重启应用
adb shell am force-stop com.quran.quranaudio.online
```

**可能原因：**
- SharedPreferences未正确设置
- 应用缓存旧状态（需要重启）
- 代码未正确编译（重新构建）

---

### 问题2：Qada数据不一致

**检查：**
```bash
# 查看完整计算日志
adb logcat -v time | grep -E "QadaDiagnosis|PrayerLogRepository"

# 检查是否使用统一规则
adb logcat -v time | grep "【统一计算规则】"
```

**可能原因：**
- 计算时间不同（早上vs晚上）
- 祷告时间数据未加载
- 代码未正确编译

---

### 问题3：找不到日志输出

**解决：**
```bash
# 清除并重新开始
adb logcat -c

# 使用更宽的过滤
adb logcat -v time | grep -E "AdFactory|Subscription|Qada|Prayer"

# 或者查看所有日志
adb logcat -v time
```

---

## 📝 完成检查清单

### 构建阶段
- [ ] 代码无编译错误
- [ ] Gradle同步成功
- [ ] APK成功构建
- [ ] 应用成功安装到设备

### 订阅广告屏蔽测试
- [ ] 免费用户能看到广告
- [ ] 订阅用户不看到广告
- [ ] 日志显示正确的订阅状态
- [ ] 所有广告类型都已测试
- [ ] 状态切换正常

### Qada计算统一测试
- [ ] 日志包含【统一计算规则】标识
- [ ] Salat和QadaTracker数据一致
- [ ] 今天的祷告判断正确
- [ ] 使用实际祷告时间
- [ ] 不同时间点测试通过

---

## 🎯 下一步

完成测试后：
1. 记录所有测试结果
2. 截图关键日志和界面
3. 报告任何发现的问题
4. 确认功能符合预期

如需帮助，请提供：
- 完整日志输出
- 截图
- 设备信息
- 重现步骤

