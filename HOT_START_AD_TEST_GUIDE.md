# 热启动开屏广告测试指南

## 🎯 快速测试步骤

### 前提条件
- 确保设备/模拟器已连接
- 确保应用已安装

### 方法 1: 自动化测试（推荐）

```bash
./test_hot_start_splash_ad.sh
```

### 方法 2: 手动测试

#### 测试场景 1: 冷启动（验证原功能不受影响）

```bash
# 1. 强制停止应用
adb shell am force-stop com.quran.quranaudio.online

# 2. 等待2秒
sleep 2

# 3. 启动应用
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

**预期结果**：
- ✅ 显示启动页进度条
- ✅ 显示开屏广告
- ✅ 广告关闭后进入主界面

---

#### 测试场景 2: 热启动（验证修复效果）

```bash
# 1. 确保应用正在运行

# 2. 按Home键将应用切到后台
adb shell input keyevent KEYCODE_HOME

# 3. 等待5秒
sleep 5

# 4. 恢复应用
adb shell am start -n com.quran.quranaudio.online/.prayertimes.ui.MainActivity
```

**预期结果**：
- ✅ **应该显示开屏广告**（修复的重点）
- ✅ 广告关闭后回到之前的界面
- ✅ 应用功能正常

---

#### 测试场景 3: 多次热启动（验证广告预加载）

```bash
# 重复场景2的步骤，验证每次热启动都有广告
```

**预期结果**：
- ✅ 每次热启动都显示开屏广告
- ✅ 广告预加载机制工作正常

---

## 📊 查看日志

### 实时日志监控

```bash
adb logcat | grep -E "(App|ActivitySplash|AdFactory)" | grep -i "hot\|app.*open\|resume"
```

### 关键日志标识

**冷启动日志**：
```
ActivitySplash: ✅ Loading AppOpen Ad for all users
ActivitySplash: 🔄 [AppOpen] Preloading next app open ad for hot start
```

**热启动日志**（修复后新增）：
```
App: 🔄 Hot start detected, showing app open ad
App: ✅ App open ad is ready, showing...
App: 📱 App open ad closed, preloading next ad
```

---

## ✅ 验证清单

完成测试后，请确认：

- [ ] **冷启动** - 开屏广告正常显示
- [ ] **热启动** - 开屏广告正常显示（**修复的核心**）
- [ ] **多次热启动** - 每次都显示广告
- [ ] **功能完整性** - 应用所有功能正常工作
- [ ] **无崩溃** - 没有发生ANR或崩溃
- [ ] **日志正确** - 能看到预期的日志输出

---

## 🐛 故障排查

### 问题 1: 热启动不显示广告

**可能原因**：
1. 广告还在加载中（等待更长时间）
2. 网络问题（检查网络连接）
3. 测试广告快速关闭（使用正式广告ID测试）

**解决方法**：
```bash
# 查看详细日志
adb logcat | grep -E "AdFactory|App"
```

### 问题 2: 日志中看到 "App open ad not ready"

**原因**：广告预加载还未完成

**解决方法**：
- 等待更长时间后再测试热启动
- 检查网络连接
- 检查AdMob账号配置

### 问题 3: 应用卡死

**这不应该发生**（已有15秒超时保护）

**如果发生**：
```bash
# 查看日志
adb logcat | grep -E "FAILSAFE|timeout"

# 强制停止
adb shell am force-stop com.quran.quranaudio.online
```

---

## 📝 测试报告模板

```
测试日期: ________
测试人员: ________
应用版本: ________

测试结果：
□ 冷启动测试: PASS / FAIL
□ 热启动测试: PASS / FAIL
□ 多次热启动: PASS / FAIL
□ 功能完整性: PASS / FAIL
□ 稳定性测试: PASS / FAIL

问题描述：
_________________________________
_________________________________
_________________________________

日志附件：
□ 已保存日志文件
```

---

## 🚀 快速验证命令

一键执行所有测试：

```bash
# 冷启动
adb shell am force-stop com.quran.quranaudio.online && sleep 2 && adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity && echo "观察冷启动广告..." && sleep 15

# 热启动
adb shell input keyevent KEYCODE_HOME && sleep 5 && adb shell am start -n com.quran.quranaudio.online/.prayertimes.ui.MainActivity && echo "观察热启动广告..."
```

---

**注意**: 首次测试请使用自动化脚本 `./test_hot_start_splash_ad.sh`，它包含完整的测试流程和日志分析。

