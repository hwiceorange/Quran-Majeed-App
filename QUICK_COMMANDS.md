# ⚡ 快速命令参考

## 📱 应用端命令

### 监控 Tafsir 日志（正确的单行命令）

```bash
cd /Users/huwei/AndroidStudioProjects/quran0

# 方法 1: 使用脚本（推荐）
./monitor_tafsir_logs.sh

# 方法 2: 直接命令
adb logcat | grep -E "TafsirManager|ActivityTafsir|API_REQUEST|API_RESPONSE"
```

---

### 编译和安装

```bash
# Clean 构建
./gradlew clean :app:assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 清除数据并启动
adb shell pm clear com.quran.quranaudio.online
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

---

## 🌐 服务器端命令

### 测试 API 端点

```bash
# 测试清单 API
curl https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json

# 测试印尼语 Tafsir 内容 API（第 1 章第 1 节）
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1

# 测试 Ayat Al-Kursi（第 2 章第 255 节）
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/2:255
```

---

### 查看响应头（调试）

```bash
# 查看 HTTP 响应头
curl -I https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1

# 查看完整响应（格式化 JSON）
curl -s https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1 | jq
```

---

## 📂 文件位置

### 本地文件

```
/Users/huwei/AndroidStudioProjects/quran0/
├── server_api_tafsir.php           ← 上传到服务器
├── server_deploy/
│   ├── .htaccess                   ← 上传到服务器
│   └── available_tafsirs_info.json
└── monitor_tafsir_logs.sh          ← 日志监控脚本
```

### 服务器文件

```
/public_html/quran/apis/tafsirs/
├── index.php      ← server_api_tafsir.php 重命名
└── .htaccess      ← server_deploy/.htaccess 复制
```

---

## 🔍 完整的 URL 路径

### API 端点

```
https://apis.dochubai.com/quran/apis/tafsirs/{slug}/by_ayah/{ayahKey}
```

### 示例

```
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
                                     ^^^^^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^
                                            slug                ayahKey
```

---

## 📋 部署清单

### 服务器端

- [ ] 上传 `server_api_tafsir.php` → `/public_html/quran/apis/tafsirs/index.php`
- [ ] 上传 `server_deploy/.htaccess` → `/public_html/quran/apis/tafsirs/.htaccess`
- [ ] 编辑 `index.php` 配置数据库连接
- [ ] 测试 API：`curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1`

### 应用端

- [x] 代码已修改（Base URL: `https://apis.dochubai.com/quran/apis/tafsirs/`）
- [ ] 编译 APK
- [ ] 安装到设备
- [ ] 测试印尼语 Tafsir

---

## 🎯 快速测试流程

### 步骤 1: 测试服务器 API

```bash
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

**预期结果：**
- HTTP 200 → ✅ 服务器已配置
- HTTP 404 → ⚠️  服务器未配置

---

### 步骤 2: 编译应用

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew clean :app:assembleDebug
```

---

### 步骤 3: 安装并启动

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm clear com.quran.quranaudio.online
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

---

### 步骤 4: 监控日志

```bash
./monitor_tafsir_logs.sh
```

**或者在新终端：**

```bash
adb logcat | grep -E "TafsirManager|ActivityTafsir|API_REQUEST|API_RESPONSE"
```

---

## 🐛 常见错误

### grep: empty (sub)expression

**原因：** 命令被换行导致语法错误

**解决方法：**
```bash
# ❌ 错误（换行）
adb logcat | grep -E "TafsirManager|ActivityTafsir|API_REQUEST|
API_RESPONSE"

# ✅ 正确（单行）
adb logcat | grep -E "TafsirManager|ActivityTafsir|API_REQUEST|API_RESPONSE"

# ✅ 或使用脚本
./monitor_tafsir_logs.sh
```

---

### HTTP 404 - API 端点未找到

**原因：** `.htaccess` 未生效

**解决方法：**
1. 确认 `.htaccess` 文件存在
2. 检查 Apache 配置：`AllowOverride All`
3. 启用 mod_rewrite：`a2enmod rewrite && service apache2 restart`

---

### HTTP 500 - 服务器错误

**原因：** 数据库连接失败

**解决方法：**
1. 检查 `index.php` 中的数据库配置
2. 查看错误日志：`tail -f /var/log/apache2/error.log`

---

## 📞 需要帮助？

**查看完整文档：**
- `SERVER_DEPLOYMENT_COMPLETE.md` - 完整部署指南
- `READY_TO_COMPILE.md` - 编译测试指南
- `COMPLETE_TAFSIR_API_DEPLOYMENT_GUIDE.md` - 详细技术文档

---

**现在开始部署吧！** 🚀

