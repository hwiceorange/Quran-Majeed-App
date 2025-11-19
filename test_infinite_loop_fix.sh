#!/bin/bash

echo "🔧 Quiz无限循环问题修复验证"
echo "================================================"
echo ""

echo "📦 Step 1: 卸载旧版本..."
adb uninstall com.quran.quranaudio.online
sleep 1

echo ""
echo "📲 Step 2: 安装新版本..."
adb install app/build/outputs/apk/debug/app-debug.apk
sleep 2

echo ""
echo "🧹 Step 3: 清空日志..."
adb logcat -c
sleep 1

echo ""
echo "✅ 准备完成！"
echo ""
echo "📱 请在手机上进行以下测试:"
echo "   ═══════════════════════════════════════"
echo "   测试1: 单一错误页面"
echo "   1. 进入Quiz模块"
echo "   2. 故意答错一道题"
echo "   3. 观察错误结果页"
echo "   "
echo "   ✅ 预期: 只显示一个错误页面"
echo "   ❌ 不应: 出现第二个页面覆盖"
echo "   ❌ 不应: 页面闪现或叠加"
echo "   ═══════════════════════════════════════"
echo ""
echo "   测试2: Quit功能（最重要！）"
echo "   1. 在错误页面点击 'Quit Level'"
echo "   2. 返回Quiz第一题"
echo "   3. 点击底部导航进入主页"
echo "   4. 在主页等待至少10秒"
echo "   "
echo "   ✅ 预期: 主页正常，不再弹出错误页"
echo "   ❌ 不应: 错误页面自动弹出（无限循环）"
echo "   ═══════════════════════════════════════"
echo ""
echo "   测试3: Skip功能"
echo "   1. 答错题目进入错误页"
echo "   2. 点击 'Skip'"
echo "   3. 观看激励视频"
echo "   4. 观察广告结束后的行为"
echo "   "
echo "   ✅ 预期: 自动跳转到下一题"
echo "   ❌ 不应: 停留在错误页面"
echo "   ═══════════════════════════════════════"
echo ""
echo "🔍 监控日志中..."
echo "   关键词: QuizReviewLearn, Received result, hasNavigatedToReview"
echo "================================================"
echo ""

adb logcat | grep -E "QuizReviewLearn|QuestionFragment.*Received result|hasNavigatedToReview|already navigated" --line-buffered --color=always

