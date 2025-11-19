# 🧪 服务器 Tafsir API 最终测试指南

## ✅ 文件已上传确认

```
/public_html/quran/apis/tafsirs/
├── .htaccess                        ✅ 已上传
├── available_tafsirs_info.json      ✅ 已上传 (828 B)
└── index.php                        ✅ 已上传 (5.53 KiB)
```

---

## 🔧 配置数据库连接

### 步骤 1: 获取数据库信息

1. **登录 Hostinger 控制面板**
   - URL: https://hpanel.hostinger.com

2. **点击左侧菜单 "数据库" (Databases)**

3. **查看数据库信息**，您会看到类似：
   ```
   数据库名称: u123456789_qurandb
   数据库用户: u123456789_user
   数据库主机: localhost
   端口: 3306
   ```

---

### 步骤 2: 编辑 index.php

**在 Hostinger 文件管理器中：**

1. 导航到 `/quran/apis/tafsirs/`
2. 右键点击 `index.php`
3. 选择 **"编辑"** 或 **"Edit"**
4. 找到 **第 36-40 行**
5. 修改为您的数据库信息：

```php
// ===================================
// 配置数据库连接
// ===================================
$DB_HOST = 'localhost';                    // 从控制面板复制
$DB_NAME = 'u123456789_qurandb';          // 从控制面板复制（示例）
$DB_USER = 'u123456789_user';             // 从控制面板复制（示例）
$DB_PASS = 'your_actual_password';        // 您设置的密码
$DB_TABLE = 'tafsir_indonesian';          // 保持不变
```

6. **保存文件**

---

## 🧪 测试 API

### 测试 1: 使用浏览器

**直接在浏览器中打开：**
```
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

**预期结果：**

#### ✅ 成功（HTTP 200）
```json
{
  "tafsir": {
    "resource_id": 999,
    "text": "印尼语注释内容...",
    "verse_key": "1:1"
  }
}
```

#### ⚠️ 数据库连接错误（HTTP 500）
```json
{
  "error": "Database error",
  "message": "Failed to query tafsir: ..."
}
```
**解决方法：** 检查数据库配置是否正确

#### ⚠️ 数据不存在（HTTP 404）
```json
{
  "error": "Tafsir not found for this ayah",
  "message": "No tafsir found for 1:1 in Tafsir Al-Qur'an Kemenag"
}
```
**解决方法：** 确认数据库表 `tafsir_indonesian` 中有数据

#### ❌ URL 重写失败（HTML 404 页面）
```html
<!DOCTYPE html>
<html>
<head><title>404 Not Found</title></head>
...
```
**解决方法：** 检查 `.htaccess` 文件是否正确

---

### 测试 2: 使用命令行（可选）

```bash
# 测试 API
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1

# 查看响应头
curl -I https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

---

### 测试 3: 测试不同的经文

```bash
# 第 1 章第 1 节
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1

# 第 2 章第 255 节 (Ayat Al-Kursi)
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/2:255

# 第 3 章第 2 节（应用正在请求的）
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/3:2
```

---

## 📱 应用端测试

### 步骤 1: 启动日志监控

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
./monitor_tafsir_logs.sh
```

**或者：**

```bash
adb logcat | grep -E "TafsirManager|ActivityTafsir|API_REQUEST|API_RESPONSE"
```

---

### 步骤 2: 在应用中测试

1. **打开应用**
2. **设置语言为印尼语**
3. **打开任意经文**
4. **点击注释图标**

---

### 步骤 3: 查看日志

**✅ 成功的日志：**
```
D TafsirManager: ✅ Parsed 3 language groups
D TafsirManager:    - id: 1 tafsirs
D ActivityTafsir: 📥 Loading Indonesian Tafsir from custom server: id-tafsir-kemenag
D QuranAppLogs: 🌐 API_REQUEST: GET https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/3:2
D QuranAppLogs: ✅ API_RESPONSE: 200
D ActivityTafsir: ✅ Tafsir loaded and cached successfully
```

**❌ 失败的日志（需要修复）：**
```
D ActivityTafsir: 📥 Loading Indonesian Tafsir from custom server: id-tafsir-kemenag
D QuranAppLogs: 🌐 API_REQUEST: GET https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/3:2
D QuranAppLogs: ✅ API_RESPONSE: 404  ← 或 500
E ActivityTafsir: ❌ Failed to load tafsir from custom server
```

---

## 🐛 故障排查

### 问题 1: HTTP 500 - 数据库连接失败

**症状：**
```json
{
  "error": "Database error",
  "message": "Access denied for user..."
}
```

**解决方法：**
1. 确认数据库配置信息正确
2. 在 Hostinger 控制面板中重置数据库密码
3. 更新 `index.php` 中的密码

---

### 问题 2: HTTP 404 - 数据不存在

**症状：**
```json
{
  "error": "Tafsir not found for this ayah"
}
```

**解决方法：**
1. 登录 phpMyAdmin
2. 检查表 `tafsir_indonesian` 是否存在
3. 检查表中是否有数据：
   ```sql
   SELECT * FROM tafsir_indonesian LIMIT 10;
   ```
4. 如果没有数据，需要导入印尼语 Tafsir 数据

---

### 问题 3: HTTP 404 HTML - URL 重写失败

**症状：** 返回 HTML 404 页面而不是 JSON

**解决方法：**

1. **检查 .htaccess 文件内容：**
   ```apache
   RewriteEngine On
   RewriteCond %{REQUEST_FILENAME} !-f
   RewriteCond %{REQUEST_FILENAME} !-d
   RewriteRule ^([^/]+)/by_ayah/([^/]+)$ index.php [L,QSA]
   ```

2. **确认 Apache mod_rewrite 已启用**
   - 通常 Hostinger 默认启用
   - 如果不确定，联系 Hostinger 支持

3. **测试 .htaccess 是否生效：**
   在 `.htaccess` 中添加一行测试：
   ```apache
   # 测试行（会导致 500 错误）
   INVALID_DIRECTIVE
   ```
   如果访问时出现 500 错误，说明 `.htaccess` 生效
   然后删除测试行

---

### 问题 4: 数据库表不存在

**解决方法：**

1. **登录 phpMyAdmin**（从 Hostinger 控制面板）

2. **创建表：**
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

3. **导入数据**（如果有 SQL 文件）

---

## 📊 验证清单

### 服务器端

- [ ] 文件已上传到正确位置
- [ ] `index.php` 数据库配置已更新
- [ ] `.htaccess` 文件存在且内容正确
- [ ] 数据库表 `tafsir_indonesian` 存在
- [ ] 表中有数据
- [ ] API 返回 HTTP 200

### 应用端

- [ ] 应用已安装最新版本
- [ ] 印尼语 Tafsir 可以正常显示
- [ ] 日志显示 HTTP 200 响应
- [ ] 注释内容正确加载

---

## 🎯 快速测试命令

```bash
# 1. 在浏览器中打开
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1

# 2. 启动应用日志监控
cd /Users/huwei/AndroidStudioProjects/quran0
./monitor_tafsir_logs.sh

# 3. 在应用中测试印尼语 Tafsir
```

---

## 📞 需要帮助？

**如果遇到问题，请提供：**

1. **浏览器测试结果** - 访问 API URL 的响应
2. **应用日志** - 包含 API_RESPONSE 的日志
3. **数据库状态** - 表是否存在，是否有数据
4. **错误消息** - 完整的错误信息

---

**现在请：**
1. ✅ 配置数据库连接（编辑 `index.php`）
2. ✅ 在浏览器中测试 API
3. ✅ 在应用中测试

祝您成功！🚀

