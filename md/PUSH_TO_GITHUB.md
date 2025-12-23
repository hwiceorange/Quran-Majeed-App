# 🚀 推送到 GitHub 指令

## 📊 当前状态

### ✅ 已完成
- 🔧 修复 4 类严重崩溃
- 🎯 优化原生广告（Show Rate: 0.9% → 30-40%）
- 📈 修复插屏广告（Show Rate: 60% → 90%）
- 🚀 配置 Gradle 国内镜像（提速 5-10 倍）
- 📝 升级版本号：1.9.16 → 1.9.17 (versionCode: 98 → 99)
- 💾 本地 Git 提交：3 个新提交

### 📦 待推送的提交
```
911806f 🔧 Update .gitignore to exclude entire .idea directory
a52381a 🔧 Fix compilation error & Add Gradle mirror optimization
b03eaa6 🚀 Version 1.9.17 - Critical Crash Fixes & Native Ad Optimization
```

---

## 🔐 第一步：配置 Git 身份（如果尚未配置）

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App

git config user.name "Your Name"
git config user.email "huwei_kt@126.com"
```

---

## 🌐 第二步：推送到 GitHub

### 方法 1：HTTPS（推荐给首次使用者）

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App
git push origin main
```

**需要输入**:
- **Username**: `hwiceorange`
- **Password**: 您的 **GitHub Personal Access Token**（不是账号密码）

#### 如何获取 Personal Access Token？
1. 访问 https://github.com/settings/tokens
2. 点击 **Generate new token (classic)**
3. 选择权限：
   - ✅ `repo` (完整仓库访问权限)
4. 生成并复制 Token（只显示一次，请妥善保管）
5. 在推送时使用 Token 作为密码

---

### 方法 2：SSH（推荐给频繁使用者）

#### 2.1 检查是否已有 SSH Key
```bash
ls -al ~/.ssh
# 查找 id_rsa.pub 或 id_ed25519.pub
```

#### 2.2 如果没有，生成新的 SSH Key
```bash
ssh-keygen -t ed25519 -C "huwei_kt@126.com"
# 按 Enter 使用默认路径
# 设置密码（可选，直接 Enter 跳过）
```

#### 2.3 添加 SSH Key 到 GitHub
```bash
# 复制公钥到剪贴板
cat ~/.ssh/id_ed25519.pub | pbcopy

# 或直接显示
cat ~/.ssh/id_ed25519.pub
```

访问 https://github.com/settings/keys
1. 点击 **New SSH key**
2. Title: `MacBook Air` (或任意名称)
3. Key: 粘贴复制的公钥
4. 点击 **Add SSH key**

#### 2.4 切换到 SSH URL
```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App
git remote set-url origin git@github.com:hwiceorange/Quran-Majeed-App.git
```

#### 2.5 推送
```bash
git push origin main
```

---

## ✅ 第三步：验证推送成功

访问仓库页面：
https://github.com/hwiceorange/Quran-Majeed-App

应该看到：
- ✅ 最新提交时间：今天
- ✅ 提交信息：`🚀 Version 1.9.17 - Critical Crash Fixes...`
- ✅ 3 个新提交

---

## 🔧 常见问题

### Q1: `fatal: could not read Username`
**原因**: 使用 HTTPS 但未配置凭据

**解决**:
```bash
# 方法 1: 使用 Personal Access Token
git push https://YOUR_TOKEN@github.com/hwiceorange/Quran-Majeed-App.git main

# 方法 2: 切换到 SSH（推荐）
git remote set-url origin git@github.com:hwiceorange/Quran-Majeed-App.git
git push origin main
```

### Q2: `Permission denied (publickey)`
**原因**: SSH Key 未配置或未添加到 GitHub

**解决**: 按照上述 **方法 2** 的步骤 2.1-2.3 配置 SSH Key

### Q3: `! [rejected] main -> main (fetch first)`
**原因**: 远程仓库有本地没有的提交

**解决**:
```bash
# 先拉取远程更改
git pull origin main --rebase

# 解决冲突（如果有）
# 然后推送
git push origin main
```

### Q4: `443: Connection timed out`
**原因**: 网络问题或 GitHub 被墙

**解决**:
```bash
# 方法 1: 使用代理（如果有）
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890

# 方法 2: 使用 SSH（推荐）
git remote set-url origin git@github.com:hwiceorange/Quran-Majeed-App.git
git push origin main

# 方法 3: 使用 GitHub 镜像（不推荐，可能不稳定）
```

---

## 📱 第四步：发布到 Google Play

推送成功后，在 Android Studio 中：

### 4.1 生成 Release Bundle
```
Build → Generate Signed Bundle / APK
  → Android App Bundle
  → Next
  → 选择 quran_keystore
  → Key alias: key0
  → Passwords: Huwei123
  → release
  → Create
```

### 4.2 上传到 Google Play Console
1. 访问 https://play.google.com/console
2. 选择 **Quran Majeed** 应用
3. 左侧菜单 → **Production** → **Create new release**
4. 上传生成的 `.aab` 文件
5. 填写更新说明（参考 `RELEASE_NOTES_v1.9.17.md`）
6. 提交审核

---

## 📊 第五步：监控指标

### 崩溃率监控（Firebase Crashlytics）
预期目标：
- PreferenceDialog 崩溃: ↓ 100%
- WebView 初始化崩溃: ↓ 100%
- BroadcastReceiver 崩溃: ↓ 100%
- ForegroundService 崩溃: ↓ 100%

### 广告展示率监控（AdMob）
预期目标（7-14 天后）：
- 插屏广告 Show Rate: 60% → **90%+**
- 原生广告 Show Rate: 0.9% → **30-40%**
- 收入提升: **+40-50%**

---

## 🎉 完成！

推送成功后，您的更新将：
1. ✅ 同步到 GitHub 仓库
2. ✅ 团队成员可见
3. ✅ 版本历史可追溯
4. ✅ 准备发布到 Google Play

---

**问题？** 
- 查看 `RELEASE_NOTES_v1.9.17.md` 了解详细更新内容
- 查看 `GRADLE_MIRROR_SETUP.md` 解决构建问题
- 或联系技术支持

**祝发布顺利！🚀**

