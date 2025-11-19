# 📦 Tafsir API 部署文件

## 📋 目录内容

```
server_deploy/
├── index.php                   # PHP API 主文件
├── .htaccess                   # URL 重写配置
├── test_db_connection.php      # 数据库连接测试（测试完删除）
└── README.md                   # 本文件
```

---

## ⚠️ 部署前必须修改

### 修改文件 1: `index.php`

**找到第 26-29 行：**

```php
$DB_HOST = 'localhost';
$DB_NAME = 'u853729749_quran_database';
$DB_USER = 'u853729749_YOUR_USERNAME';    // ⚠️ 改为您的数据库用户名
$DB_PASS = 'YOUR_PASSWORD';               // ⚠️ 改为您的数据库密码
```

### 修改文件 2: `test_db_connection.php`

**找到第 14-17 行，修改相同内容**

---

## 📤 上传到服务器

**目标位置：**
```
/public_html/quran/apis/tafsirs/
```

**上传文件：**
- ✅ `index.php`
- ✅ `.htaccess`
- ✅ `test_db_connection.php`（测试用，测试完删除）

---

## 🧪 测试

### 测试 1: 数据库连接
```
https://apis.dochubai.com/quran/apis/tafsirs/test_db_connection.php
```

### 测试 2: API 端点
```
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

---

## 🔒 安全提示

**测试成功后，请立即删除：**
- `/public_html/quran/apis/tafsirs/test_db_connection.php`

**原因：** 该文件包含数据库连接信息，可能存在安全风险。

---

## 📚 详细说明

请查看项目根目录的 **`START_HERE.md`** 获取完整部署指南。

