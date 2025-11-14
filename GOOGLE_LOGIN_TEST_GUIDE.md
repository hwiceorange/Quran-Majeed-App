# 🧪 Google 登录测试指南

## 📱 测试设备信息
- **设备型号**: Pixel 7
- **设备ID**: 35311FDH2000QP
- **Android 版本**: 测试设备
- **应用版本**: Debug Build

---

## 🎯 测试目标

验证 Google 登录功能在以下场景下正常工作：
1. ✅ 首次用户登录（Onboarding 流程）
2. ✅ 主页面登录
3. ✅ 登出功能
4. ✅ 账户切换
5. ✅ 与订阅功能共存

---

## 📋 测试前准备

### 1. 确认应用已安装
```bash
adb shell pm list packages | grep quran
```
期望输出：
```
package:com.quran.quranaudio.online
```

### 2. 确认设备连接
```bash
adb devices
```
期望输出：
```
35311FDH2000QP    device
```

### 3. 启用日志监控
在单独的终端窗口运行：
```bash
adb logcat -c  # 清除旧日志
adb logcat | grep -E "GoogleAuthManager|FirebaseAuth|GoogleSignIn"
```

---

## 🧪 测试用例

### 测试用例 1: 首次启动 - Onboarding 登录

#### 步骤
1. **完全卸载应用**（模拟首次安装）
   ```bash
   adb uninstall com.quran.quranaudio.online
   ```

2. **重新安装**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **启动应用**
   ```bash
   adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
   ```

4. **在应用中操作**
   - 应该看到 Onboarding 登录页面
   - 点击 "Sign in with Google" 按钮
   - 选择一个 Google 账户

#### 预期结果
- ✅ Google 账户选择器弹出
- ✅ 选择账户后，显示 "Welcome, [用户名]!" Toast
- ✅ 自动跳转到主页面
- ✅ 主页面显示用户头像和名称

#### 日志验证
期望看到以下日志：
```
D/GoogleAuthManager: Initializing GoogleSignInClient with Web Client ID: 517834286063-...
D/GoogleAuthManager: GoogleSignInClient initialized successfully
D/GoogleAuthManager: Google Play Services is available and up to date
D/GoogleAuthManager: handleSignInResult() called
D/GoogleAuthManager: GoogleSignInAccount retrieved successfully
D/GoogleAuthManager:   - Display Name: [用户名]
D/GoogleAuthManager:   - Email: [用户邮箱]
D/GoogleAuthManager:   - ID Token: Present
D/GoogleAuthManager: firebaseAuthWithGoogle:[用户ID]
D/GoogleAuthManager: signInWithCredential:success
```

#### 失败场景
如果出现 "Sign in canceled"，日志会显示：
```
E/GoogleAuthManager: Google Sign-In failed with ApiException
E/GoogleAuthManager:   - Status Code: 12501
E/GoogleAuthManager:   - Status Message: ...
```

---

### 测试用例 2: 主页面头像点击登录

#### 前提条件
- 应用已安装但未登录
- 或者已从测试用例 1 退出登录

#### 步骤
1. **启动应用**
   ```bash
   adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
   ```

2. **跳过 Onboarding**（如果显示）
   - 点击 "Skip" 按钮

3. **在主页面操作**
   - 找到顶部的用户头像图标
   - 点击头像
   - 点击 "Sign in with Google"
   - 选择 Google 账户

#### 预期结果
- ✅ Google 账户选择器弹出
- ✅ 选择账户后登录成功
- ✅ 头像更新为 Google 账户头像
- ✅ 显示用户名

#### UI 验证要点
- 登录前：显示默认头像
- 登录后：显示 Google 账户头像和用户名

---

### 测试用例 3: 退出登录

#### 步骤
1. **确保已登录**（从测试用例 1 或 2）

2. **退出登录**
   - 点击头像或打开侧边栏
   - 找到 "Sign Out" 或退出选项
   - 点击退出

#### 预期结果
- ✅ 用户信息清除
- ✅ 头像恢复为默认状态
- ✅ 不再显示用户名

#### 日志验证
```
D/GoogleAuthManager: User signed out
```

---

### 测试用例 4: 账户切换

#### 步骤
1. **使用账户 A 登录**（按测试用例 1 或 2）

2. **退出登录**

3. **使用账户 B 重新登录**
   - 点击 Google 登录
   - 选择不同的账户 B

#### 预期结果
- ✅ 成功切换到账户 B
- ✅ 显示账户 B 的头像和名称
- ✅ 没有残留账户 A 的信息

---

### 测试用例 5: 与订阅功能共存测试

#### 步骤
1. **登录 Google 账户**

2. **打开订阅页面**
   - 导航到订阅功能
   - 查看订阅套餐

3. **返回主页面**
   - 验证 Google 登录状态保持

4. **退出登录后再次打开订阅页面**
   - 验证订阅功能不受影响

#### 预期结果
- ✅ Google 登录和订阅功能互不干扰
- ✅ Billing Manager 和 Google Sign-In 可以同时工作
- ✅ 没有冲突错误

#### 日志验证
```
D/BillingManager: 🔧 Initializing Billing Client...
D/BillingManager: ✅ Billing setup successful
D/GoogleAuthManager: GoogleSignInClient initialized successfully
```
（两者应该都成功初始化）

---

### 测试用例 6: 网络异常处理

#### 步骤
1. **关闭设备网络**
   ```bash
   adb shell svc wifi disable
   adb shell svc data disable
   ```

2. **尝试 Google 登录**
   - 点击 Google 登录按钮
   - 观察应用行为

3. **恢复网络**
   ```bash
   adb shell svc wifi enable
   adb shell svc data enable
   ```

4. **再次尝试登录**

#### 预期结果
- ✅ 网络断开时显示友好错误提示
- ✅ 日志显示 "Network error"
- ✅ 网络恢复后可以正常登录

---

### 测试用例 7: Google Play Services 不可用

#### 模拟场景
（仅供参考，通常不需要实际测试）

如果 Google Play Services 不可用，日志会显示：
```
E/GoogleAuthManager: Google Play Services not available: [status code]
W/GoogleAuthManager: Google Play Services error is user-resolvable
```

应用应该：
- 提示用户更新 Google Play Services
- 或提供跳过登录的选项

---

## 📊 测试检查清单

### 功能检查
- [ ] 首次 Onboarding 登录成功
- [ ] 主页面头像登录成功
- [ ] 登出功能正常
- [ ] 账户切换正常
- [ ] 与订阅功能无冲突
- [ ] 网络异常有友好提示
- [ ] 用户头像正确显示
- [ ] 用户名正确显示

### 日志检查
- [ ] 无 "Sign in canceled" 错误（除非用户主动取消）
- [ ] Google Play Services 可用性检查通过
- [ ] Web Client ID 正确加载
- [ ] Firebase 认证成功
- [ ] 没有 ApiException status code 12501

### UI/UX 检查
- [ ] 登录按钮可点击
- [ ] Google 账户选择器正常弹出
- [ ] Toast 提示消息显示
- [ ] 页面跳转流畅
- [ ] 头像加载正常
- [ ] 没有界面卡顿或崩溃

---

## 🐛 常见问题排查

### 问题 1: 点击后立即显示 "Sign in canceled"

**排查步骤**：
1. 检查 SHA1 是否匹配
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```

2. 对比 Firebase Console 中的 SHA1

3. 检查 Web Client ID
   ```bash
   grep "default_web_client_id" app/build/generated/res/google-services/debug/values/values.xml
   ```

4. 运行诊断脚本
   ```bash
   ./diagnose_google_login_v2.sh
   ```

### 问题 2: 没有反应或长时间加载

**可能原因**：
- 网络问题
- Google Play Services 问题

**排查**：
```bash
# 查看详细日志
adb logcat -s GoogleAuthManager:D,FirebaseAuth:D

# 检查网络连接
adb shell ping -c 3 8.8.8.8
```

### 问题 3: 应用崩溃

**收集崩溃日志**：
```bash
adb logcat -s AndroidRuntime:E
```

查找 stack trace 并分析错误原因。

---

## 📸 测试截图建议

建议在测试过程中截图记录：
1. Onboarding 登录页面
2. Google 账户选择器
3. 登录成功后的主页面（显示头像和用户名）
4. 退出登录后的状态

截图命令：
```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

---

## ✅ 测试通过标准

所有以下条件必须满足：
1. ✅ 首次登录流程完整无错误
2. ✅ 主页面登录功能正常
3. ✅ 退出登录功能正常
4. ✅ 用户信息正确显示
5. ✅ 没有崩溃或严重错误
6. ✅ 日志显示正常的认证流程
7. ✅ 与订阅功能无冲突

---

## 📝 测试报告模板

### Google 登录测试报告

**测试日期**: ___________
**测试设备**: Pixel 7 (35311FDH2000QP)
**应用版本**: Debug Build
**测试人员**: ___________

#### 测试结果

| 测试用例 | 状态 | 备注 |
|---------|------|------|
| 首次 Onboarding 登录 | ☐ 通过 ☐ 失败 | |
| 主页面登录 | ☐ 通过 ☐ 失败 | |
| 退出登录 | ☐ 通过 ☐ 失败 | |
| 账户切换 | ☐ 通过 ☐ 失败 | |
| 订阅功能共存 | ☐ 通过 ☐ 失败 | |
| 网络异常处理 | ☐ 通过 ☐ 失败 | |

#### 发现的问题
1. 
2. 
3. 

#### 总体评价
☐ 通过所有测试，可以发布
☐ 有小问题，需要修复
☐ 有严重问题，需要重新测试

---

**测试完成！如有问题，请参考诊断脚本或查看修复完成报告。**


