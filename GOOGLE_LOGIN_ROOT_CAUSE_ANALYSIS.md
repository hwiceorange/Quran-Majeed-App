# 🔍 Google 登录失败 - 根本原因分析

## ❌ 问题确认

**错误**: Sign-in Canceled (12501)  
**根本原因**: SHA-1 指纹完全不匹配 ❌

---

## 📊 SHA-1 对比分析

### 实际使用的 Keystore SHA-1
```
Keystore: app/quran_keystore
Alias: key0
SHA-1: 19:18:43:87:C8:63:B6:AC:66:86:33:C7:91:7D:34:C8:9D:DF:54:F5
小写: 191843876863b6ac66866333c7917d34c89ddf54f5
```

### Firebase Console 中已注册的 SHA-1
```
❌ SHA-1 #1: 6dc10985e207824215ec7610200f3741eb4640ab
❌ SHA-1 #2: 8ae5e2c39e284c7c3277ed2e8957bf08ab4f9e45
```

### 🚨 结论
**实际 SHA-1 与 Firebase 中注册的 SHA-1 完全不同！**

这就是为什么 Google 登录一直失败的根本原因。

---

## ✅ 解决方案（2 选 1）

### 方案 1：在 Firebase Console 添加正确的 SHA-1（推荐）

#### 步骤 1: 登录 Firebase Console
访问: https://console.firebase.google.com/

#### 步骤 2: 选择项目
项目名称: `quran-majeed-aa3d2`

#### 步骤 3: 进入项目设置
左侧菜单 → ⚙️ 项目设置 → 您的应用 → Android

#### 步骤 4: 添加 SHA-1 指纹
点击 **"添加指纹"** 按钮

粘贴以下 SHA-1（去掉冒号）:
```
191843876863B6AC66866333C7917D34C89DDF54F5
```

#### 步骤 5: 下载新的 google-services.json
点击 **"下载 google-services.json"**

#### 步骤 6: 替换文件
```bash
# 备份旧文件
cp app/google-services.json app/google-services.json.backup

# 将新下载的文件复制到
app/google-services.json
```

#### 步骤 7: 清理并重新编译
```bash
./gradlew clean
./gradlew :app:assembleDebug
adb uninstall com.quran.quranaudio.online
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### 步骤 8: 等待生效
⏳ 等待 5-10 分钟让 Firebase 配置生效

#### 步骤 9: 测试
打开应用 → 尝试 Google 登录

---

### 方案 2：使用已注册的 Keystore（不推荐）

如果您有与这两个 SHA-1 匹配的 keystore：
- `6dc10985e207824215ec7610200f3741eb4640ab`
- `8ae5e2c39e284c7c3277ed2e8957bf08ab4f9e45`

则需要在 `build.gradle` 中修改 `signingConfigs` 使用那个 keystore。

**但我们不推荐这个方案**，因为当前的 `app/quran_keystore` 应该是正确的。

---

## 🔍 为什么之前的修复没有效果？

### 之前的假设 ❌
- 认为问题是 Debug SHA-1 未注册
- 让 Debug 使用 Release 签名

### 实际情况 ✅
- Debug 和 Release 都使用同一个 keystore
- 但这个 keystore 的 SHA-1 根本没有在 Firebase 注册
- 所以无论 Debug 还是 Release 都会失败

---

## 🎯 快速修复命令

复制粘贴以下 SHA-1 到 Firebase Console:
```
191843876863B6AC66866333C7917D34C89DDF54F5
```

或者使用带格式的版本（Firebase 会自动处理）:
```
19:18:43:87:C8:63:B6:AC:66:86:33:C7:91:7D:34:C8:9D:DF:54:F5
```

---

## 📋 验证清单

- [ ] Firebase Console → 项目设置 → Android 应用
- [ ] 点击 "添加指纹"
- [ ] 粘贴 SHA-1: `191843876863B6AC66866333C7917D34C89DDF54F5`
- [ ] 保存
- [ ] 下载新的 google-services.json
- [ ] 替换 app/google-services.json
- [ ] 清理并重新编译
- [ ] 卸载并重新安装 APK
- [ ] 等待 5-10 分钟
- [ ] 测试 Google 登录

---

## ⚠️ 重要说明

### 关于现有的 2 个 SHA-1
google-services.json 中已有的两个 SHA-1:
- `6dc10985e207824215ec7610200f3741eb4640ab`
- `8ae5e2c39e284c7c3277ed2e8957bf08ab4f9e45`

**这些可能是**:
1. 旧的 keystore 指纹
2. 其他开发者的 Debug 证书
3. Google Play Console 上传密钥
4. 错误的配置

**建议**: 保留它们，添加新的 SHA-1 即可。Firebase 支持多个 SHA-1。

---

## 🎉 修复后的效果

添加正确的 SHA-1 后:
- ✅ Google 登录弹窗正常
- ✅ 选择邮箱后成功登录
- ✅ 不再显示 "Sign-in canceled"
- ✅ 用户信息正常显示

---

**📱 这是真正的根本原因，修复后 Google 登录将正常工作！**
