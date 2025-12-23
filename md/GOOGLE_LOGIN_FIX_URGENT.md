# 🚨 Google 登录 "Sign-in canceled" 紧急修复指南

## ❌ 问题诊断

### **症状**
- 点击头像图标
- Google 账户选择器弹出
- 选择账户后立即显示 "Sign-in canceled"
- 环境：Android 9 + 香港 WiFi

### **根本原因（已确认）**

检查您的 `app/google-services.json` 文件，发现：

```json
"oauth_client": []   // ⚠️ 空数组！这是问题根源！
```

**这意味着：**
- ❌ Firebase 项目中**没有配置任何 OAuth 客户端 ID**
- ❌ 没有 Web Client ID（用于 Firebase Auth）
- ❌ 可能没有添加 Android 客户端的 SHA-1 指纹
- ❌ Google Sign-In 无法验证应用身份，直接返回 "canceled"

**这不是用户取消，也不是网络问题，而是配置缺失！**

---

## ✅ 完整修复步骤

### **步骤 1: 获取应用的 SHA-1 指纹 ⭐⭐⭐ 最关键**

#### **方法 1: Debug SHA-1（用于测试）**

```bash
cd /Users/huwei/AndroidStudioProjects/quran0

keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

**预期输出**：
```
SHA1: A1:B2:C3:D4:E5:F6:...(40个字符)
```

#### **方法 2: Release SHA-1（用于正式版）**

```bash
# 如果有 release keystore
keytool -list -v -keystore app/quran_keystore -alias quran | grep SHA1

# 或者从 Google Play Console 获取
```

**⚠️ 复制这个 SHA-1 值，下一步要用！**

---

### **步骤 2: 在 Firebase Console 中配置**

1. **访问 Firebase Console**
   - 打开：https://console.firebase.google.com/
   - 选择项目：**quran-majeed-aa3d2**

2. **添加 SHA-1 指纹**
   - 点击左侧 **Project Settings**（项目设置）
   - 向下滚动到 "Your apps"（您的应用）
   - 找到 Android 应用：`com.quran.quranaudio.online`
   - 点击应用，找到 **"SHA certificate fingerprints"** 部分
   - 点击 **"Add fingerprint"** 按钮
   - 粘贴步骤 1 获取的 SHA-1 值
   - 点击 **"Save"** 保存

3. **启用 Google Sign-In**
   - 点击左侧 **Authentication**（身份验证）
   - 点击 **"Sign-in method"** 标签
   - 找到 **"Google"**
   - 如果是 "Disabled"，点击它并启用
   - 填写项目支持邮箱（必填）
   - 点击 **"Save"** 保存

4. **获取 Web Client ID** ⭐⭐⭐
   - 返回 **Project Settings** → **General** 标签
   - 向下滚动到 "Your apps"
   - 找到 **"Web client (auto created by Google Service)"** 或类似的 Web 应用
   - 复制 **Web Client ID**（格式：`123456789-xxxxx.apps.googleusercontent.com`）

5. **下载新的 google-services.json**
   - 在 **Project Settings** → **General** → **Your apps**
   - 找到您的 Android 应用
   - 点击 **"google-services.json"** 下载按钮
   - **替换** 项目中的旧文件：
     ```bash
     # 备份旧文件
     mv app/google-services.json app/google-services.json.old
     
     # 将下载的新文件复制到项目
     # 新文件中的 oauth_client 数组应该不再是空的
     ```

---

### **步骤 3: 配置代码中的 Web Client ID**

打开文件：`app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java`

找到第 46 行：

```java
.requestIdToken("YOUR_WEB_CLIENT_ID_HERE") // ⚠️ 替换这里
```

替换为步骤 2 获取的 Web Client ID：

```java
.requestIdToken("123456789-xxxxx.apps.googleusercontent.com") // 您的实际 Web Client ID
```

---

### **步骤 4: 重新编译并安装**

```bash
cd /Users/huwei/AndroidStudioProjects/quran0

# 清理旧的构建
./gradlew clean

# 重新编译并安装
./gradlew installDebug --no-daemon
```

---

### **步骤 5: 验证修复**

1. **检查新的 google-services.json**
   ```bash
   cat app/google-services.json | grep -A 20 "oauth_client"
   ```
   
   **应该看到**：
   ```json
   "oauth_client": [
     {
       "client_id": "123456789-xxxxx.apps.googleusercontent.com",
       "client_type": 3
     }
   ]
   ```
   
   **而不是**：
   ```json
   "oauth_client": []  // ❌ 这是错误的
   ```

2. **测试登录**
   - 启动应用
   - 点击头像图标
   - 选择 Google 账户
   - **应该成功登录，不再显示 "canceled"**

---

## 🌐 香港网络环境特别说明

### **为什么香港也可能有问题？**

1. **Google 服务连接性**
   - 香港可以访问 Google 服务
   - 但某些 WiFi 网络可能有限制或代理
   - Firebase 和 Google Play Services 需要稳定连接

2. **推荐网络配置**
   - ✅ 使用 4G/5G 移动网络（最稳定）
   - ✅ 使用香港本地 ISP 的 WiFi
   - ⚠️ 避免使用公司或学校的 WiFi（可能有防火墙）
   - ⚠️ 避免使用 VPN（可能干扰 Google Play Services）

3. **Android 9 特别注意**
   - Android 9 引入了网络安全配置
   - 某些 HTTPS 连接可能被限制
   - 确保 Google Play Services 是最新版本

---

## 🔍 诊断命令（逐步排查）

### **1. 检查 SHA-1 指纹**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

### **2. 检查 google-services.json 配置**
```bash
# 检查是否有 oauth_client 配置
cat app/google-services.json | grep -A 20 "oauth_client"

# 应该看到至少一个 client_id，而不是空数组
```

### **3. 检查 Google Play Services 版本**
```bash
adb shell dumpsys package com.google.android.gms | grep versionName
```

**最低要求**：版本 >= 20.0.0

### **4. 检查网络连接**
```bash
# 测试能否访问 Google 服务
adb shell ping -c 3 accounts.google.com

# 测试能否访问 Firebase
adb shell ping -c 3 firebaseapp.com
```

### **5. 查看详细错误日志**
```bash
adb logcat -c  # 清除旧日志
# 执行登录操作
adb logcat -d | grep -E "GoogleAuth|FragMain|StatusCode"
```

**关键日志**：
```log
✅ 成功（修复后）：
E/GoogleAuthManager:   - Status Code: 0 (或没有错误)
D/GoogleAuthManager: GoogleSignInAccount retrieved successfully

❌ 失败（修复前）：
E/GoogleAuthManager:   - Status Code: 12501
Sign-in was canceled
```

---

## 🎯 最可能的原因排序

根据 Android 9 + 香港 WiFi 环境，问题可能性排序：

| 排名 | 原因 | 可能性 | 解决方案 |
|------|------|--------|----------|
| 1 | **OAuth Client 未配置** | ⭐⭐⭐⭐⭐ | 按照步骤 1-4 配置 |
| 2 | **SHA-1 指纹未添加** | ⭐⭐⭐⭐ | 步骤 1-2 |
| 3 | **Web Client ID 未配置** | ⭐⭐⭐⭐ | 步骤 3 |
| 4 | Google Play Services 过旧 | ⭐⭐ | 在设备上更新 |
| 5 | 网络问题 | ⭐ | 切换到 4G 测试 |
| 6 | 权限问题 | ⭐ | 检查应用权限 |

---

## 📋 完整检查清单

在联系我之前，请完成：

- [ ] **步骤 1**：获取 SHA-1 指纹（复制保存）
- [ ] **步骤 2**：Firebase Console 中添加 SHA-1
- [ ] **步骤 2**：Firebase Console 中启用 Google Sign-In
- [ ] **步骤 2**：获取 Web Client ID（复制保存）
- [ ] **步骤 2**：下载新的 google-services.json
- [ ] **步骤 3**：在代码中配置 Web Client ID
- [ ] **步骤 4**：重新编译并安装应用
- [ ] **步骤 5**：验证新 google-services.json 不是空数组
- [ ] **测试**：在设备上测试登录
- [ ] **日志**：查看是否还有 "Status Code: 12501"

---

## 🆘 如果还是失败

### **提供以下信息**：

1. **SHA-1 指纹**（前后 6 位即可）：
   ```
   SHA1: A1B2C3...XYZ890
   ```

2. **新的 google-services.json 中的 oauth_client 部分**：
   ```bash
   cat app/google-services.json | grep -A 20 "oauth_client" > oauth_config.txt
   # 发送 oauth_config.txt（隐藏敏感信息）
   ```

3. **完整日志**：
   ```bash
   adb logcat -c
   # 执行登录操作
   adb logcat -d > login_error.txt
   # 发送 login_error.txt
   ```

4. **Google Play Services 版本**：
   ```bash
   adb shell dumpsys package com.google.android.gms | grep versionName
   ```

5. **网络测试结果**：
   ```bash
   adb shell ping -c 3 accounts.google.com
   ```

---

## 💡 快速测试脚本

保存为 `test_google_login.sh`：

```bash
#!/bin/bash

echo "======================================"
echo "Google 登录配置诊断"
echo "======================================"

echo ""
echo "1. 检查 SHA-1 指纹..."
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep SHA1

echo ""
echo "2. 检查 google-services.json..."
if grep -q '"oauth_client": \[\]' app/google-services.json; then
    echo "❌ 错误：oauth_client 是空数组！"
    echo "   必须在 Firebase Console 添加 SHA-1 指纹"
else
    echo "✅ oauth_client 已配置"
    cat app/google-services.json | grep -A 5 "oauth_client"
fi

echo ""
echo "3. 检查设备连接..."
adb devices | grep "device$"

echo ""
echo "4. 检查 Google Play Services..."
adb shell dumpsys package com.google.android.gms | grep versionName | head -1

echo ""
echo "5. 测试网络连接..."
adb shell ping -c 2 accounts.google.com 2>&1 | grep "bytes from"

echo ""
echo "======================================"
echo "诊断完成"
echo "======================================"
```

运行：
```bash
chmod +x test_google_login.sh
./test_google_login.sh
```

---

## ⚡ 紧急快速修复（5分钟）

如果您现在就需要测试，最快的方式：

```bash
# 1. 获取 SHA-1（复制输出）
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# 2. 去 Firebase Console (https://console.firebase.google.com/)
#    - 选择项目 quran-majeed-aa3d2
#    - Project Settings → Your apps → com.quran.quranaudio.online
#    - 添加 SHA-1 指纹（粘贴步骤1的输出）
#    - 下载新的 google-services.json
#    - 替换 app/google-services.json

# 3. 重新编译
./gradlew clean installDebug --no-daemon

# 4. 测试
adb logcat -c
# 在设备上测试登录
adb logcat | grep -E "GoogleAuth|StatusCode"
```

---

**关键点：`oauth_client` 数组不能是空的！必须有至少一个 Web Client ID 配置！**

这是 100% 的配置问题，与网络环境（香港 WiFi）和系统版本（Android 9）无关。

完成配置后，应该可以正常登录。✅

