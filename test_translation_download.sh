#!/bin/bash

echo "🔧 翻译下载服务测试脚本（Android 14修复）"
echo "============================================"
echo ""

echo "📦 Step 1: 卸载旧版本..."
adb uninstall com.quran.quranaudio.online
sleep 1

echo ""
echo "📲 Step 2: 安装新APK..."
adb install app/build/outputs/apk/debug/app-debug.apk
sleep 2

echo ""
echo "🧹 Step 3: 清空日志..."
adb logcat -c
sleep 1

echo ""
echo "✅ 安装完成！"
echo ""
echo "📱 请在手机上进行以下操作:"
echo "   1. 打开应用"
echo "   2. 在多语言页选择 **印尼语 (Indonesian)**"
echo "   3. 点击 Continue"
echo "   4. 选择一个翻译版本"
echo "   5. 点击 Continue"
echo ""
echo "📊 监控日志中..."
echo "   关键词: TranslationDownloadService, FATAL, Exception"
echo "============================================"
echo ""

adb logcat | grep -E "TranslationDownloadService|RecitationChapterDownloadService|KFQPCScriptFontsDownloadService|FATAL|MissingForegroundServiceTypeException" --line-buffered

