#!/bin/bash

##############################################
# 快速测试脚本 - 搜索功能验证
# Quick Test Script - Search Feature
##############################################

echo "=========================================="
echo "🔍 Quran App - Search Feature Test"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 步骤 1: 清理旧的构建文件
echo "📦 Step 1: Cleaning old builds..."
./gradlew clean > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Clean successful${NC}"
else
    echo -e "${RED}❌ Clean failed${NC}"
    exit 1
fi
echo ""

# 步骤 2: 编译 Debug 包
echo "🔨 Step 2: Building Debug APK..."
echo "   (This may take 1-2 minutes...)"
./gradlew assembleDebug --no-daemon 2>&1 | tail -20

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Build successful${NC}"
else
    echo -e "${RED}❌ Build failed${NC}"
    echo "   Please check the error messages above."
    exit 1
fi
echo ""

# 步骤 3: 检查 APK 是否存在
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo -e "${GREEN}✅ APK found: $APK_PATH${NC}"
    
    # 显示 APK 信息
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "   Size: $APK_SIZE"
else
    echo -e "${RED}❌ APK not found at: $APK_PATH${NC}"
    exit 1
fi
echo ""

# 步骤 4: 检查设备连接
echo "📱 Step 3: Checking connected devices..."
DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l | xargs)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}⚠️  No devices connected${NC}"
    echo ""
    echo "Please connect your Android device via USB and enable USB Debugging."
    echo ""
    echo "Manual installation:"
    echo "   adb install -r $APK_PATH"
    echo ""
    exit 0
else
    echo -e "${GREEN}✅ Found $DEVICE_COUNT device(s)${NC}"
    adb devices | grep "device" | grep -v "List"
fi
echo ""

# 步骤 5: 安装 APK
echo "📲 Step 4: Installing APK to device..."
adb install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Installation successful${NC}"
else
    echo -e "${RED}❌ Installation failed${NC}"
    echo "   Please check device connection and USB debugging."
    exit 1
fi
echo ""

# 步骤 6: 启动应用
echo "🚀 Step 5: Launching app..."
PACKAGE_NAME="com.quran.quranaudio.online"
ACTIVITY_NAME=".prayertimes.ui.MainActivity"

adb shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME" > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ App launched${NC}"
else
    echo -e "${YELLOW}⚠️  Could not auto-launch app${NC}"
    echo "   Please manually open the app on your device."
fi
echo ""

# 测试指南
echo "=========================================="
echo "📋 Test Checklist"
echo "=========================================="
echo ""
echo "Please test the following:"
echo ""
echo "  ✅ Test 1: Home Search Button"
echo "     1. Ensure you're on the Home page"
echo "     2. Click the search icon (🔍) in the top-right"
echo "     3. Verify search page opens without crash"
echo "     4. Type a query (e.g., 'Allah')"
echo "     5. Verify results appear"
echo "     6. Click a result and verify navigation"
echo ""
echo "  ✅ Test 2: Voice Search"
echo "     1. Open search page"
echo "     2. Click microphone icon"
echo "     3. Speak a query"
echo "     4. Verify voice-to-text works"
echo ""
echo "  ✅ Test 3: Search History"
echo "     1. Perform a search"
echo "     2. Go back to Home"
echo "     3. Open search again"
echo "     4. Verify previous search appears"
echo ""
echo "  ✅ Test 4: Quick Jump"
echo "     1. Open search"
echo "     2. Type '2:255'"
echo "     3. Verify direct navigation to verse"
echo ""
echo "=========================================="
echo "🎉 Installation Complete!"
echo "=========================================="
echo ""
echo "📊 Report any issues with:"
echo "   - Device model and Android version"
echo "   - Error messages or crashes"
echo "   - Steps to reproduce"
echo ""
echo "📁 Logs can be captured with:"
echo "   adb logcat -d > search_test_log.txt"
echo ""
echo "Good luck with testing! 🚀"
echo ""

