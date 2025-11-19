
# 🚀 服务器部署说明

## 文件位置
本地文件: server_deploy/available_tafsirs_info.json
目标位置: https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json

## 自动部署（推荐）

如果您的服务器使用 Git 部署，请执行：

```bash
# 1. 提交到 Git
git add server_deploy/available_tafsirs_info.json
git add app/src/main/assets/tafsir/available_tafsirs_info.json
git commit -m "Add Tafsir manifest with Indonesian support"
git push origin main

# 2. 在服务器上部署
ssh user@dochubai.com << 'ENDSSH'
cd /var/www/apis.dochubai.com
git pull origin main
mkdir -p quran/apis/tafsirs
cp server_deploy/available_tafsirs_info.json quran/apis/tafsirs/
chmod 644 quran/apis/tafsirs/available_tafsirs_info.json
chown www-data:www-data quran/apis/tafsirs/available_tafsirs_info.json
ENDSSH
```

## 手动部署

如果需要手动上传，请：

1. 打开 FTP/SFTP 客户端
2. 连接到 dochubai.com
3. 上传文件到: /quran/apis/tafsirs/available_tafsirs_info.json
4. 设置权限: 644

## 验证部署

```bash
curl https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

应该返回 JSON 内容。
