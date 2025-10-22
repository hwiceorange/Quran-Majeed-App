# Google 登录失败修复指南 - Debug SHA-1 缺失问题

## 🔍 问题诊断

### 错误现象
```
Sign-in Canceled
StatusCode: 12501
```

### 根本原因 ⭐
**当前安装的是 Debug 版本 APK，但 Firebase Console 中只配置了 Play Store SHA-1，缺少 Debug SHA-1！**

---

## 📊 签名分析

### 当前设备安装的 APK 签名
```
类型：Debug 版本
SHA-1：8A:E5:E2:C3:9E:28:4C:7C:32:77:ED:2E:89:57:BF:08:AB:4F:9E:45
SHA-256：40:27:E1:2D:39:E5:90:B3:EC:45:E2:3E:7F:16:68:60:08:32:68:5E:34:6A:2C:5A:9D:6A:C3:5C:B6:71:52:08
Keystore：~/.android/debug.keystore
```

### Firebase Console 当前配置
```
✅ Play Store SHA-1：6D:C1:09:85:E2:07:82:42:15:EC:76:10:20:0F:37:41:EB:46:40:AB
❌ Debug SHA-1：未配置（缺失！）
```

### Google Play Console 配置
```
✅ SHA-1：6D:C1:09:85:E2:07:82:42:15:EC:76:10:20:0F:37:41:EB:46:40:AB
✅ SHA-256：82:FD:AB:C6:66:68:BB:6E:43:AD:33:B7:1D:25:5D:AA:19:A7:EF:D4:B8:D6:30:88:8B:F0:...
```

---

## 🔑 为什么需要两个 SHA-1？

```
┌─────────────────────────────────────────────────────────┐
│  开发环境（Debug）                                       │
│  - 使用 ~/.android/debug.keystore 签名                  │
│  - SHA-1: 8A:E5:E2:C3:9E:28:4C:7C:32:77:ED...          │
│  - 用于开发和测试                                        │
│  - ✅ 需要添加到 Firebase                               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  生产环境（Release）                                     │
│  - 上传到 Google Play 后由 Google 重新签名              │
│  - SHA-1: 6D:C1:09:85:E2:07:82:42:15:EC...             │
│  - 用于线上正式版本                                      │
│  - ✅ 已添加到 Firebase                                 │
└─────────────────────────────────────────────────────────┘
```

**结论**：两个环境使用不同的签名，因此都需要在 Firebase 中注册！

---

## ✅ 完整修复步骤

### 步骤 1：在 Firebase Console 添加 Debug SHA-1 ⭐⭐⭐

**URL**：
```
https://console.firebase.google.com/project/quran-majeed-aa3d2/settings/general
```

**操作**：
1. 登录 Firebase Console
2. 选择项目：**Quran Majeed** (quran-majeed-aa3d2)
3. 点击左侧 **设置图标 ⚙️** → **项目设置**
4. 向下滚动到 **"您的应用"**
5. 找到 Android 应用：`com.quran.quranaudio.online`
6. 在 **"SHA 证书指纹"** 部分，点击 **"添加指纹"**
7. **粘贴以下 Debug SHA-1**（完整复制）：
   ```
   8A:E5:E2:C3:9E:28:4C:7C:32:77:ED:2E:89:57:BF:08:AB:4F:9E:45
   ```
8. 点击 **"保存"**

**预期结果**：
```
Firebase Console 现在应显示两个 SHA-1：
✅ 6D:C1:09:85:E2:07:82:42:15:EC:76:10:20:0F:37:41:EB:46:40:AB (Play Store)
✅ 8A:E5:E2:C3:9E:28:4C:7C:32:77:ED:2E:89:57:BF:08:AB:4F:9E:45 (Debug)
```

---

### 步骤 2：下载新的 google-services.json ⭐⭐

**重要**：添加 SHA-1 后，必须重新下载 `google-services.json`！

1. 在同一页面（项目设置 → 常规）
2. 向下滚动到 **"您的应用"**
3. 找到 `google-services.json` 旁边的 **下载按钮**
4. 点击下载
5. 保存到 `~/Downloads/google-services.json`

**为什么必须重新下载？**
```
旧文件：
{
  "oauth_client": [
    {
      "client_id": "...",
      "certificate_hash": "6dc10985e207824215ec7610200f3741eb4640ab"  // 只有 Play Store
    }
  ]
}

新文件：
{
  "oauth_client": [
    {
      "client_id": "...",
      "certificate_hash": "6dc10985e207824215ec7610200f3741eb4640ab"  // Play Store
    },
    {
      "client_id": "...",
      "certificate_hash": "8ae5e2c39e284c7c3277ed2e8957bf08ab4f9e45"  // Debug (新增)
    }
  ]
}
```

---

### 步骤 3：替换项目中的 google-services.json

**方法 1：使用自动脚本（推荐）**

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
./replace_google_services.sh
```

脚本会：
- ✅ 自动备份旧文件（加时间戳）
- ✅ 复制新文件到项目
- ✅ 验证新文件包含 Debug SHA-1
- ✅ 检查 OAuth 客户端配置

**方法 2：手动替换**

```bash
cd /Users/huwei/AndroidStudioProjects/quran0

# 备份旧文件
cp app/google-services.json app/google-services.json.backup

# 替换新文件
cp ~/Downloads/google-services.json app/google-services.json

# 验证新文件
cat app/google-services.json | grep -i "8ae5e2c3"
# 应该能找到 Debug SHA-1
```

---

### 步骤 4：清理并重新编译

```bash
# 清理旧构建
./gradlew clean

# 重新编译并安装到设备
./gradlew installDebug --no-daemon
```

**预期输出**：
```
BUILD SUCCESSFUL in Xm Ys
Installing APK 'app-debug.apk' on 'Pixel 7' for :app:debug
Installed on 1 device.
```

---

### 步骤 5：测试 Google 登录

**方法 1：使用测试脚本**

```bash
./test_google_login.sh
```

脚本会自动：
1. 重启应用
2. 等待你在设备上点击 Google 登录
3. 分析登录日志
4. 显示成功或失败信息

**方法 2：手动测试**

1. 在设备上打开应用
2. 点击登录按钮
3. 选择 Google 登录
4. 选择账户并授权

**预期结果**：
```
✅ 成功：顺利完成 Google 登录，返回主页
✅ 日志显示：signInWithCredential:success
✅ 主页显示：用户头像和姓名
```

---

## 🔍 验证修复结果

### 检查 google-services.json 是否包含 Debug SHA-1

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
cat app/google-services.json | grep -c "certificate_hash"
```

**预期输出**：至少 `2` (一个 Play Store，一个 Debug)

### 检查 Firebase Console

访问：https://console.firebase.google.com/project/quran-majeed-aa3d2/settings/general

应该看到：
```
SHA 证书指纹：
✅ 6D:C1:09:85:E2:07:82:42:15:EC:76:10:20:0F:37:41:EB:46:40:AB  SHA-1
✅ 82:FD:AB:C6:66:68:BB:6E:43:AD:33:B7:1D:25:5D:AA:19:...      SHA-256
✅ 8A:E5:E2:C3:9E:28:4C:7C:32:77:ED:2E:89:57:BF:08:AB:4F:9E:45  SHA-1 (新添加)
```

### 检查应用日志

```bash
adb logcat -d | grep -E "(GoogleAuth|StatusCode)" | tail -20
```

**失败日志（修复前）**：
```
❌ GoogleAuthManager: Status Code: 12501
❌ GoogleAuthManager: Sign-in was canceled
```

**成功日志（修复后）**：
```
✅ GoogleAuthManager: ID Token: Present
✅ GoogleAuthManager: signInWithCredential:success
✅ FragMain: Google Sign-In SUCCESS!
```

---

## 🎯 故障排查清单

如果修复后仍然失败，请检查：

### 1. ✅ Firebase SHA-1 配置
- [ ] Debug SHA-1 已添加：`8A:E5:E2:C3:9E:28:4C:7C:32:77:ED:2E:89:57:BF:08:AB:4F:9E:45`
- [ ] Play Store SHA-1 已添加：`6D:C1:09:85:E2:07:82:42:15:EC:76:10:20:0F:37:41:EB:46:40:AB`
- [ ] 已点击"保存"按钮

### 2. ✅ google-services.json 文件
- [ ] 已下载最新版本（添加 SHA-1 之后下载）
- [ ] 已替换项目中的文件（app/google-services.json）
- [ ] 文件包含两个 `certificate_hash` 条目

### 3. ✅ 应用编译和安装
- [ ] 已运行 `./gradlew clean`
- [ ] 已运行 `./gradlew installDebug`
- [ ] 设备上安装的是最新版本

### 4. ✅ Firebase Authentication 配置
- [ ] Firebase Console → Authentication → Sign-in method
- [ ] Google 登录提供商已启用
- [ ] Support Email 已配置

### 5. ✅ Web Client ID 配置
```bash
# 检查代码中的 Web Client ID
grep "requestIdToken" app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java

# 应该显示：
# .requestIdToken("517834286063-52gsp24nqkb7sht7e7jn31397nhanumb.apps.googleusercontent.com")
```

---

## 📚 相关文档

- **Firebase SHA-1 配置文档**: https://developers.google.com/android/guides/client-auth
- **Google Sign-In 故障排查**: https://developers.google.com/identity/sign-in/android/troubleshooting
- **项目签名问题总结**: `签名问题快速修复.md`

---

## 🚀 快速命令参考

```bash
# 1. 获取 Debug SHA-1
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# 2. 替换 google-services.json
./replace_google_services.sh

# 3. 重新编译并安装
./gradlew clean && ./gradlew installDebug

# 4. 测试登录
./test_google_login.sh

# 5. 查看登录日志
adb logcat -d | grep -E "(GoogleAuth|StatusCode|Sign-in)"
```

---

## ✅ 总结

### 问题根源
- ❌ Firebase 中只有 Play Store SHA-1
- ❌ 缺少 Debug SHA-1
- ❌ 导致 Debug 版本无法通过 Google Sign-In 验证

### 解决方案
1. ✅ 在 Firebase Console 添加 Debug SHA-1
2. ✅ 重新下载 google-services.json
3. ✅ 替换项目文件并重新编译
4. ✅ 测试 Google 登录功能

### 预期结果
- ✅ Google 登录成功
- ✅ 用户信息正确显示
- ✅ 每日任务功能可用

---

**创建时间**: 2025-10-18  
**状态**: 等待用户在 Firebase Console 添加 Debug SHA-1  
**下一步**: 添加 SHA-1 → 下载新文件 → 运行 `./replace_google_services.sh`

