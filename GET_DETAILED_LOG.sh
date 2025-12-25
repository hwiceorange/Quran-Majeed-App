#!/bin/bash
# 获取详细崩溃和广告日志

echo "======================================"
echo "🔍 详细日志收集工具"
echo "======================================"
echo ""

# 清空日志
echo "→ 清空旧日志..."
adb logcat -c
echo "✅ 日志已清空"
echo ""

echo "📝 开始记录日志..."
echo "请在Android Studio运行应用，然后："
echo "  1. 卸载应用: adb uninstall com.quran.quranaudio.online"
echo "  2. 重新安装运行（触发首次启动）"
echo "  3. 进入多语言选择页面"
echo "  4. 等待广告展示或失败"
echo "  5. 按 Ctrl+C 停止日志记录"
echo ""
echo "======================================"
echo ""

# 实时显示日志并保存
adb logcat | grep -E "DIAGNOSE|DIAGNOSE_ERROR|FATAL|FragOnboardLanguage|NativeAdHelper|NativeAdManager|ActivityOnboarding" | tee detailed_ad_log.txt

echo ""
echo "======================================"
echo "✅ 日志已保存到: detailed_ad_log.txt"
echo "======================================"

