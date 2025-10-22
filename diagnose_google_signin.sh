#!/bin/bash

# Google 登录配置诊断脚本
# 用于诊断 "Sign-in canceled" 错误

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}🔍 Google 登录配置诊断工具${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# 错误计数
ERRORS=0
WARNINGS=0

# 1. 检查 SHA-1 指纹
echo -e "${BLUE}[1/7] 检查 SHA-1 指纹...${NC}"
SHA1=$(keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep "SHA1:" | head -1)
if [ -n "$SHA1" ]; then
    echo -e "${GREEN}✅ SHA-1 指纹已找到${NC}"
    echo "$SHA1"
    echo ""
    echo -e "${YELLOW}⚠️  请确保此 SHA-1 已添加到 Firebase Console！${NC}"
    echo "   https://console.firebase.google.com/ → Project Settings → Your apps"
else
    echo -e "${RED}❌ 错误：无法获取 SHA-1 指纹${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 2. 检查 google-services.json
echo -e "${BLUE}[2/7] 检查 google-services.json 配置...${NC}"
if [ ! -f "app/google-services.json" ]; then
    echo -e "${RED}❌ 错误：google-services.json 文件不存在！${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ google-services.json 文件存在${NC}"
    
    # 检查 oauth_client 是否为空
    if grep -q '"oauth_client": \[\]' app/google-services.json; then
        echo -e "${RED}❌ 严重错误：oauth_client 是空数组！${NC}"
        echo -e "${RED}   这是 'Sign-in canceled' 的根本原因！${NC}"
        echo ""
        echo -e "${YELLOW}修复步骤：${NC}"
        echo "   1. 访问 Firebase Console"
        echo "   2. 添加上面显示的 SHA-1 指纹"
        echo "   3. 启用 Google Sign-In"
        echo "   4. 下载新的 google-services.json 并替换此文件"
        ERRORS=$((ERRORS + 1))
    else
        echo -e "${GREEN}✅ oauth_client 已配置${NC}"
        echo ""
        echo "OAuth 客户端配置："
        cat app/google-services.json | grep -A 10 "oauth_client" | head -15
    fi
fi
echo ""

# 3. 检查 GoogleAuthManager.java 中的 Web Client ID
echo -e "${BLUE}[3/7] 检查代码中的 Web Client ID 配置...${NC}"
if [ -f "app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java" ]; then
    if grep -q "YOUR_WEB_CLIENT_ID_HERE" app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java; then
        echo -e "${RED}❌ 错误：Web Client ID 未配置（仍是占位符）${NC}"
        echo "   文件：GoogleAuthManager.java"
        echo "   行号：约 46"
        echo ""
        echo -e "${YELLOW}修复步骤：${NC}"
        echo "   1. 从 Firebase Console 获取 Web Client ID"
        echo "   2. 替换 GoogleAuthManager.java 中的 YOUR_WEB_CLIENT_ID_HERE"
        ERRORS=$((ERRORS + 1))
    else
        WEB_CLIENT_ID=$(grep "requestIdToken" app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java | grep -oE '"[^"]+"' | head -1)
        echo -e "${GREEN}✅ Web Client ID 已配置${NC}"
        echo "   Client ID: $WEB_CLIENT_ID"
    fi
else
    echo -e "${RED}❌ 错误：GoogleAuthManager.java 文件不存在！${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 4. 检查设备连接
echo -e "${BLUE}[4/7] 检查 ADB 设备连接...${NC}"
if ! command -v adb &> /dev/null; then
    echo -e "${YELLOW}⚠️  警告：ADB 未找到，跳过设备检查${NC}"
    WARNINGS=$((WARNINGS + 1))
else
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l | xargs)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo -e "${YELLOW}⚠️  警告：未检测到设备${NC}"
        WARNINGS=$((WARNINGS + 1))
    else
        DEVICE_NAME=$(adb shell getprop ro.product.model 2>/dev/null)
        ANDROID_VERSION=$(adb shell getprop ro.build.version.release 2>/dev/null)
        echo -e "${GREEN}✅ 设备已连接: $DEVICE_NAME (Android $ANDROID_VERSION)${NC}"
        
        # 检查是否是 Android 9
        if [ "$ANDROID_VERSION" = "9" ]; then
            echo -e "${YELLOW}   ℹ️  Android 9 系统，请确保 Google Play Services 已更新${NC}"
        fi
    fi
fi
echo ""

# 5. 检查 Google Play Services（如果设备已连接）
echo -e "${BLUE}[5/7] 检查 Google Play Services...${NC}"
if command -v adb &> /dev/null && [ "$DEVICE_COUNT" -gt 0 ]; then
    GMS_VERSION=$(adb shell dumpsys package com.google.android.gms 2>/dev/null | grep "versionName" | head -1 | cut -d'=' -f2)
    if [ -n "$GMS_VERSION" ]; then
        echo -e "${GREEN}✅ Google Play Services 版本: $GMS_VERSION${NC}"
        
        # 提取主版本号（如 20.x.x）
        MAJOR_VERSION=$(echo $GMS_VERSION | cut -d'.' -f1)
        if [ "$MAJOR_VERSION" -lt 20 ]; then
            echo -e "${YELLOW}⚠️  警告：版本较旧，建议更新到 20.0.0+${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
    else
        echo -e "${RED}❌ 错误：无法检测 Google Play Services${NC}"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  跳过（设备未连接）${NC}"
fi
echo ""

# 6. 测试网络连接（如果设备已连接）
echo -e "${BLUE}[6/7] 测试网络连接到 Google 服务...${NC}"
if command -v adb &> /dev/null && [ "$DEVICE_COUNT" -gt 0 ]; then
    # 测试连接到 accounts.google.com
    PING_RESULT=$(adb shell "ping -c 2 accounts.google.com 2>&1" | grep "bytes from")
    if [ -n "$PING_RESULT" ]; then
        echo -e "${GREEN}✅ 可以连接到 accounts.google.com${NC}"
    else
        echo -e "${RED}❌ 错误：无法连接到 accounts.google.com${NC}"
        echo -e "${YELLOW}   可能原因：网络限制、防火墙、VPN${NC}"
        echo -e "${YELLOW}   建议：切换到 4G/5G 移动网络测试${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
    # 测试连接到 Firebase
    FIREBASE_RESULT=$(adb shell "ping -c 2 firebaseapp.com 2>&1" | grep "bytes from")
    if [ -n "$FIREBASE_RESULT" ]; then
        echo -e "${GREEN}✅ 可以连接到 firebaseapp.com${NC}"
    else
        echo -e "${YELLOW}⚠️  警告：无法连接到 firebaseapp.com${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  跳过（设备未连接）${NC}"
fi
echo ""

# 7. 检查应用是否已安装
echo -e "${BLUE}[7/7] 检查应用安装状态...${NC}"
if command -v adb &> /dev/null && [ "$DEVICE_COUNT" -gt 0 ]; then
    if adb shell pm list packages | grep -q "com.quran.quranaudio.online"; then
        echo -e "${GREEN}✅ 应用已安装${NC}"
        
        # 检查应用是否正在运行
        if adb shell pidof com.quran.quranaudio.online > /dev/null 2>&1; then
            APP_PID=$(adb shell pidof com.quran.quranaudio.online)
            echo -e "${GREEN}✅ 应用正在运行 (PID: $APP_PID)${NC}"
        else
            echo -e "${YELLOW}⚠️  应用未运行${NC}"
        fi
    else
        echo -e "${RED}❌ 应用未安装${NC}"
        echo "   请先运行：./gradlew installDebug"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  跳过（设备未连接）${NC}"
fi
echo ""

# 总结
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}📊 诊断总结${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ 所有检查通过！${NC}"
    echo ""
    echo "如果登录仍然失败，请运行："
    echo "  adb logcat -c"
    echo "  # 在设备上执行登录操作"
    echo "  adb logcat -d | grep -E 'GoogleAuth|FragMain|StatusCode'"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  发现 $WARNINGS 个警告，但配置基本正确${NC}"
    echo ""
    echo "建议先测试登录功能，如果失败再处理警告。"
else
    echo -e "${RED}❌ 发现 $ERRORS 个错误，$WARNINGS 个警告${NC}"
    echo ""
    echo -e "${YELLOW}🔧 优先修复以下问题：${NC}"
    echo ""
    
    # 根据错误给出具体建议
    if grep -q '"oauth_client": \[\]' app/google-services.json 2>/dev/null; then
        echo -e "${RED}1. oauth_client 为空（最严重）${NC}"
        echo "   → 必须在 Firebase Console 添加 SHA-1 指纹"
        echo "   → 然后下载新的 google-services.json"
        echo ""
    fi
    
    if grep -q "YOUR_WEB_CLIENT_ID_HERE" app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java 2>/dev/null; then
        echo -e "${RED}2. Web Client ID 未配置${NC}"
        echo "   → 从 Firebase Console 获取 Web Client ID"
        echo "   → 替换 GoogleAuthManager.java 中的占位符"
        echo ""
    fi
    
    echo "详细修复步骤请查看："
    echo "  GOOGLE_LOGIN_FIX_URGENT.md"
fi

echo ""
echo -e "${BLUE}=========================================${NC}"
echo ""

# 退出码
if [ $ERRORS -gt 0 ]; then
    exit 1
else
    exit 0
fi

