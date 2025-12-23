# 🔍 Google 登录失败完整诊断与修复指南

## ❌ 问题描述

**症状**: 在登录弹窗选择 Google 邮箱后，持续失败，显示 "Sign-in canceled" (错误码 12501)

**现象**: 
- Google 登录界面正常弹出 ✅
- 可以选择 Google 账号 ✅
- 选择后立即失败 ❌
- 错误：`SIGN_IN_CANCELLED` (Status Code: 12501)

---

## 🔍 根本原因分析

### 1. **SHA-1 证书指纹不匹配**

Google Sign-In 要求应用的 SHA-1 证书指纹必须在 Firebase Console 中注册。

**检测到的问题**：
```java
// GoogleAuthManager.java 第 46 行
.requestIdToken("517834286063-52gsp24nqkb7sht7e7jn31397nhanumb.apps.googleusercontent.com")
```

这个 Web Client ID 是正确的，但问题出在：
- ⚠️ Debug APK 使用的签名可能没有在 Firebase 中注册
- ⚠️ 或者 SHA-1 指纹不匹配

### 2. **google-services.json 中的配置**

当前配置有两个 Android 客户端：

```json
{
  "client_type": 1,
  "android_info": {
    "package_name": "com.quran.quranaudio.online",
    "certificate_hash": "6dc10985e207824215ec7610200f3741eb4640ab"  // SHA-1 #1
  }
},
{
  "client_type": 1,
  "android_info": {
    "package_name": "com.quran.quranaudio.online",
    "certificate_hash": "8ae5e2c39e284c7c3277ed2e8957bf08ab4f9e45"  // SHA-1 #2
  }
}
```

---

## 🛠️ 解决方案

### 方案 1：添加 Debug Keystore SHA-1 到 Firebase（推荐）

#### **步骤 1：获取 Debug Keystore SHA-1**

```bash
# macOS/Linux
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android | grep SHA1

# 或使用项目中的 Java
/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin/keytool \
  -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android
```

#### **步骤 2：获取 Release Keystore SHA-1**

```bash
keytool -list -v -keystore app/quran_keystore \
  -alias key0 -storepass Huwei123 -keypass Huwei123 | grep SHA1
```

#### **步骤 3：在 Firebase Console 添加 SHA-1**

1. **登录 Firebase Console**
   - 访问：https://console.firebase.google.com
   - 选择项目：quran-majeed-aa3d2

2. **进入项目设置**
   ```
   Project Settings → General → Your apps → Android app
   ```

3. **添加 SHA 证书指纹**
   - 点击 **"Add fingerprint"**
   - 粘贴 Debug Keystore 的 SHA-1
   - 点击 **"Save"**
   - 再次点击 **"Add fingerprint"**
   - 粘贴 Release Keystore 的 SHA-1
   - 点击 **"Save"**

4. **下载新的 google-services.json**
   - 点击 **"Download google-services.json"**
   - 替换项目中的 `app/google-services.json`
   - 重新编译应用

5. **等待生效**
   - ⏰ 通常需要 **5-10 分钟**
   - 有时需要 **最多 1 小时**

---

### 方案 2：使用 Release 签名的 Debug 版本

如果 Release Keystore 的 SHA-1 已经在 Firebase 中注册，可以让 Debug 版本也使用 Release 签名：

#### **修改 build.gradle**

```gradle
android {
    ...
    buildTypes {
        debug {
            // 使用 release 签名配置
            signingConfig signingConfigs.release
        }
        release {
            signingConfig signingConfigs.release
            ...
        }
    }
}
```

#### **重新编译**

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew clean
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### 方案 3：诊断和验证当前 SHA-1

#### **创建诊断脚本**

```bash
#!/bin/bash
echo "🔍 Google Sign-In 诊断脚本"
echo ""

# 1. 检查 Debug Keystore
echo "【Debug Keystore】"
if [ -f ~/.android/debug.keystore ]; then
    keytool -list -v -keystore ~/.android/debug.keystore \
      -alias androiddebugkey -storepass android -keypass android 2>/dev/null | \
      grep -E "SHA1|SHA256"
else
    echo "❌ Debug keystore 不存在"
fi
echo ""

# 2. 检查 Release Keystore
echo "【Release Keystore】"
if [ -f app/quran_keystore ]; then
    keytool -list -v -keystore app/quran_keystore \
      -alias key0 -storepass Huwei123 -keypass Huwei123 2>/dev/null | \
      grep -E "SHA1|SHA256"
else
    echo "❌ Release keystore 不存在"
fi
echo ""

# 3. 检查 google-services.json 中的 SHA-1
echo "【Firebase 配置中的 SHA-1】"
cat app/google-services.json | grep -E "certificate_hash" | \
  sed 's/.*"certificate_hash": "\(.*\)".*/\1/' | \
  while read sha1; do
    echo "  - $sha1"
  done
echo ""

# 4. 检查 Web Client ID
echo "【Web Client ID】"
cat app/google-services.json | grep -A 1 '"client_type": 3' | \
  grep "client_id" | sed 's/.*"client_id": "\(.*\)".*/\1/'
echo ""

# 5. 检查代码中的 Web Client ID
echo "【代码中的 Web Client ID】"
grep -r "requestIdToken" app/src/main/java/ | grep -v ".class" | head -5
echo ""

echo "✅ 诊断完成"
```

---

## 🔬 详细诊断步骤

### 1. 获取当前所有 SHA-1 指纹

运行以下命令：

```bash
# Debug Keystore
echo "Debug SHA-1:"
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android 2>/dev/null | \
  grep "SHA1:" | cut -d' ' -f3

# Release Keystore  
echo "Release SHA-1:"
keytool -list -v -keystore /Users/huwei/AndroidStudioProjects/quran0/app/quran_keystore \
  -alias key0 -storepass Huwei123 -keypass Huwei123 2>/dev/null | \
  grep "SHA1:" | cut -d' ' -f3

# google-services.json 中的 SHA-1
echo "Firebase 中已注册的 SHA-1:"
cat /Users/huwei/AndroidStudioProjects/quran0/app/google-services.json | \
  grep "certificate_hash" | sed 's/.*": "//;s/".*//'
```

### 2. 对比 SHA-1

**期望结果**：
- Debug 或 Release 的 SHA-1 **必须出现**在 google-services.json 中
- 如果不匹配，登录会失败

**示例匹配**：
```
Debug SHA-1:    A1:B2:C3:D4:E5:...
Firebase SHA-1: 6dc10985e207824215ec7610200f3741eb4640ab ✅ 匹配
```

**示例不匹配**：
```
Debug SHA-1:    X1:Y2:Z3:W4:V5:...
Firebase SHA-1: 6dc10985e207824215ec7610200f3741eb4640ab ❌ 不匹配
```

---

## 📋 完整的修复检查清单

### Firebase Console 配置

- [ ] 登录 Firebase Console
- [ ] 选择项目：quran-majeed-aa3d2
- [ ] 进入 Project Settings → General
- [ ] 找到 Android app (`com.quran.quranaudio.online`)
- [ ] 检查 SHA certificate fingerprints
- [ ] 确认 Debug Keystore SHA-1 已添加
- [ ] 确认 Release Keystore SHA-1 已添加
- [ ] 下载最新的 google-services.json
- [ ] 替换项目中的文件
- [ ] 重新编译应用

### 代码配置

- [x] Web Client ID 已配置: `517834286063-52gsp24nqkb7sht7e7jn31397nhanumb.apps.googleusercontent.com` ✅
- [x] GoogleAuthManager 正确使用 Web Client ID ✅
- [x] google-services.json 存在 ✅
- [ ] google-services.json 包含当前 keystore 的 SHA-1

### build.gradle 配置

- [x] Google Services plugin 已启用 ✅
- [x] Firebase Auth 依赖已添加 ✅
- [x] Play Services Auth 依赖已添加 ✅

---

## 🚀 快速修复步骤

### 立即尝试的方法

#### **方法 1：获取并添加 Debug SHA-1**

```bash
# 1. 创建诊断脚本
cat > /Users/huwei/AndroidStudioProjects/quran0/diagnose_sha1.sh << 'SCRIPT'
#!/bin/bash
echo "🔍 获取 Debug Keystore SHA-1"
echo ""

# 使用 Android Studio 默认的 debug keystore
KEYSTORE_PATH="$HOME/.android/debug.keystore"

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "❌ Debug keystore 不存在于: $KEYSTORE_PATH"
    echo "💡 请先运行一次 Android Studio 的 debug 构建"
    exit 1
fi

echo "📂 Keystore 路径: $KEYSTORE_PATH"
echo ""

# 获取 SHA-1 (移除冒号，转小写)
SHA1=$(keytool -list -v -keystore "$KEYSTORE_PATH" \
  -alias androiddebugkey -storepass android -keypass android 2>/dev/null | \
  grep "SHA1:" | cut -d' ' -f3 | tr -d ':' | tr '[:upper:]' '[:lower:]')

if [ -z "$SHA1" ]; then
    echo "❌ 无法获取 SHA-1"
    exit 1
fi

echo "✅ Debug Keystore SHA-1:"
echo ""
echo "   $SHA1"
echo ""
echo "📋 请按以下步骤操作："
echo ""
echo "1. 登录 Firebase Console:"
echo "   https://console.firebase.google.com/project/quran-majeed-aa3d2"
echo ""
echo "2. 进入: Project Settings → General"
echo ""
echo "3. 找到 Android app: com.quran.quranaudio.online"
echo ""
echo "4. 点击 'Add fingerprint'"
echo ""
echo "5. 粘贴以下 SHA-1:"
echo "   $SHA1"
echo ""
echo "6. 点击 'Save'"
echo ""
echo "7. 下载新的 google-services.json"
echo ""
echo "8. 替换项目文件并重新编译"
echo ""
SCRIPT

chmod +x /Users/huwei/AndroidStudioProjects/quran0/diagnose_sha1.sh
/Users/huwei/AndroidStudioProjects/quran0/diagnose_sha1.sh
```

#### **方法 2：使用 Release 签名（临时方案）**

修改 `app/build.gradle`：

```gradle
buildTypes {
    debug {
        // 临时使用 release 签名以解决登录问题
        signingConfig signingConfigs.release
    }
    release {
        signingConfig signingConfigs.release
        minifyEnabled false
        ...
    }
}
```

然后重新编译：
```bash
./gradlew clean
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📊 常见错误码说明

| 错误码 | 错误名称 | 原因 | 解决方法 |
|-------|---------|------|---------|
| **12501** | SIGN_IN_CANCELLED | SHA-1 不匹配或配置错误 | 添加正确的 SHA-1 到 Firebase |
| 12500 | SIGN_IN_FAILED | Google Services 配置问题 | 检查 google-services.json |
| 7 | NETWORK_ERROR | 网络连接问题 | 检查网络连接 |
| 10 | DEVELOPER_ERROR | Web Client ID 错误 | 检查 requestIdToken 参数 |

---

## 🔧 详细的 SHA-1 管理指南

### 获取所有 Keystore 的 SHA-1

#### **Debug Keystore**
```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android
```

**默认位置**：
- macOS: `~/.android/debug.keystore`
- Windows: `C:\Users\<用户名>\.android\debug.keystore`
- Linux: `~/.android/debug.keystore`

#### **Release Keystore**
```bash
keytool -list -v -keystore /Users/huwei/AndroidStudioProjects/quran0/app/quran_keystore \
  -alias key0 -storepass Huwei123 -keypass Huwei123
```

---

### Firebase Console 配置步骤（图文）

#### **1. 进入 Firebase 项目设置**
```
https://console.firebase.google.com/
→ 选择项目：quran-majeed-aa3d2
→ 齿轮图标 ⚙️ → Project settings
```

#### **2. 找到 Android 应用**
```
General 标签 → Your apps → Android app
应用包名：com.quran.quranaudio.online
```

#### **3. 添加 SHA 证书指纹**
```
SHA certificate fingerprints 部分
→ 点击 "Add fingerprint"
→ 粘贴 SHA-1（小写，无冒号）
   示例：6dc10985e207824215ec7610200f3741eb4640ab
→ 点击 "Save"
```

#### **4. 重复添加所有 Keystore**
- Debug Keystore SHA-1
- Release Keystore SHA-1
- 任何其他测试设备的 SHA-1

#### **5. 下载更新后的配置**
```
→ 点击 "Download google-services.json"
→ 替换：app/google-services.json
→ 重新编译
```

---

## 🔍 验证修复

### 1. 检查 google-services.json

运行以下命令验证 SHA-1 已添加：

```bash
cat app/google-services.json | python3 << 'EOF'
import json, sys

data = json.load(sys.stdin)
oauth_clients = data['client'][0]['oauth_client']

print("📋 Firebase 中已配置的 OAuth 客户端：\n")

for idx, client in enumerate(oauth_clients, 1):
    client_type = client.get('client_type')
    client_id = client.get('client_id', 'N/A')
    
    if client_type == 1:  # Android 客户端
        cert_hash = client.get('android_info', {}).get('certificate_hash', 'N/A')
        print(f"{idx}. Android Client")
        print(f"   Client ID: {client_id}")
        print(f"   SHA-1: {cert_hash}")
    elif client_type == 3:  # Web 客户端
        print(f"{idx}. Web Client (用于 requestIdToken)")
        print(f"   Client ID: {client_id}")
    print()

print("✅ 检查完成")
EOF
```

### 2. 测试登录

```bash
# 1. 清除应用数据
adb shell pm clear com.quran.quranaudio.online

# 2. 启动应用
adb shell am start -n com.quran.quranaudio.online/.prayertimes.ui.MainActivity

# 3. 查看实时日志
adb logcat | grep -E "(GoogleAuthManager|GoogleSignIn|FirebaseAuth|SIGN_IN)"
```

### 3. 期望的日志输出

**成功登录**：
```
GoogleAuthManager: getSignInIntent() called
GoogleAuthManager: handleSignInResult() called
GoogleAuthManager: GoogleSignInAccount retrieved successfully
GoogleAuthManager:   - Display Name: John Doe
GoogleAuthManager:   - Email: john@gmail.com
GoogleAuthManager:   - ID Token: Present
GoogleAuthManager: firebaseAuthWithGoogle: ABC123...
GoogleAuthManager: signInWithCredential:success
✅ Login successful!
```

**失败登录（SHA-1 不匹配）**：
```
GoogleAuthManager: getSignInIntent() called
GoogleAuthManager: handleSignInResult() called
GoogleAuthManager: Google Sign-In failed with ApiException
GoogleAuthManager:   - Status Code: 12501
GoogleAuthManager:   - Status Message: SIGN_IN_CANCELLED
❌ Sign-in was canceled
```

---

## 💡 常见问题 FAQ

### Q1: 为什么选择邮箱后立即失败？

**A**: 这是因为 Google 验证了应用的签名（SHA-1），发现与 Firebase 中注册的不匹配，因此拒绝了登录请求。

### Q2: 为什么有时候能登录，有时候不能？

**A**: 可能是：
- 使用了不同的 keystore（debug vs release）
- 只有其中一个 SHA-1 在 Firebase 中注册
- Firebase 配置缓存问题

### Q3: 添加 SHA-1 后需要多久生效？

**A**: 通常 5-10 分钟，最多 1 小时。可以尝试：
- 清除应用数据
- 重启设备
- 等待更长时间

### Q4: 如何确认 SHA-1 已生效？

**A**: 
1. 下载最新的 google-services.json
2. 检查文件中包含新的 certificate_hash
3. 测试登录，查看日志

---

## 🎯 推荐的修复步骤（按优先级）

### 🥇 优先级 1：添加 Debug SHA-1（最简单）

```bash
# 1. 获取 SHA-1
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android | \
  grep "SHA1:" | cut -d' ' -f3

# 2. 转换为小写无冒号格式
# 示例：A1:B2:C3:D4:... → a1b2c3d4...

# 3. 在 Firebase Console 添加
# 4. 下载新的 google-services.json
# 5. 替换并重新编译
```

### 🥈 优先级 2：让 Debug 使用 Release 签名

```gradle
// app/build.gradle
buildTypes {
    debug {
        signingConfig signingConfigs.release  // 添加这行
    }
}
```

### 🥉 优先级 3：使用 Release 版本测试

```bash
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📞 如果仍然失败

### 收集详细日志

```bash
# 清除日志
adb logcat -c

# 开始登录操作

# 保存完整日志
adb logcat -d > google_signin_debug.log

# 查找关键信息
cat google_signin_debug.log | grep -E "(GoogleAuthManager|ApiException|StatusCode|SHA|certificate)"
```

### 检查 Firebase 项目配置

1. **验证包名匹配**：
   ```
   Firebase: com.quran.quranaudio.online
   代码: com.quran.quranaudio.online
   ✅ 匹配
   ```

2. **验证 Web Client ID**：
   ```
   代码中: 517834286063-52gsp24nqkb7sht7e7jn31397nhanumb.apps.googleusercontent.com
   Firebase: 517834286063-52gsp24nqkb7sht7e7jn31397nhanumb.apps.googleusercontent.com
   ✅ 匹配
   ```

3. **验证 google-services.json 是最新的**：
   - 检查文件修改日期
   - 重新下载确保最新

---

## 📚 相关文档

- `README_GOOGLE_LOGIN.md` - Google 登录配置指南
- `GOOGLE_SIGN_IN_DEBUG_SHA1_FIX.md` - SHA-1 修复指南
- `Google登录优化说明.md` - 优化说明

---

## ✅ 总结

**最可能的原因**：
1. **Debug Keystore 的 SHA-1 未在 Firebase 中注册** ⭐⭐⭐⭐⭐
2. google-services.json 文件过期
3. Firebase 配置缓存问题

**推荐修复方法**：
1. **获取 Debug Keystore SHA-1**
2. **在 Firebase Console 添加**
3. **下载新的 google-services.json**
4. **替换并重新编译**
5. **等待 5-10 分钟生效**

**临时方案**：
让 Debug 版本使用 Release 签名，Release SHA-1 已经在 Firebase 中注册。

---

**🎯 请按照上述步骤添加 Debug SHA-1 到 Firebase Console，这应该能解决 "Sign-in canceled" 的问题！**

