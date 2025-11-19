#!/bin/bash

# ========================================
# Tafsir API 完整测试脚本
# ========================================

set -e  # 遇到错误立即退出

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║         🚀 Tafsir API 完整编译和测试                        ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ========================================
# 步骤 1: 测试服务器 API
# ========================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📡 步骤 1: 测试服务器 API"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "🔍 测试 Tafsir 清单 API..."
MANIFEST_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json)
if [ "$MANIFEST_RESPONSE" == "200" ]; then
    echo "✅ Tafsir 清单 API 可访问 (HTTP $MANIFEST_RESPONSE)"
else
    echo "⚠️  Tafsir 清单 API 响应异常 (HTTP $MANIFEST_RESPONSE)"
fi
echo ""

echo "🔍 测试印尼语 Tafsir 内容 API..."
TAFSIR_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1)
if [ "$TAFSIR_RESPONSE" == "200" ]; then
    echo "✅ 印尼语 Tafsir API 可访问 (HTTP $TAFSIR_RESPONSE)"
    echo ""
    echo "📄 API 响应内容："
    curl -s https://apis.dochubai.com/quran/api/v4/tafsirs/id-tafsir-kemenag/by_ayah/1:1 | head -20
else
    echo "❌ 印尼语 Tafsir API 不可访问 (HTTP $TAFSIR_RESPONSE)"
    echo ""
    echo "⚠️  警告：服务器 API 尚未配置！"
    echo "   请按照 COMPLETE_TAFSIR_API_DEPLOYMENT_GUIDE.md 配置服务器"
    echo ""
    echo "   您可以选择："
    echo "   1. 按 Ctrl+C 退出，先配置服务器"
    echo "   2. 按 Enter 继续编译应用（印尼语 Tafsir 将无法加载）"
    read -p "   请选择: " choice
fi
echo ""

# ========================================
# 步骤 2: Clean 构建
# ========================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧹 步骤 2: Clean 构建"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

./gradlew clean
echo "✅ Clean 完成"
echo ""

# ========================================
# 步骤 3: 编译 Debug APK
# ========================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔨 步骤 3: 编译 Debug APK"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

./gradlew :app:assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "✅ APK 编译成功: $APK_PATH"
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "   APK 大小: $APK_SIZE"
else
    echo "❌ APK 编译失败！"
    exit 1
fi
echo ""

# ========================================
# 步骤 4: 卸载旧版本
# ========================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📱 步骤 4: 卸载旧版本"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

adb uninstall com.quran.quranaudio.online 2>/dev/null || echo "   (未安装旧版本)"
echo "✅ 卸载完成"
echo ""

# ========================================
# 步骤 5: 安装新版本
# ========================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📲 步骤 5: 安装新版本"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

adb install -r "$APK_PATH"
echo "✅ 安装完成"
echo ""

# ========================================
# 步骤 6: 启动应用
# ========================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 步骤 6: 启动应用"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
echo "✅ 应用已启动"
echo ""

# ========================================
# 步骤 7: 监控日志
# ========================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 步骤 7: 监控日志"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔍 监控以下关键日志："
echo "   - TafsirManager: Tafsir 清单加载"
echo "   - ActivityTafsir: Tafsir 内容加载"
echo "   - API_REQUEST/API_RESPONSE: API 请求/响应"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

sleep 2

echo "开始监控日志（按 Ctrl+C 停止）..."
echo ""

adb logcat | grep -E "TafsirManager|ActivityTafsir|API_REQUEST|API_RESPONSE|MainActivity.*Tafsir" --line-buffered --color=always

