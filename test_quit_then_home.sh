#!/bin/bash

echo "🔧 Quiz Quit后切换主页问题测试"
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
echo "   1. 进入Quiz模块"
echo "   2. 故意答错一道题"
echo "   3. 进入错误结果页"
echo "   4. 点击 'Quit Level'"
echo "   5. 返回到题目页面（第一题）"
echo "   6. 立即点击底部导航栏 → 主页"
echo "   7. 在主页等待10秒"
echo "   "
echo "   ❌ 问题重现：错误结果页会弹出"
echo "   ═══════════════════════════════════════"
echo ""
echo "🔍 监控日志中..."
echo "   关键词: QuizReviewLearn, countValueAnimator, timeStart, onResume"
echo "================================================"
echo ""

adb logcat | grep -E "QuizReviewLearn|QuestionFragment.*(Received result|timeStart|onResume|countValueAnimator|📌)" --line-buffered --color=always

