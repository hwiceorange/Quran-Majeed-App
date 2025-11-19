# 📤 上传 Tafsir 清单文件到服务器

## 📋 文件信息

**本地文件：** `available_tafsirs_server.json`  
**目标 URL：** `https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json`

---

## 方式 1: 使用 SCP/SFTP 上传（推荐）

### 前提条件
- 拥有服务器 SSH 访问权限
- 知道服务器的 Web 根目录路径

### 上传命令

```bash
# 方式 1A: 使用 SCP
scp available_tafsirs_server.json user@dochubai.com:/var/www/html/quran/apis/tafsirs/available_tafsirs_info.json

# 方式 1B: 使用 SFTP
sftp user@dochubai.com
put available_tafsirs_server.json /var/www/html/quran/apis/tafsirs/available_tafsirs_info.json
exit
```

### 设置权限

```bash
ssh user@dochubai.com
chmod 644 /var/www/html/quran/apis/tafsirs/available_tafsirs_info.json
chown www-data:www-data /var/www/html/quran/apis/tafsirs/available_tafsirs_info.json
```

---

## 方式 2: 使用 FTP 客户端上传

### 推荐工具
- **macOS:** FileZilla, Cyberduck, Transmit
- **Windows:** FileZilla, WinSCP

### 步骤
1. 打开 FTP 客户端
2. 连接到 `dochubai.com`
3. 导航到 `/quran/apis/tafsirs/`
4. 上传 `available_tafsirs_server.json`
5. 重命名为 `available_tafsirs_info.json`

---

## 方式 3: 使用 cPanel/管理面板上传

### 步骤
1. 登录 cPanel: `https://dochubai.com:2083`
2. 打开 **文件管理器**
3. 导航到 `public_html/quran/apis/tafsirs/`
4. 点击 **上传**
5. 选择 `available_tafsirs_server.json`
6. 上传完成后，重命名为 `available_tafsirs_info.json`

---

## 方式 4: 使用 Git 部署

如果您的服务器使用 Git 部署，可以：

```bash
# 1. 添加文件到 Git 仓库
git add available_tafsirs_server.json
git commit -m "Add Indonesian Tafsir manifest"
git push origin main

# 2. 在服务器上拉取
ssh user@dochubai.com
cd /var/www/html/quran
git pull origin main
mv available_tafsirs_server.json apis/tafsirs/available_tafsirs_info.json
```

---

## 方式 5: 使用 API/管理后台上传

如果您的服务器有管理后台或上传 API：

### cURL 上传示例

```bash
curl -X POST https://apis.dochubai.com/admin/upload \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -F "file=@available_tafsirs_server.json" \
  -F "destination=/quran/apis/tafsirs/available_tafsirs_info.json"
```

---

## ✅ 验证上传

### 方法 1: 使用浏览器

打开 URL：
```
https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

**预期结果：** 显示 JSON 文件内容

### 方法 2: 使用 cURL

```bash
curl -I https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

**预期结果：**
```
HTTP/2 200
content-type: application/json
content-length: 1234
```

### 方法 3: 使用 wget

```bash
wget -O - https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

---

## 🔧 服务器配置（Nginx）

如果需要配置 Nginx 以正确提供 JSON 文件：

```nginx
# /etc/nginx/sites-available/dochubai.com
server {
    listen 443 ssl http2;
    server_name apis.dochubai.com;

    root /var/www/html;
    index index.php index.html;

    location /quran/apis/tafsirs/ {
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, OPTIONS' always;
        add_header 'Content-Type' 'application/json; charset=utf-8' always;
        
        if ($request_method = 'OPTIONS') {
            return 204;
        }
        
        try_files $uri $uri/ =404;
    }
}
```

重启 Nginx：
```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 🔧 服务器配置（Apache）

如果使用 Apache，创建 `.htaccess` 文件：

```apache
# /var/www/html/quran/apis/tafsirs/.htaccess
<IfModule mod_headers.c>
    Header set Access-Control-Allow-Origin "*"
    Header set Access-Control-Allow-Methods "GET, OPTIONS"
</IfModule>

<FilesMatch "\.json$">
    Header set Content-Type "application/json; charset=utf-8"
</FilesMatch>
```

---

## 📊 文件内容说明

### JSON 结构

```json
{
  "tafsirs": {
    "语言代码": [
      {
        "id": 999,              // Tafsir 资源 ID
        "name": "名称",         // Tafsir 名称
        "author_name": "作者",  // 作者名称
        "slug": "唯一标识符",   // API 调用时使用
        "language_name": "语言",// 语言名称
        "translated_name": {    // 翻译后的名称
          "name": "翻译名称",
          "language_name": "语言"
        }
      }
    ]
  }
}
```

### 当前支持的语言

| 语言代码 | 语言名称 | Tafsir 数量 | Slug |
|---------|---------|------------|------|
| `en` | English | 1 | `en-tafisr-ibn-kathir` |
| `id` | Indonesian | 1 | `id-tafsir-kemenag` |
| `ar` | Arabic | 1 | `ar-tafsir-muyassar` |

---

## 🚨 常见问题

### 问题 1: 文件上传成功，但无法访问

**原因：** 文件权限错误

**解决：**
```bash
ssh user@dochubai.com
chmod 644 /path/to/available_tafsirs_info.json
```

### 问题 2: 浏览器显示纯文本而非 JSON

**原因：** Content-Type 未设置

**解决：** 添加 `.htaccess` 或 Nginx 配置（见上文）

### 问题 3: CORS 错误

**原因：** 跨域资源共享未配置

**解决：** 添加 CORS 头（见 Nginx/Apache 配置）

---

## 📞 需要帮助？

如果您遇到以下情况，请提供：

1. **服务器访问方式**
   - SSH 登录命令示例
   - FTP/SFTP 连接信息
   - cPanel/管理面板 URL

2. **Web 服务器类型**
   - Nginx / Apache / Caddy / 其他？

3. **目录结构**
   - Web 根目录路径
   - `apis.dochubai.com` 的实际文件位置

我可以根据您的具体环境提供定制化的上传脚本！

---

## 🎯 下一步

上传完成后：

1. ✅ **验证文件可访问**
   ```bash
   curl https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
   ```

2. ✅ **测试应用**
   ```bash
   adb logcat | grep TafsirManager
   # 期待看到：✅ Network load successful
   ```

3. ✅ **配置 Tafsir 内容 API**
   - 确保 `/api/qdc/tafsirs/id-tafsir-kemenag/by_ayah/:verseKey` 端点可用

---

**准备好上传了吗？请告诉我您的服务器访问方式，我可以提供更具体的命令！** 🚀

