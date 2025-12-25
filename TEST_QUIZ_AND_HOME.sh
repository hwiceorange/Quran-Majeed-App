#!/bin/bash

echo "═══════════════════════════════════════════════"
echo "🎯 测试Quiz和主页原生广告"
echo "═══════════════════════════════════════════════"
echo ""

echo "1️⃣ 清空日志..."
adb logcat -c
echo "✅ 日志已清空"
echo ""

echo "📝 开始记录日志..."
echo "日志将保存到: quiz_home_native_ads.txt"
echo ""
echo "请按照以下步骤测试："
echo ""
echo "【测试1】Quiz答题结果页"
echo "  1. 在Android Studio重新编译运行"
echo "  2. 跳过onboarding，进入主页"
echo "  3. 点击 Quiz 或 Review & Learn"
echo "  4. 开始答题（随便答）"
echo "  5. 完成答题，查看结果页底部"
echo "  ✅ 应该看到原生广告"
echo ""
echo "【测试2】主页 Verse of the Day"
echo "  1. 返回主页"
echo "  2. 向下滚动到 Verse of the Day 卡片"
echo "  3. 查看卡片底部"
echo "  ✅ 应该看到原生广告"
echo ""
echo "═══════════════════════════════════════════════"
echo "📊 开始记录日志..."
echo "按 Ctrl+C 停止监控"
echo "═══════════════════════════════════════════════"
echo ""

adb logcat | grep -E "NATIVE_AD_TRACK|QuizReviewLearnActivity|AdNativeSmallWrapperView|HomeFragment.*VOTD|votdNativeAdContainer" | tee quiz_home_native_ads.txt

