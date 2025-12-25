#!/bin/bash

echo "═══════════════════════════════════════════════"
echo "🎯 原生广告完整测试脚本"
echo "═══════════════════════════════════════════════"
echo ""

echo "1️⃣ 清空日志..."
adb logcat -c
echo "✅ 日志已清空"
echo ""

echo "2️⃣ 启动日志监控（保存到文件）..."
echo "📝 日志将保存到: native_ads_all_positions.txt"
echo ""
echo "请按照以下步骤测试："
echo ""
echo "【测试1】Quiz答题结果页"
echo "  1. 在Android Studio运行应用"
echo "  2. 进入Quiz"
echo "  3. 完成答题"
echo "  4. 查看结果页底部"
echo "  ✅ 应该看到原生广告"
echo ""
echo "【测试2】多语言选择页"
echo "  1. 在另一个终端运行: adb uninstall com.quran.quranaudio.online"
echo "  2. 重新运行应用"
echo "  3. 在多语言页向下滚动"
echo "  ✅ 应该看到底部原生广告"
echo ""
echo "【测试3】主页 Verse of the Day"
echo "  1. 进入主页"
echo "  2. 向下滚动到Verse of the Day卡片"
echo "  ✅ 应该看到卡片底部原生广告"
echo ""
echo "═══════════════════════════════════════════════"
echo "📊 开始记录日志..."
echo "按 Ctrl+C 停止监控"
echo "═══════════════════════════════════════════════"
echo ""

adb logcat | grep -E "NATIVE_AD_TRACK|DIAGNOSE.*Native|HomeFragment.*VOTD|FragOnboard.*Native|AdNativeSmallWrapperView" | tee native_ads_all_positions.txt

