# Google 登录修复 - 文档索引

## 📋 问题

**症状**: 手动安装正式包 APK 后，Google 登录不成功

**根本原因**: Release Keystore 的 SHA-1 指纹 (`19184387c863b6ac668633c7917d34c89ddf54f5`) 未在 Firebase Console 中注册

**影响**: 所有使用 Release APK 的用户无法登录

---

## 🚀 快速开始

### 1. 快速修复（推荐新手）

查看：**`QUICK_FIX_GOOGLE_LOGIN.md`**

- ⚡ 5 分钟快速修复
- 📋 5 步操作指南
- 🎯 关键信息速查

### 2. 诊断问题

运行脚本：

```bash
./get_release_sha1.sh
```

查看：
- Release Keystore 的 SHA-1 指纹
- Firebase 中已注册的 SHA-1
- 是否匹配

### 3. 自动化测试

运行脚本：

```bash
./test_google_login_release.sh
```

自动完成：
- ✅ 检查 SHA-1 配置
- ✅ 检查设备连接
- ✅ 编译 Release APK
- ✅ 安装到设备
- ✅ 监控登录日志

---

## 📚 文档列表

### 核心文档

1. **`QUICK_FIX_GOOGLE_LOGIN.md`** ⭐ 推荐
   - 5 分钟快速修复指南
   - 适合：需要快速解决问题

2. **`GOOGLE_LOGIN_ISSUE_SUMMARY.md`**
   - 完整的问题总结和解决方案
   - 适合：了解问题全貌

3. **`GOOGLE_LOGIN_RELEASE_FIX_GUIDE.md`**
   - 详细的修复指南
   - 包含故障排查和 FAQ
   - 适合：深入了解和排查问题

### 工具脚本

4. **`get_release_sha1.sh`**
   - SHA-1 诊断脚本
   - 自动获取和对比 SHA-1 指纹

5. **`test_google_login_release.sh`**
   - 自动化测试脚本
   - 完整的测试流程

### 历史文档

6. **`GOOGLE_SIGN_IN_TROUBLESHOOTING_COMPLETE.md`**
   - 之前的故障排查指南
   - 包含通用的 Google 登录问题

---

## 🎯 根据场景选择文档

### 场景 1: 我只想快速修复

👉 查看：**`QUICK_FIX_GOOGLE_LOGIN.md`**

5 步操作，10 分钟搞定。

### 场景 2: 我想了解问题原因

👉 查看：**`GOOGLE_LOGIN_ISSUE_SUMMARY.md`**

包含完整的问题诊断和技术细节。

### 场景 3: 修复后还是失败

👉 查看：**`GOOGLE_LOGIN_RELEASE_FIX_GUIDE.md`**

包含详细的故障排查步骤和 FAQ。

### 场景 4: 我想自动化测试

👉 运行：**`./test_google_login_release.sh`**

自动完成所有测试步骤。

---

## 🔧 核心修复步骤

### 1. 添加 SHA-1 到 Firebase

```
SHA-1: 19184387c863b6ac668633c7917d34c89ddf54f5
```

1. 访问：https://console.firebase.google.com/project/quran-majeed-aa3d2
2. Project Settings → General
3. 找到 Android app: com.quran.quranaudio.online
4. 点击 "Add fingerprint"
5. 粘贴 SHA-1
6. 点击 "Save"

### 2. 下载新配置

1. 点击 "Download google-services.json"
2. 替换：`app/google-services.json`

### 3. 重新编译

```bash
./gradlew clean
./gradlew :app:assembleRelease
```

### 4. 等待生效

⏰ 5-10 分钟（最多 1 小时）

### 5. 测试

```bash
./test_google_login_release.sh
```

---

## ✅ 验证修复

### 方法 1: 运行诊断脚本

```bash
./get_release_sha1.sh
```

**预期输出**：
```
✅ 当前 Release Keystore 的 SHA-1 已在 Firebase 中注册！
```

### 方法 2: 查看日志

```bash
adb logcat | grep GoogleAuthManager
```

**成功登录**：
```
GoogleAuthManager: signInWithCredential:success
```

### 方法 3: 手动测试

1. 安装 Release APK
2. 尝试 Google 登录
3. ✅ 验证成功

---

## 📊 文档结构

```
Google 登录修复文档
│
├── README_GOOGLE_LOGIN_FIX.md (本文档)
│   └── 文档索引和快速导航
│
├── QUICK_FIX_GOOGLE_LOGIN.md ⭐
│   └── 5 分钟快速修复指南
│
├── GOOGLE_LOGIN_ISSUE_SUMMARY.md
│   └── 完整的问题总结
│
├── GOOGLE_LOGIN_RELEASE_FIX_GUIDE.md
│   └── 详细的修复指南和故障排查
│
├── get_release_sha1.sh
│   └── SHA-1 诊断脚本
│
└── test_google_login_release.sh
    └── 自动化测试脚本
```

---

## 🔑 关键信息

| 项目 | 值 |
|-----|-----|
| **问题** | Release APK Google 登录失败 |
| **原因** | SHA-1 未注册 |
| **SHA-1** | `19184387c863b6ac668633c7917d34c89ddf54f5` |
| **Firebase 项目** | quran-majeed-aa3d2 |
| **包名** | com.quran.quranaudio.online |
| **修复时间** | < 5 分钟（操作） + 5-10 分钟（生效） |
| **优先级** | P0 (Critical) |

---

## 💡 提示

1. **从快速修复开始**：如果是第一次遇到这个问题，先看 `QUICK_FIX_GOOGLE_LOGIN.md`
2. **使用自动化工具**：脚本可以帮你节省时间和避免错误
3. **耐心等待**：Firebase 配置需要时间生效
4. **保存文档**：这些文档对未来的问题排查很有帮助

---

## 📞 获取帮助

如果按照文档操作后仍然失败：

1. **运行诊断**：
   ```bash
   ./get_release_sha1.sh
   ```

2. **收集日志**：
   ```bash
   adb logcat -d > google_login_debug.log
   ```

3. **查看详细指南**：
   - `GOOGLE_LOGIN_RELEASE_FIX_GUIDE.md` 的故障排查部分
   - FAQ 部分

---

**创建日期**: 2025-12-23  
**最后更新**: 2025-12-23  
**版本**: 1.0  
**状态**: ⚠️ 待 Firebase 配置更新


