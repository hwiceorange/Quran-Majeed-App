#!/bin/bash

echo "=========================================="
echo "🧪 Google 登录测试脚本 - Release APK"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目路径
PROJECT_DIR="/Users/huwei_kt126.com/Documents/Quran-Majeed-App"
PACKAGE_NAME="com.quran.quranaudio.online"

# 步骤 1: 检查 SHA-1 配置
echo "【步骤 1/6】检查 SHA-1 配置"
echo "=========================================="
echo ""

cd "$PROJECT_DIR"
RELEASE_SHA1="19184387c863b6ac668633c7917d34c89ddf54f5"

if grep -q "$RELEASE_SHA1" app/google-services.json; then
    echo -e "${GREEN}✅ Release SHA-1 已在 google-services.json 中${NC}"
    echo ""
else
    echo -e "${RED}❌ Release SHA-1 未在 google-services.json 中${NC}"
    echo ""
    echo -e "${YELLOW}请先完成以下步骤：${NC}"
    echo "1. 在 Firebase Console 添加 SHA-1: $RELEASE_SHA1"
    echo "2. 下载新的 google-services.json"
    echo "3. 替换项目中的 app/google-services.json"
    echo ""
    echo "详细步骤请查看: GOOGLE_LOGIN_RELEASE_FIX_GUIDE.md"
    echo ""
    read -p "是否已完成上述步骤？(y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "请完成 Firebase 配置后再运行此脚本"
        exit 1
    fi
fi

# 步骤 2: 检查设备连接
echo "【步骤 2/6】检查设备连接"
echo "=========================================="
echo ""

if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ 未检测到 Android 设备${NC}"
    echo ""
    echo "请确保："
    echo "1. 设备已通过 USB 连接"
    echo "2. 已启用 USB 调试"
    echo "3. 已授权此电脑进行调试"
    exit 1
fi

DEVICE=$(adb devices | grep "device$" | head -1 | awk '{print $1}')
echo -e "${GREEN}✅ 设备已连接: $DEVICE${NC}"
echo ""

# 步骤 3: 检查 Release APK
echo "【步骤 3/6】检查 Release APK"
echo "=========================================="
echo ""

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
    echo -e "${YELLOW}⚠️ Release APK 不存在，开始编译...${NC}"
    echo ""
    
    cd "$PROJECT_DIR"
    ./gradlew clean
    ./gradlew :app:assembleRelease
    
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}❌ 编译失败${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✅ Release APK 已准备就绪${NC}"
echo "   路径: $APK_PATH"
echo ""

# 获取 APK 签名信息
echo "【APK 签名信息】"
APK_SHA1=$(unzip -p "$APK_PATH" META-INF/*.RSA 2>/dev/null | keytool -printcert 2>/dev/null | grep "SHA1:" | cut -d' ' -f3 | tr -d ':' | tr '[:upper:]' '[:lower:]')
echo "   SHA-1: $APK_SHA1"
echo ""

if [ "$APK_SHA1" != "$RELEASE_SHA1" ]; then
    echo -e "${RED}⚠️ 警告: APK 的 SHA-1 与预期不符${NC}"
    echo "   预期: $RELEASE_SHA1"
    echo "   实际: $APK_SHA1"
    echo ""
fi

# 步骤 4: 安装 APK
echo "【步骤 4/6】安装 Release APK"
echo "=========================================="
echo ""

# 先卸载旧版本
if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "卸载旧版本..."
    adb uninstall "$PACKAGE_NAME" 2>/dev/null
fi

echo "安装 Release APK..."
if adb install "$APK_PATH" 2>&1 | grep -q "Success"; then
    echo -e "${GREEN}✅ APK 安装成功${NC}"
    echo ""
else
    echo -e "${RED}❌ APK 安装失败${NC}"
    exit 1
fi

# 步骤 5: 检查 Google Play Services
echo "【步骤 5/6】检查 Google Play Services"
echo "=========================================="
echo ""

GMS_VERSION=$(adb shell dumpsys package com.google.android.gms | grep "versionName" | head -1 | cut -d'=' -f2)
echo "   版本: $GMS_VERSION"

if [ -n "$GMS_VERSION" ]; then
    echo -e "${GREEN}✅ Google Play Services 已安装${NC}"
else
    echo -e "${RED}⚠️ Google Play Services 未安装或版本过低${NC}"
fi
echo ""

# 步骤 6: 启动应用并监控日志
echo "【步骤 6/6】启动应用并监控登录日志"
echo "=========================================="
echo ""

echo "清除旧日志..."
adb logcat -c

echo "启动应用..."
adb shell am start -n "$PACKAGE_NAME/.prayertimes.ui.MainActivity"
sleep 2

echo ""
echo -e "${YELLOW}📱 请在设备上操作：${NC}"
echo "1. 进入需要登录的功能（如 Daily Quests）"
echo "2. 点击 Google 登录按钮"
echo "3. 选择 Google 账号"
echo ""
echo "正在监控登录日志..."
echo "按 Ctrl+C 停止监控"
echo ""
echo "=========================================="
echo ""

# 监控登录相关日志
adb logcat | grep --line-buffered -E "(GoogleAuthManager|GoogleSignIn|FirebaseAuth|SIGN_IN)" | while read line; do
    if echo "$line" | grep -q "success"; then
        echo -e "${GREEN}$line${NC}"
    elif echo "$line" | grep -q -i "fail\|error\|cancel"; then
        echo -e "${RED}$line${NC}"
    else
        echo "$line"
    fi
    
    # 检测登录成功
    if echo "$line" | grep -q "signInWithCredential:success"; then
        echo ""
        echo "=========================================="
        echo -e "${GREEN}🎉 Google 登录成功！${NC}"
        echo "=========================================="
        echo ""
        break
    fi
    
    # 检测登录失败
    if echo "$line" | grep -q "Status Code: 12501\|SIGN_IN_CANCELLED"; then
        echo ""
        echo "=========================================="
        echo -e "${RED}❌ Google 登录失败${NC}"
        echo "=========================================="
        echo ""
        echo "错误码: 12501 (SIGN_IN_CANCELLED)"
        echo ""
        echo -e "${YELLOW}可能的原因：${NC}"
        echo "1. SHA-1 指纹未在 Firebase 中注册"
        echo "2. Firebase 配置尚未生效（需要 5-10 分钟）"
        echo "3. google-services.json 未更新"
        echo ""
        echo -e "${YELLOW}解决方法：${NC}"
        echo "1. 确认已在 Firebase Console 添加 SHA-1: $RELEASE_SHA1"
        echo "2. 下载最新的 google-services.json"
        echo "3. 等待 5-10 分钟后重试"
        echo ""
        echo "详细步骤请查看: GOOGLE_LOGIN_RELEASE_FIX_GUIDE.md"
        echo ""
        break
    fi
done

echo ""
echo "=========================================="
echo "✅ 测试完成"
echo "=========================================="


