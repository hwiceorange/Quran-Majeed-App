#!/bin/bash

# Firebase 签名配置验证脚本

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}🔍 Firebase 签名配置完整验证${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

ERRORS=0
WARNINGS=0

# 1. Debug SHA-1
echo -e "${BLUE}[1/6] 检查 Debug SHA-1 指纹...${NC}"
DEBUG_SHA1=$(keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep "SHA1:" | awk '{print $2}')
if [ -n "$DEBUG_SHA1" ]; then
    echo -e "${GREEN}✅ Debug SHA-1：${NC}"
    echo "   $DEBUG_SHA1"
    echo ""
    echo -e "${YELLOW}⚠️  请确保此 SHA-1 已添加到 Firebase Console！${NC}"
    echo "   https://console.firebase.google.com/"
    echo "   → Project Settings → Your apps → Add fingerprint"
else
    echo -e "${RED}❌ 错误：无法获取 Debug SHA-1${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 2. 包名验证
echo -e "${BLUE}[2/6] 验证包名一致性...${NC}"
BUILD_GRADLE_PKG=$(grep "applicationId" app/build.gradle | grep -oE '"[^"]+"' | tr -d '"')
GOOGLE_SERVICES_PKG=$(grep "package_name" app/google-services.json | grep -oE '"[^"]+"' | tr -d '"')

echo "build.gradle:         $BUILD_GRADLE_PKG"
echo "google-services.json: $GOOGLE_SERVICES_PKG"

if [ "$BUILD_GRADLE_PKG" = "$GOOGLE_SERVICES_PKG" ]; then
    echo -e "${GREEN}✅ 包名匹配正确${NC}"
else
    echo -e "${RED}❌ 错误：包名不匹配！${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 3. OAuth 客户端配置
echo -e "${BLUE}[3/6] 检查 OAuth 客户端配置...${NC}"
if grep -q '"oauth_client": \[\]' app/google-services.json; then
    echo -e "${RED}❌ 严重错误：oauth_client 是空数组！${NC}"
    echo ""
    echo -e "${YELLOW}这是 'Sign-in canceled' 的根本原因！${NC}"
    echo ""
    echo "修复步骤："
    echo "  1. 访问 Firebase Console"
    echo "  2. 添加 Debug SHA-1: $DEBUG_SHA1"
    echo "  3. 添加 Play Store SHA-1（从 Google Play Console 获取）"
    echo "  4. 启用 Google Sign-In"
    echo "  5. 下载新的 google-services.json"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ oauth_client 已配置${NC}"
    echo ""
    echo "OAuth 客户端："
    cat app/google-services.json | grep -A 5 "client_id" | head -10
fi
echo ""

# 4. Web Client ID 配置
echo -e "${BLUE}[4/6] 检查 Web Client ID 配置...${NC}"
if [ -f "app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java" ]; then
    if grep -q "YOUR_WEB_CLIENT_ID_HERE" app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java; then
        echo -e "${RED}❌ 错误：Web Client ID 未配置（仍是占位符）${NC}"
        echo ""
        echo "修复步骤："
        echo "  1. 从 Firebase Console 获取 Web Client ID"
        echo "  2. 打开 GoogleAuthManager.java（第 46 行）"
        echo "  3. 替换 YOUR_WEB_CLIENT_ID_HERE"
        ERRORS=$((ERRORS + 1))
    else
        WEB_CLIENT_ID=$(grep "requestIdToken" app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java | grep -oE '"[^"]+"' | head -1)
        echo -e "${GREEN}✅ Web Client ID 已配置${NC}"
        echo "   Client ID: $WEB_CLIENT_ID"
    fi
else
    echo -e "${RED}❌ 错误：GoogleAuthManager.java 文件不存在${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 5. 设备连接检查
echo -e "${BLUE}[5/6] 检查设备连接...${NC}"
if command -v adb &> /dev/null; then
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l | xargs)
    if [ "$DEVICE_COUNT" -gt 0 ]; then
        DEVICE_NAME=$(adb shell getprop ro.product.model 2>/dev/null)
        ANDROID_VERSION=$(adb shell getprop ro.build.version.release 2>/dev/null)
        echo -e "${GREEN}✅ 设备已连接：$DEVICE_NAME (Android $ANDROID_VERSION)${NC}"
    else
        echo -e "${YELLOW}⚠️  警告：未检测到设备${NC}"
        echo "   请连接设备后再测试 Google 登录"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  警告：ADB 未找到${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 6. 应用安装状态
echo -e "${BLUE}[6/6] 检查应用安装状态...${NC}"
if command -v adb &> /dev/null && [ "$DEVICE_COUNT" -gt 0 ]; then
    if adb shell pm list packages | grep -q "com.quran.quranaudio.online"; then
        echo -e "${GREEN}✅ 应用已安装${NC}"
        
        # 获取应用版本
        APP_VERSION=$(adb shell dumpsys package com.quran.quranaudio.online | grep "versionName" | head -1 | cut -d'=' -f2)
        if [ -n "$APP_VERSION" ]; then
            echo "   版本：$APP_VERSION"
        fi
    else
        echo -e "${YELLOW}⚠️  应用未安装${NC}"
        echo "   请运行：./gradlew installDebug"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  跳过（设备未连接）${NC}"
fi
echo ""

# 总结
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}📊 验证总结${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ 所有检查通过！${NC}"
    echo ""
    echo "可以在设备上测试 Google 登录了。"
    echo ""
    echo "测试步骤："
    echo "  1. 启动应用"
    echo "  2. 点击右上角头像图标"
    echo "  3. 选择 Google 账户"
    echo "  4. 应该成功登录"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  发现 $WARNINGS 个警告${NC}"
    echo ""
    echo "配置基本正确，可以测试登录功能。"
else
    echo -e "${RED}❌ 发现 $ERRORS 个错误，$WARNINGS 个警告${NC}"
    echo ""
    echo -e "${YELLOW}🔧 必须修复的问题：${NC}"
    echo ""
    
    # 根据错误给出具体建议
    if grep -q '"oauth_client": \[\]' app/google-services.json 2>/dev/null; then
        echo -e "${RED}1. oauth_client 为空（最严重）${NC}"
        echo "   修复方案："
        echo "   ① 访问 Firebase Console: https://console.firebase.google.com/"
        echo "   ② 选择项目：quran-majeed-aa3d2"
        echo "   ③ Project Settings → Your apps → Add fingerprint"
        echo "   ④ 添加 Debug SHA-1: $DEBUG_SHA1"
        echo "   ⑤ 从 Google Play Console 获取 Play Store SHA-1 并添加"
        echo "   ⑥ Authentication → Sign-in method → Google → Enable"
        echo "   ⑦ 下载新的 google-services.json"
        echo "   ⑧ 替换 app/google-services.json"
        echo ""
    fi
    
    if grep -q "YOUR_WEB_CLIENT_ID_HERE" app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java 2>/dev/null; then
        echo -e "${RED}2. Web Client ID 未配置${NC}"
        echo "   修复方案："
        echo "   ① Firebase Console → Project Settings → General"
        echo "   ② 找到 'Web client (auto created by Google Service)'"
        echo "   ③ 复制 Web Client ID"
        echo "   ④ 打开 GoogleAuthManager.java（第 46 行）"
        echo "   ⑤ 替换 YOUR_WEB_CLIENT_ID_HERE 为实际 ID"
        echo ""
    fi
    
    echo "详细修复步骤请查看："
    echo "  → FIREBASE_SIGNATURE_FIX.md"
fi

echo ""

# Play Store SHA-1 提醒
if [ $ERRORS -gt 0 ]; then
    echo -e "${BLUE}=========================================${NC}"
    echo -e "${BLUE}⭐ 重要提醒：Play Store SHA-1${NC}"
    echo -e "${BLUE}=========================================${NC}"
    echo ""
    echo "如果您的应用使用 Google Play App Signing："
    echo ""
    echo "1. 访问 Google Play Console:"
    echo "   https://play.google.com/console/"
    echo ""
    echo "2. 选择您的应用 → App Integrity → App signing"
    echo ""
    echo "3. 复制 'App signing key certificate' 下的 SHA-1"
    echo ""
    echo "4. 将此 SHA-1 也添加到 Firebase Console"
    echo ""
    echo -e "${YELLOW}⚠️  线上版本需要 Play Store SHA-1 才能登录！${NC}"
    echo ""
fi

echo -e "${BLUE}=========================================${NC}"
echo ""

# 退出码
if [ $ERRORS -gt 0 ]; then
    exit 1
else
    exit 0
fi

