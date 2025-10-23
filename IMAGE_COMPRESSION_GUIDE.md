# 图片压缩优化指南

生成时间: 2025-10-23

---

## 🎯 目标文件清单

### 🔥 优先级1：超大文件（必须压缩）

| 文件 | 当前大小 | 目标大小 | 节省 | 位置 |
|------|----------|----------|------|------|
| **tasbih_bg.jpg** | **5.6 MB** | 500 KB | **5.1 MB** | drawable-v24/ |

### 🔴 优先级2：大图片（强烈建议压缩）

| 文件 | 当前大小 | 目标大小 | 节省 | 位置 |
|------|----------|----------|------|------|
| tasbeeh_7.png | 167 KB | 80 KB | 87 KB | drawable-v24/ |
| tasbeeh_6.png | 164 KB | 80 KB | 84 KB | drawable-v24/ |
| tasbeeh_5.png | 158 KB | 75 KB | 83 KB | drawable-v24/ |
| tasbeeh_4.png | 155 KB | 75 KB | 80 KB | drawable-v24/ |
| tasbeeh_3.png | 152 KB | 70 KB | 82 KB | drawable-v24/ |
| tasbeeh_2.png | 147 KB | 70 KB | 77 KB | drawable-v24/ |
| tasbeeh_1.png | 144 KB | 70 KB | 74 KB | drawable-v24/ |
| **小计** | **1.1 MB** | **520 KB** | **~600 KB** | |

### 🟠 优先级3：中等图片

| 文件 | 当前大小 | 目标大小 | 位置 |
|------|----------|----------|------|
| compass_3.png | 268 KB | 150 KB | drawable-xxhdpi/ |
| compass_4.png | 216 KB | 120 KB | drawable-xxhdpi/ |
| meccalive.jpg | 254 KB | 120 KB | drawable-xxhdpi/ |
| medinalive.jpg | 229 KB | 110 KB | drawable-xxhdpi/ |
| today_header.jpg | 284 KB | 140 KB | drawable-v24/ |
| splash.jpg | 271 KB | 140 KB | drawable/ |
| img_prayer.png | 164 KB | 80 KB | drawable-v24/ |

### 🟡 优先级4：Wudu 步骤图

12张图片，每张 102-128 KB，总计约 1.4 MB
- wudu_step_01.jpg ~ wudu_step_12.jpg
- 位置：drawable-xxhdpi/
- 目标：每张 50-60 KB，总计约 700 KB

---

## 🛠️ 压缩方法

### 方法1：在线工具（推荐，简单易用）

#### A. TinyPNG (PNG压缩)
- 网址：https://tinypng.com
- 优点：免费，保持高质量，批量处理
- 限制：每次最多20张，每张最大5MB
- **适用于**：Tasbeeh 系列 PNG

**步骤：**
1. 打开 https://tinypng.com
2. 拖拽 7 个 tasbeeh_*.png 文件
3. 等待压缩完成（通常压缩50-70%）
4. 下载压缩后的文件
5. 替换原文件

#### B. Squoosh (JPG/PNG压缩)
- 网址：https://squoosh.app
- 优点：免费，可视化调整，支持 WebP 转换
- **适用于**：tasbih_bg.jpg, splash.jpg 等

**步骤：**
1. 打开 https://squoosh.app
2. 拖拽图片（如 tasbih_bg.jpg）
3. 右侧选择压缩方式：
   - JPG：质量设为 85-90%
   - 或转换为 WebP：质量 80-85%
4. 对比压缩前后效果
5. 下载并替换

---

### 方法2：命令行工具（批量处理）

#### 安装工具（Mac）
```bash
# 使用 Homebrew 安装
brew install imagemagick
brew install pngquant
brew install jpegoptim
```

#### 安装工具（Linux/Ubuntu）
```bash
sudo apt-get install imagemagick
sudo apt-get install pngquant
sudo apt-get install jpegoptim
```

---

### 🔥 压缩脚本（复制执行）

#### 脚本1：压缩 tasbih_bg.jpg (5.6MB → 500KB)

```bash
#!/bin/bash
cd /Users/huwei/AndroidStudioProjects/quran0/app/src/main/res/drawable-v24

# 备份原文件
cp tasbih_bg.jpg tasbih_bg_original.jpg

# 方法A：使用 ImageMagick（推荐）
# 调整大小并压缩质量
convert tasbih_bg.jpg \
  -resize 1920x1080 \
  -quality 85 \
  -strip \
  tasbih_bg_compressed.jpg

# 检查压缩后大小
ls -lh tasbih_bg_compressed.jpg

# 如果满意，替换原文件
mv tasbih_bg_compressed.jpg tasbih_bg.jpg

echo "✅ tasbih_bg.jpg 压缩完成"
```

**参数说明：**
- `-resize 1920x1080`：调整为全高清分辨率（根据需要调整）
- `-quality 85`：质量85%（可调整70-95，数字越大质量越好但文件越大）
- `-strip`：移除元数据，进一步减小文件

---

#### 脚本2：批量压缩 Tasbeeh PNG 系列

```bash
#!/bin/bash
cd /Users/huwei/AndroidStudioProjects/quran0/app/src/main/res/drawable-v24

# 创建备份目录
mkdir -p backup_tasbeeh
cp tasbeeh_*.png backup_tasbeeh/

# 使用 pngquant 压缩（保持高质量）
for file in tasbeeh_*.png; do
  echo "压缩 $file..."
  pngquant --quality=70-85 --speed 1 --force --output "$file" "$file"
done

echo "✅ Tasbeeh 系列压缩完成"
ls -lh tasbeeh_*.png
```

---

#### 脚本3：批量压缩 Wudu 步骤图

```bash
#!/bin/bash
cd /Users/huwei/AndroidStudioProjects/quran0/app/src/main/res/drawable-xxhdpi

# 备份
mkdir -p backup_wudu
cp wudu_step_*.jpg backup_wudu/

# 压缩 JPG
for file in wudu_step_*.jpg; do
  echo "压缩 $file..."
  convert "$file" \
    -resize 800x600 \
    -quality 80 \
    -strip \
    "${file%.jpg}_compressed.jpg"
  
  mv "${file%.jpg}_compressed.jpg" "$file"
done

echo "✅ Wudu 步骤图压缩完成"
ls -lh wudu_step_*.jpg
```

---

#### 脚本4：压缩罗盘和其他大图

```bash
#!/bin/bash
cd /Users/huwei/AndroidStudioProjects/quran0/app/src/main/res

# 备份
mkdir -p backup_large_images

# 压缩 compass PNG
echo "压缩罗盘图片..."
cp drawable-xxhdpi/compass_3.png backup_large_images/
cp drawable-xxhdpi/compass_4.png backup_large_images/

pngquant --quality=65-80 --force \
  --output drawable-xxhdpi/compass_3.png \
  drawable-xxhdpi/compass_3.png

pngquant --quality=65-80 --force \
  --output drawable-xxhdpi/compass_4.png \
  drawable-xxhdpi/compass_4.png

# 压缩直播背景
echo "压缩直播背景..."
cp drawable-xxhdpi/meccalive.jpg backup_large_images/
cp drawable-xxhdpi/medinalive.jpg backup_large_images/

convert drawable-xxhdpi/meccalive.jpg \
  -quality 75 -strip drawable-xxhdpi/meccalive.jpg

convert drawable-xxhdpi/medinalive.jpg \
  -quality 75 -strip drawable-xxhdpi/medinalive.jpg

# 压缩其他 JPG
echo "压缩其他大图..."
cp drawable-v24/today_header.jpg backup_large_images/
cp drawable/splash.jpg backup_large_images/

convert drawable-v24/today_header.jpg \
  -quality 80 -strip drawable-v24/today_header.jpg

convert drawable/splash.jpg \
  -quality 80 -strip drawable/splash.jpg

echo "✅ 所有大图压缩完成"
```

---

### 🎨 完整压缩脚本（一键执行）

```bash
#!/bin/bash
# APK 包体优化 - 图片压缩脚本
# 执行前请确保已安装 ImageMagick 和 pngquant

set -e  # 遇到错误立即停止

PROJECT_ROOT="/Users/huwei/AndroidStudioProjects/quran0"
cd "$PROJECT_ROOT"

echo "🖼️  开始图片压缩优化..."
echo ""

# 创建备份目录
BACKUP_DIR="image_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
echo "📁 备份目录：$BACKUP_DIR"

# 1. 压缩超大文件 tasbih_bg.jpg
echo ""
echo "🔥 [1/4] 压缩 tasbih_bg.jpg (5.6MB → ~500KB)..."
cp app/src/main/res/drawable-v24/tasbih_bg.jpg "$BACKUP_DIR/"
convert app/src/main/res/drawable-v24/tasbih_bg.jpg \
  -resize 1920x1080 \
  -quality 85 \
  -strip \
  app/src/main/res/drawable-v24/tasbih_bg_temp.jpg
mv app/src/main/res/drawable-v24/tasbih_bg_temp.jpg \
   app/src/main/res/drawable-v24/tasbih_bg.jpg

NEW_SIZE=$(du -h app/src/main/res/drawable-v24/tasbih_bg.jpg | cut -f1)
echo "✅ 完成！新大小：$NEW_SIZE"

# 2. 压缩 Tasbeeh PNG 系列
echo ""
echo "📿 [2/4] 压缩 Tasbeeh 系列 PNG (1.1MB → ~520KB)..."
cd app/src/main/res/drawable-v24
for file in tasbeeh_*.png; do
  cp "$file" "$PROJECT_ROOT/$BACKUP_DIR/"
  pngquant --quality=70-85 --speed 1 --force --output "$file" "$file" 2>/dev/null || echo "⚠️  $file 跳过"
done
cd "$PROJECT_ROOT"
echo "✅ Tasbeeh 系列压缩完成"

# 3. 压缩 Wudu 步骤图
echo ""
echo "💧 [3/4] 压缩 Wudu 步骤图 (1.4MB → ~700KB)..."
cd app/src/main/res/drawable-xxhdpi
for file in wudu_step_*.jpg; do
  cp "$file" "$PROJECT_ROOT/$BACKUP_DIR/"
  convert "$file" \
    -resize 800x600 \
    -quality 80 \
    -strip \
    "${file}_temp.jpg"
  mv "${file}_temp.jpg" "$file"
done
cd "$PROJECT_ROOT"
echo "✅ Wudu 步骤图压缩完成"

# 4. 压缩其他大图
echo ""
echo "🖼️  [4/4] 压缩其他大图..."

# 罗盘图
cp app/src/main/res/drawable-xxhdpi/compass_3.png "$BACKUP_DIR/"
cp app/src/main/res/drawable-xxhdpi/compass_4.png "$BACKUP_DIR/"
pngquant --quality=65-80 --force \
  --output app/src/main/res/drawable-xxhdpi/compass_3.png \
  app/src/main/res/drawable-xxhdpi/compass_3.png 2>/dev/null || echo "⚠️  compass_3.png 跳过"
pngquant --quality=65-80 --force \
  --output app/src/main/res/drawable-xxhdpi/compass_4.png \
  app/src/main/res/drawable-xxhdpi/compass_4.png 2>/dev/null || echo "⚠️  compass_4.png 跳过"

# 直播背景
cp app/src/main/res/drawable-xxhdpi/meccalive.jpg "$BACKUP_DIR/"
cp app/src/main/res/drawable-xxhdpi/medinalive.jpg "$BACKUP_DIR/"
convert app/src/main/res/drawable-xxhdpi/meccalive.jpg \
  -quality 75 -strip app/src/main/res/drawable-xxhdpi/meccalive_temp.jpg
mv app/src/main/res/drawable-xxhdpi/meccalive_temp.jpg \
   app/src/main/res/drawable-xxhdpi/meccalive.jpg

convert app/src/main/res/drawable-xxhdpi/medinalive.jpg \
  -quality 75 -strip app/src/main/res/drawable-xxhdpi/medinalive_temp.jpg
mv app/src/main/res/drawable-xxhdpi/medinalive_temp.jpg \
   app/src/main/res/drawable-xxhdpi/medinalive.jpg

# 其他 JPG
cp app/src/main/res/drawable-v24/today_header.jpg "$BACKUP_DIR/"
cp app/src/main/res/drawable/splash.jpg "$BACKUP_DIR/"
convert app/src/main/res/drawable-v24/today_header.jpg \
  -quality 80 -strip app/src/main/res/drawable-v24/today_header_temp.jpg
mv app/src/main/res/drawable-v24/today_header_temp.jpg \
   app/src/main/res/drawable-v24/today_header.jpg

convert app/src/main/res/drawable/splash.jpg \
  -quality 80 -strip app/src/main/res/drawable/splash_temp.jpg
mv app/src/main/res/drawable/splash_temp.jpg \
   app/src/main/res/drawable/splash.jpg

echo "✅ 其他大图压缩完成"

# 统计
echo ""
echo "================================================"
echo "✅ 图片压缩全部完成！"
echo "================================================"
echo ""
echo "📊 预计节省空间：6-8 MB"
echo "📁 备份位置：$BACKUP_DIR"
echo ""
echo "⚠️  请立即测试应用，确认："
echo "  1. Tasbih 背景图清晰度"
echo "  2. Tasbeeh 念珠图片清晰度"
echo "  3. Wudu 步骤图可读性"
echo "  4. 所有界面图片显示正常"
echo ""
echo "如有问题，从备份恢复："
echo "  cp $BACKUP_DIR/* app/src/main/res/xxx/"
echo ""
```

---

## 📋 压缩后测试清单

### 视觉质量检查

#### 1. Tasbih 界面
- [ ] 背景图片清晰，无明显压缩痕迹
- [ ] 念珠图片1-7清晰，边缘平滑
- [ ] 整体视觉效果良好

#### 2. Wudu 洗礼指南
- [ ] 12个步骤图片清晰可辨
- [ ] 文字/细节清晰可读
- [ ] 没有模糊或失真

#### 3. 罗盘界面
- [ ] 罗盘图片清晰
- [ ] 刻度和指针可读

#### 4. 启动和主界面
- [ ] splash.jpg 启动画面正常
- [ ] today_header.jpg 清晰
- [ ] 直播卡片背景清晰

### 性能检查
- [ ] 应用启动速度未变慢
- [ ] 界面加载流畅
- [ ] 内存占用正常

---

## 🔄 回滚方法

```bash
# 从备份恢复所有文件
cd /Users/huwei/AndroidStudioProjects/quran0
cp image_backup_YYYYMMDD_HHMMSS/* app/src/main/res/drawable-v24/
cp image_backup_YYYYMMDD_HHMMSS/* app/src/main/res/drawable-xxhdpi/
cp image_backup_YYYYMMDD_HHMMSS/* app/src/main/res/drawable/
```

---

## 📊 预期效果总结

| 文件类型 | 压缩前 | 压缩后 | 节省 |
|----------|--------|--------|------|
| tasbih_bg.jpg | 5.6 MB | ~500 KB | 5.1 MB |
| Tasbeeh PNG 系列 | 1.1 MB | ~520 KB | 600 KB |
| Wudu JPG 系列 | 1.4 MB | ~700 KB | 700 KB |
| 其他大图 | ~2 MB | ~1 MB | 1 MB |
| **总计** | **10.1 MB** | **2.7 MB** | **7.4 MB** |

**总节省空间：约 6-8 MB** 🎉

---

## ✅ 下一步

完成图片压缩后，可以继续：
1. 分析字体文件使用情况
2. 运行 lint 检查未使用资源
3. 考虑 WebP 转换（进一步优化）
4. 启用 APK 分析器查看最终效果

**准备好执行压缩了吗？** 🚀

