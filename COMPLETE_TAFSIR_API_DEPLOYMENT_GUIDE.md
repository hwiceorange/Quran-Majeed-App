# 🚀 完整 Tafsir API 部署指南

## 📋 总览

本指南提供**完整的多语言 Tafsir API 解决方案**，支持从自定义服务器和 Quran.com 加载不同语言的古兰经注释。

---

## ✅ 已完成的应用端修改

### 1. 恢复印尼语 Tafsir 到清单
- ✅ `app/src/main/assets/tafsir/available_tafsirs_info.json`
- ✅ 包含英语、印尼语、阿拉伯语

### 2. 创建自定义 Tafsir API 接口
- ✅ `app/src/main/java/.../api/CustomTafsirApi.kt`
- ✅ 定义 `getTafsir(slug, ayahKey)` 方法

### 3. 修改 RetrofitInstance
- ✅ `app/src/main/java/.../api/RetrofitInstance.kt`
- ✅ 添加 `customTafsir` 实例
- ✅ Base URL: `https://apis.dochubai.com/quran/api/`

### 4. 修改 ActivityTafsir 支持多 API 源
- ✅ `app/.../activities/ActivityTafsir.kt`
- ✅ 印尼语 Tafsir (`id-*`) → 自定义服务器
- ✅ 其他 Tafsir → Quran.com

---

## 🔧 服务器端部署步骤

### 步骤 1: 上传 PHP API 文件

1. **将 `server_api_tafsir.php` 上传到服务器**
   ```
   服务器路径: /public_html/quran/api/v4/tafsirs/index.php
   ```

2. **配置 .htaccess (URL 重写)**
   
   在 `/public_html/quran/api/v4/tafsirs/` 目录创建 `.htaccess`：
   
   ```apache
   RewriteEngine On
   
   # 处理 Tafsir API 请求
   # /v4/tafsirs/{slug}/by_ayah/{ayahKey} → index.php
   RewriteCond %{REQUEST_FILENAME} !-f
   RewriteCond %{REQUEST_FILENAME} !-d
   RewriteRule ^(.+)/by_ayah/(.+)$ index.php [L,QSA]
   ```

### 步骤 2: 配置数据库连接

编辑 `server_api_tafsir.php` 的数据库配置：

```php
// ===================================
// 配置数据库连接
// ===================================
$DB_HOST = 'localhost';           // 您的数据库主机
$DB_NAME = 'quran_database';      // 您的数据库名称
$DB_USER = 'your_db_user';        // 您的数据库用户名
$DB_PASS = 'your_db_password';    // 您的数据库密码
$DB_TABLE = 'tafsir_indonesian';  // 印尼语 Tafsir 表名
```

### 步骤 3: 验证数据库表结构

确保 `tafsir_indonesian` 表有以下字段：

```sql
CREATE TABLE IF NOT EXISTS `tafsir_indonesian` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `surah_id` int(11) NOT NULL,
  `ayat_id` int(11) NOT NULL,
  `text` text NOT NULL,
  `language` varchar(10) NOT NULL DEFAULT 'id',
  PRIMARY KEY (`id`),
  KEY `idx_verse` (`surah_id`, `ayat_id`, `language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 步骤 4: 上传 Tafsir 清单文件

```bash
# 上传清单文件到服务器
上传 server_deploy/available_tafsirs_info.json
到: https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

---

## 🧪 测试 API

### 测试 1: 验证 API 端点

```bash
# 测试印尼语 Tafsir API
curl https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

**预期响应：**
```json
{
  "tafsir": {
    "resource_id": 999,
    "text": "...印尼语注释内容...",
    "verse_key": "1:1"
  }
}
```

### 测试 2: 测试不同经文

```bash
# 测试第 2 章第 255 节（Ayat Al-Kursi）
curl https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/2:255
```

### 测试 3: 测试错误处理

```bash
# 测试无效的 Tafsir slug
curl https://apis.dochubai.com/quran/api/v4/tafsirs/invalid-slug/by_ayah/1:1
# 预期: HTTP 404

# 测试无效的 ayah key
curl https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/999:999
# 预期: HTTP 404
```

---

## 📱 应用端测试

### 测试流程

1. **编译应用**
   ```bash
   cd /Users/huwei/AndroidStudioProjects/quran0
   ./gradlew clean :app:assembleDebug
   ```

2. **安装到设备**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **清除数据并重启**
   ```bash
   adb shell pm clear com.quran.quranaudio.online
   adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
   ```

4. **监控日志**
   ```bash
   adb logcat | grep -E "ActivityTafsir|TafsirManager|API_REQUEST"
   ```

### 预期日志

#### 成功加载印尼语 Tafsir：
```
D TafsirManager: ✅ Parsed 3 language groups
D TafsirManager:    - id: 1 tafsirs
D MainActivity: ✅ Auto-selected Tafsir: id-tafsir-kemenag for language: id
D ActivityTafsir: 📥 Loading Indonesian Tafsir from custom server: id-tafsir-kemenag
D API_REQUEST: 🌐 GET https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1
D API_RESPONSE: ✅ 200 https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1
D ActivityTafsir: ✅ Tafsir loaded and cached successfully
```

#### 成功加载英语 Tafsir（从 Quran.com）：
```
D MainActivity: ✅ Auto-selected Tafsir: en-tafisr-ibn-kathir for language: en
D ActivityTafsir: 📥 Loading Tafsir from Quran.com: en-tafisr-ibn-kathir
D API_RESPONSE: ✅ 200 https://api.quran.com/api/v4/tafsirs/...
D ActivityTafsir: ✅ Tafsir loaded and cached successfully
```

---

## 🐛 故障排查

### 问题 1: HTTP 404 - API 端点未找到

**可能原因：**
- `.htaccess` 未配置
- PHP 文件路径错误
- Apache `mod_rewrite` 未启用

**解决方法：**
```bash
# 检查 .htaccess 是否生效
curl -I https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1

# 检查 Apache 模块
a2enmod rewrite
service apache2 restart
```

### 问题 2: HTTP 500 - 数据库错误

**可能原因：**
- 数据库连接配置错误
- 表名或字段名不匹配
- 数据库权限不足

**解决方法：**
1. 检查数据库配置（主机、用户名、密码）
2. 验证表结构：
   ```sql
   SHOW COLUMNS FROM tafsir_indonesian;
   ```
3. 检查 PHP 错误日志：
   ```bash
   tail -f /var/log/apache2/error.log
   ```

### 问题 3: 应用崩溃 - JSON 解析错误

**可能原因：**
- API 响应格式不兼容
- 字段名不匹配

**解决方法：**
1. 检查 API 响应格式：
   ```bash
   curl https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1 | jq
   ```
2. 确保响应包含 `tafsir.text` 字段
3. 查看应用日志：
   ```bash
   adb logcat | grep -E "JSON|TafsirModel"
   ```

### 问题 4: 无网络连接

**解决方法：**
1. 检查设备网络连接
2. 验证防火墙设置
3. 尝试使用浏览器访问 API

---

## 📊 API 响应格式规范

### 成功响应（HTTP 200）

```json
{
  "tafsir": {
    "resource_id": 999,
    "text": "印尼语注释内容...",
    "verse_key": "1:1"
  }
}
```

### 错误响应（HTTP 404）

```json
{
  "error": "Tafsir not found",
  "message": "详细错误信息"
}
```

### 错误响应（HTTP 500）

```json
{
  "error": "Database error",
  "message": "详细错误信息"
}
```

---

## 🔐 安全建议

1. **API 速率限制**
   - 添加 IP 限流，防止滥用

2. **数据库安全**
   - 使用最小权限数据库用户
   - 定期备份数据

3. **HTTPS**
   - 确保使用 HTTPS（已配置 `apis.dochubai.com`）

4. **错误日志**
   - 不在生产环境暴露详细错误信息
   - 记录到日志文件

---

## 📝 文件清单

### 应用端文件（已修改）
- ✅ `app/src/main/assets/tafsir/available_tafsirs_info.json`
- ✅ `app/.../api/CustomTafsirApi.kt`
- ✅ `app/.../api/RetrofitInstance.kt`
- ✅ `app/.../activities/ActivityTafsir.kt`

### 服务器端文件（待部署）
- ⚠️ `server_api_tafsir.php` → `/public_html/quran/api/v4/tafsirs/index.php`
- ⚠️ `.htaccess` → `/public_html/quran/api/v4/tafsirs/.htaccess`
- ⚠️ `available_tafsirs_info.json` → CDN

### 文档文件
- ✅ `COMPLETE_TAFSIR_API_DEPLOYMENT_GUIDE.md`
- ✅ `TAFSIR_INDONESIAN_CONTENT_API.md`

---

## 🎯 下一步操作

### 立即执行（必须）

1. **上传服务器文件**
   ```bash
   # 上传 server_api_tafsir.php
   # 创建 .htaccess
   # 配置数据库连接
   ```

2. **测试 API**
   ```bash
   curl https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1
   ```

3. **编译应用**
   ```bash
   ./gradlew clean :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. **测试应用**
   - 设置语言为印尼语
   - 点击任意经文注释
   - 验证是否成功加载

### 后续优化（可选）

1. **添加更多语言 Tafsir**
   - 修改 `server_api_tafsir.php` 的 `$supportedTafsirs` 数组
   - 更新清单文件

2. **实施缓存机制**
   - 服务器端：Redis/Memcached
   - 应用端：已实现本地文件缓存

3. **监控和日志**
   - 添加 API 访问日志
   - 监控响应时间

---

## ✅ 完成标志

**服务器端：**
- [ ] PHP API 文件已上传
- [ ] `.htaccess` 已配置
- [ ] 数据库连接已测试
- [ ] API 端点可访问（HTTP 200）

**应用端：**
- [x] 代码已修改
- [ ] APK 已编译
- [ ] 应用已测试
- [ ] 印尼语 Tafsir 正常加载

---

**准备好部署了吗？按照上述步骤操作，或者告诉我您需要哪方面的帮助！** 🚀

