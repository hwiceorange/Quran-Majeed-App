# 📤 服务器上传说明

## ✅ 文件已准备好

**本地文件：** `server_deploy/available_tafsirs_info.json` (1.1 KB)

---

## 🎯 服务器上传位置

### 完整 URL
```
https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

### 服务器文件路径
```
/var/www/html/quran/apis/tafsirs/available_tafsirs_info.json
```

或者（取决于您的服务器配置）：
```
/var/www/apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

---

## 📂 目录结构

在服务器上创建以下目录结构：

```
/var/www/html/
└── quran/
    └── apis/
        └── tafsirs/
            └── available_tafsirs_info.json  ← 上传到这里
```

---

## 🚀 快速上传命令

### 方式 1: SCP（如果有 SSH 访问）

```bash
scp server_deploy/available_tafsirs_info.json \
  user@dochubai.com:/var/www/html/quran/apis/tafsirs/available_tafsirs_info.json
```

### 方式 2: SFTP

```bash
sftp user@dochubai.com
cd /var/www/html/quran/apis/tafsirs
put server_deploy/available_tafsirs_info.json available_tafsirs_info.json
chmod 644 available_tafsirs_info.json
bye
```

### 方式 3: FTP 客户端（FileZilla、Cyberduck 等）

1. 连接到：`dochubai.com`
2. 导航到：`/quran/apis/tafsirs/`（或 `/public_html/quran/apis/tafsirs/`）
3. 上传文件：`available_tafsirs_info.json`
4. 设置权限：644

### 方式 4: cPanel 文件管理器

1. 登录 cPanel
2. 打开"文件管理器"
3. 导航到：`public_html/quran/apis/tafsirs/`
4. 点击"上传"
5. 选择：`server_deploy/available_tafsirs_info.json`
6. 上传后设置权限为 644

---

## ✅ 验证上传

上传完成后，在浏览器中打开：

```
https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

应该显示 JSON 内容，包含 3 种语言的 Tafsir（英语、印尼语、阿拉伯语）。

或使用命令验证：

```bash
curl https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

---

## 🔧 服务器配置（如需要）

### Nginx 配置

在 `/etc/nginx/sites-available/apis.dochubai.com` 中添加：

```nginx
location /quran/apis/tafsirs/ {
    add_header 'Access-Control-Allow-Origin' '*' always;
    add_header 'Content-Type' 'application/json; charset=utf-8' always;
}
```

重启 Nginx：
```bash
sudo systemctl reload nginx
```

### Apache 配置

创建 `/var/www/html/quran/apis/tafsirs/.htaccess`：

```apache
<IfModule mod_headers.c>
    Header set Access-Control-Allow-Origin "*"
    Header set Content-Type "application/json; charset=utf-8"
</IfModule>

<Files "available_tafsirs_info.json">
    Order allow,deny
    Allow from all
</Files>
```

---

## 📞 需要帮助？

请提供以下信息：
1. 您的服务器访问方式（SSH/FTP/cPanel）
2. 服务器登录信息（私下提供）
3. Web 服务器类型（Nginx/Apache）

我可以为您提供更具体的上传命令！

---

## ⚡ 临时解决方案

**在服务器文件上传之前**，应用已经包含了备用的内置清单文件：

- 位置：`app/src/main/assets/tafsir/available_tafsirs_info.json`
- 加载顺序：先尝试网络，失败后自动使用内置文件

所以您可以：
1. **立即编译测试应用**（使用内置文件）
2. **稍后上传到服务器**（让应用从网络加载）

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

**文件已准备好，等待您上传到服务器！** 🚀
