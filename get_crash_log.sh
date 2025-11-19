#!/bin/bash

echo "🔍 准备捕获崩溃日志..."
echo "================================"
echo ""
echo "📱 请执行以下步骤："
echo "   1. 在多语言页选择一个非英语、非阿语的语言（如印尼语）"
echo "   2. 点击下一步"
echo "   3. 在引导页点击'选择古兰经翻译版本'"
echo ""
echo "⏳ 等待崩溃发生..."
echo ""

# 清空日志
adb logcat -c
sleep 1

# 监控崩溃日志
adb logcat | grep -E "FATAL|AndroidRuntime|Exception|Crash|FragOnboard|LoadTranslsTask|QuranTranslationFactory" --line-buffered

