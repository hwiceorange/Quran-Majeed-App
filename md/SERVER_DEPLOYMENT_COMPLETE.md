# 🚀 Tafsir API 服务器部署完整指南

## 📍 服务器目录结构

```
/public_html/quran/apis/tafsirs/
├── index.php           ← server_api_tafsir.php 重命名为这个
└── .htaccess          ← URL 重写配置
```

---

## 📋 部署步骤

### 步骤 1: 上传 PHP 文件

**本地文件：** `/Users/huwei/AndroidStudioProjects/quran0/server_api_tafsir.php`

**上传到服务器：** `/public_html/quran/apis/tafsirs/index.php`

**⚠️ 重要：** 文件必须重命名为 `index.php`

---

### 步骤 2: 创建 .htaccess 文件

**服务器路径：** `/public_html/quran/apis/tafsirs/.htaccess`

**文件内容：**

```apache
# Tafsir API URL 重写规则
RewriteEngine On

# 处理 Tafsir API 请求
# 示例: /id-tafsir-kemenag/by_ayah/1:1 → index.php
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule ^([^/]+)/by_ayah/([^/]+)$ index.php [L,QSA]

# 错误处理
ErrorDocument 404 "Tafsir API endpoint not found"
```

---

### 步骤 3: 配置数据库连接

编辑服务器上的 `index.php` 文件（第 36-40 行）：

```php
// ===================================
// 配置数据库连接
// ===================================
$DB_HOST = 'localhost';           // 您的数据库主机
$DB_NAME = 'your_database_name';  // 您的数据库名称
$DB_USER = 'your_db_user';        // 您的数据库用户名
$DB_PASS = 'your_db_password';    // 您的数据库密码
$DB_TABLE = 'tafsir_indonesian';  // 印尼语 Tafsir 表名
```

---

### 步骤 4: 测试 API

```bash
# 测试 API 端点
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

**预期响应（HTTP 200）：**
```json
{
  "tafsir": {
    "resource_id": 999,
    "text": "印尼语注释内容...",
    "verse_key": "1:1"
  }
}
```

**如果返回 404：**
- 检查 `.htaccess` 是否存在
- 检查 Apache `mod_rewrite` 是否启用
- 查看错误日志：`tail -f /var/log/apache2/error.log`

**如果返回 500：**
- 检查数据库连接配置
- 检查 PHP 错误日志
- 验证表名和字段名

---

## 🔍 完整的 URL 路径

### 应用请求的 URL

```
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

### URL 分解

```
https://apis.dochubai.com   ← 域名
/quran/apis/tafsirs/        ← Base URL (对应服务器目录)
id-tafsir-kemenag           ← slug (Tafsir 类型)
/by_ayah/                   ← API 路径
1:1                         ← ayahKey (章节:经文)
```

### .htaccess 重写规则

```apache
# 输入 URL:
/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1

# 重写为:
/quran/apis/tafsirs/index.php
# 同时 $_SERVER['REQUEST_URI'] 保持原始 URL
```

### PHP 解析

```php
$requestUri = $_SERVER['REQUEST_URI'];
// 值为: /quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1

$pattern = '#/quran/apis/tafsirs/([^/]+)/by_ayah/([^/?]+)#';
preg_match($pattern, $requestUri, $matches);

$slug = $matches[1];     // id-tafsir-kemenag
$ayahKey = $matches[2];  // 1:1
```

---

## 📁 文件清单

### 本地文件（已生成）

```
/Users/huwei/AndroidStudioProjects/quran0/
├── server_api_tafsir.php           ← 上传这个文件
├── COMPLETE_TAFSIR_API_DEPLOYMENT_GUIDE.md
├── SERVER_DEPLOYMENT_COMPLETE.md   ← 本文档
└── monitor_tafsir_logs.sh          ← 日志监控脚本
```

### 服务器文件（需要创建）

```
/public_html/quran/apis/tafsirs/
├── index.php      ← server_api_tafsir.php 重命名
└── .htaccess      ← 手动创建
```

---

## 🧪 测试清单

### 测试 1: .htaccess 是否生效

```bash
# 测试 URL 重写
curl -I https://apis.dochubai.com/quran/apis/tafsirs/test/by_ayah/1:1
```

**预期：** HTTP 404（带有错误消息）
**说明：** 说明 `.htaccess` 生效，但 slug 不存在

---

### 测试 2: 数据库连接

```bash
# 测试印尼语 Tafsir
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

**预期：** HTTP 200 + JSON 响应

---

### 测试 3: 不同经文

```bash
# 测试第 2 章 255 节（Ayat Al-Kursi）
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/2:255
```

---

## 📱 应用端测试

### 方法 A: 使用日志监控脚本

```bash
cd /Users/huwei/AndroidStudioProjects/quran0

# 启动日志监控
./monitor_tafsir_logs.sh
```

### 方法 B: 手动命令

```bash
# 在终端运行（单行命令）
adb logcat | grep -E "TafsirManager|ActivityTafsir|API_REQUEST|API_RESPONSE"
```

---

## 🔍 预期日志输出

### 成功加载印尼语 Tafsir

```
D TafsirManager: ✅ Parsed 3 language groups
D TafsirManager:    - id: 1 tafsirs
D MainActivity: ✅ Auto-selected Tafsir: id-tafsir-kemenag for language: id
D ActivityTafsir: 📥 Loading Indonesian Tafsir from custom server: id-tafsir-kemenag
D API_REQUEST: 🌐 GET https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
D API_RESPONSE: ✅ 200 https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
D ActivityTafsir: ✅ Tafsir loaded and cached successfully
```

### 服务器 API 未配置（会显示错误）

```
D ActivityTafsir: 📥 Loading Indonesian Tafsir from custom server: id-tafsir-kemenag
D API_REQUEST: 🌐 GET https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
E API_ERROR: ❌ HTTP 404 Not Found
E ActivityTafsir: ❌ Failed to load tafsir from custom server (dochubai.com): HTTP 404
```

---

## 🐛 常见问题排查

### 问题 1: HTTP 404 - 页面未找到

**原因：**
- `.htaccess` 文件不存在
- Apache `mod_rewrite` 未启用
- URL 路径错误

**解决方法：**
1. 确认 `.htaccess` 文件存在
2. 启用 mod_rewrite：
   ```bash
   a2enmod rewrite
   service apache2 restart
   ```
3. 检查 Apache 配置允许 `.htaccess`：
   ```apache
   <Directory /public_html/quran/apis/tafsirs>
       AllowOverride All
   </Directory>
   ```

---

### 问题 2: HTTP 500 - 服务器错误

**原因：**
- 数据库连接失败
- PHP 语法错误
- 表名或字段名错误

**解决方法：**
1. 检查 PHP 错误日志：
   ```bash
   tail -f /var/log/apache2/error.log
   ```
2. 测试数据库连接：
   ```bash
   mysql -u your_user -p your_database
   ```
3. 验证表结构：
   ```sql
   SHOW COLUMNS FROM tafsir_indonesian;
   ```

---

### 问题 3: 返回空白或 HTML 页面

**原因：**
- PHP 文件未正确执行
- `.htaccess` 配置错误

**解决方法：**
1. 检查文件权限：
   ```bash
   chmod 644 index.php
   chmod 644 .htaccess
   ```
2. 验证 PHP 是否工作：
   ```bash
   php -l index.php  # 语法检查
   ```

---

## 📝 快速部署命令（复制粘贴）

### 在服务器上执行

```bash
# 1. 创建目录（如果不存在）
mkdir -p /public_html/quran/apis/tafsirs

# 2. 创建 .htaccess
cat > /public_html/quran/apis/tafsirs/.htaccess << 'EOF'
RewriteEngine On
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule ^([^/]+)/by_ayah/([^/]+)$ index.php [L,QSA]
EOF

# 3. 上传 index.php（使用 FTP 或 SCP）
# 然后编辑数据库配置

# 4. 设置权限
chmod 644 /public_html/quran/apis/tafsirs/index.php
chmod 644 /public_html/quran/apis/tafsirs/.htaccess

# 5. 测试 API
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

---

## ✅ 部署成功标志

### 服务器端

- [x] `index.php` 已上传到 `/public_html/quran/apis/tafsirs/`
- [x] `.htaccess` 已创建
- [x] 数据库连接已配置
- [x] API 返回 HTTP 200 + 正确的 JSON

### 应用端

- [x] APK 已编译
- [x] 应用已安装
- [x] 日志显示正确的 API 请求
- [x] 印尼语 Tafsir 成功加载

---

## 🚀 现在开始

### 步骤 1: 上传文件到服务器

```
本地: /Users/huwei/AndroidStudioProjects/quran0/server_api_tafsir.php
↓
服务器: /public_html/quran/apis/tafsirs/index.php
```

### 步骤 2: 创建 .htaccess（复制上面的内容）

### 步骤 3: 配置数据库连接

### 步骤 4: 测试 API

```bash
curl https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

### 步骤 5: 编译应用并测试

```bash
# 在 Android Studio 中编译，或者：
./gradlew clean :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 监控日志
./monitor_tafsir_logs.sh
```

---

**准备好了吗？开始部署吧！** 🎉

