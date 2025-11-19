#!/bin/bash

echo "🔧 Quiz单一错误页面修复验证"
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
echo "🔍 Step 3: 验证Activity注册..."
echo "================================================"
adb shell dumpsys package com.quran.quranaudio.online | grep -E "QuizReviewLearn|QuranQuestionFail|QuranQuestionRevival" | head -10
echo "================================================"
echo ""
echo "✅ 应该只看到: QuizReviewLearnActivity"
echo "❌ 不应看到: QuranQuestionFailActivity"
echo "❌ 不应看到: QuranQuestionRevivalActivity"
echo ""

echo "🧹 Step 4: 清空日志..."
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
echo "   ❌ 不应出现: 第二个页面覆盖"
echo "   ❌ 不应出现: 8秒倒计时页面"
echo "   ═══════════════════════════════════════"
echo ""
echo "   测试2: Skip功能"
echo "   1. 在错误页面点击 'Skip'"
echo "   2. 观看激励视频广告"
echo "   3. 观察广告结束后的行为"
echo "   "
echo "   ✅ 预期: 自动跳转到下一题"
echo "   ❌ 不应: 停留在错误页面"
echo "   ═══════════════════════════════════════"
echo ""
echo "   测试3: 离开后不再弹出（重要！）"
echo "   1. 在错误页面点击 'Quit Level'"
echo "   2. 点击底部导航进入主页"
echo "   3. 等待10秒"
echo "   "
echo "   ✅ 预期: 主页正常显示，不再弹出错误页"
echo "   ❌ 不应: 错误页面自动弹出"
echo "   ═══════════════════════════════════════"
echo ""
echo "🔍 监控日志中..."
echo "   关键词: QuizReviewLearn, QuranQuestionFail, RxBus"
echo "================================================"
echo ""

adb logcat | grep -E "QuizReviewLearn|QuranQuestionFail|QuranQuestionRevival|RxBus.*TRY_AGAIN" --line-buffered --color=always

