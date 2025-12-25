#!/bin/bash
# 获取原生广告详细日志

echo "======================================"
echo "🔍 原生广告追踪日志收集工具"
echo "======================================"
echo ""

# 清空日志
echo "→ 清空旧日志..."
adb logcat -c
echo "✅ 日志已清空"
echo ""

echo "📝 开始记录原生广告日志..."
echo "请在Android Studio运行应用，然后："
echo "  1. 进入多语言页面（查看底部原生广告）"
echo "  2. 进入主页（查看Verse of the Day原生广告）"
echo "  3. 进入Quiz答题结果页（查看原生广告）"
echo "  4. 按 Ctrl+C 停止日志记录"
echo ""
echo "======================================"
echo ""

# 实时显示日志并保存
adb logcat | grep -E "NATIVE_AD_TRACK|NativeAdManager|NativeAdHelper|DIAGNOSE.*Native" | tee native_ad_detailed_log.txt

echo ""
echo "======================================"
echo "✅ 日志已保存到: native_ad_detailed_log.txt"
echo "======================================"

