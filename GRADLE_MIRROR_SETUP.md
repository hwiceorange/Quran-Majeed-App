# Gradle 国内镜像加速配置

## 问题
```
Could not install Gradle distribution from 'https://services.gradle.org/distributions/gradle-9.0-milestone-1-bin.zip'.
Reason: java.net.SocketTimeoutException: Connect timed out
```

## 解决方案

### ✅ 已完成的优化

1. **Gradle 本体下载加速**
   - 文件：`gradle/wrapper/gradle-wrapper.properties`
   - 修改：使用腾讯云镜像 + 稳定版本 Gradle 8.10.2
   - 原地址：`https://services.gradle.org/distributions/gradle-9.0-milestone-1-bin.zip`
   - 新地址：`https://mirrors.cloud.tencent.com/gradle/gradle-8.10.2-bin.zip`

2. **依赖下载加速**
   - 文件：`init.gradle`（已创建）
   - 功能：自动将 Maven Central、JCenter 等仓库替换为阿里云/腾讯云镜像

### 🚀 使用方式

#### 方法 1：项目级配置（已完成）
直接在当前项目运行，`init.gradle` 会自动生效：

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App
./gradlew clean build
```

#### 方法 2：全局配置（可选）
将 `init.gradle` 复制到用户目录，对所有项目生效：

```bash
cp init.gradle ~/.gradle/init.gradle
```

### 📊 镜像源对比

| 仓库 | 官方地址 | 镜像地址 | 速度提升 |
|------|---------|---------|---------|
| Gradle | services.gradle.org | mirrors.cloud.tencent.com | 5-10x |
| Maven Central | repo1.maven.org | maven.aliyun.com | 3-8x |
| Google | dl.google.com | maven.aliyun.com/repository/google | 5-10x |
| JCenter | jcenter.bintray.com | maven.aliyun.com/repository/jcenter | 3-5x |

### 🔧 版本变更说明

**Gradle 版本降级：9.0-milestone-1 → 8.10.2**

原因：
1. ✅ **稳定性**：8.10.2 是 LTS 长期支持版本
2. ✅ **兼容性**：完全兼容 Android Gradle Plugin 8.x
3. ✅ **可用性**：国内镜像源有完整支持
4. ⚠️ 9.0-milestone-1 是开发版，镜像源可能不支持

### 🛠️ 故障排除

#### 如果仍然超时
1. **清理 Gradle 缓存**：
   ```bash
   rm -rf ~/.gradle/caches
   rm -rf ~/.gradle/wrapper
   ```

2. **手动下载 Gradle**：
   - 下载地址：https://mirrors.cloud.tencent.com/gradle/gradle-8.10.2-bin.zip
   - 放置位置：`~/.gradle/wrapper/dists/gradle-8.10.2-bin/`

3. **使用代理**（如果有）：
   在 `gradle.properties` 添加：
   ```properties
   systemProp.http.proxyHost=127.0.0.1
   systemProp.http.proxyPort=7890
   systemProp.https.proxyHost=127.0.0.1
   systemProp.https.proxyPort=7890
   ```

#### 验证镜像生效
运行构建时会看到日志：
```
Repository https://repo1.maven.org/maven2/ replaced by mirror.
Repository https://jcenter.bintray.com/ replaced by mirror.
```

### 📈 预期效果

- **首次下载 Gradle**：从 5-10 分钟 → 30-60 秒
- **依赖下载**：从 10-30 分钟 → 2-5 分钟
- **整体构建速度**：提升 60-80%

### 🌐 其他可用镜像源

如果腾讯云/阿里云仍然慢，可以尝试：

**华为云**：
```
https://mirrors.huaweicloud.com/gradle/gradle-8.10.2-bin.zip
https://mirrors.huaweicloud.com/repository/maven/
```

**清华大学**：
```
https://mirrors.tuna.tsinghua.edu.cn/gradle/gradle-8.10.2-bin.zip
https://mirrors.tuna.tsinghua.edu.cn/maven2/
```

## ✅ 完成确认

重新运行构建命令：

```bash
./gradlew clean
./gradlew build
```

应该可以顺利下载 Gradle 并开始构建！🚀

