#!/bin/bash

echo "🔧 Quiz模块完全清理安装测试"
echo "================================================"
echo ""

echo "🧹 Step 1: 完全卸载旧应用..."
adb uninstall com.quran.quranaudio.online
if [ $? -eq 0 ]; then
    echo "   ✅ 卸载成功"
else
    echo "   ⚠️  应用可能未安装"
fi
sleep 2

echo ""
echo "🔍 Step 2: 确认应用已完全卸载..."
adb shell pm list packages | grep com.quran.quranaudio.online
if [ $? -eq 0 ]; then
    echo "   ❌ 应用仍然存在！"
    exit 1
else
    echo "   ✅ 应用已完全卸载"
fi

echo ""
echo "📲 Step 3: 安装全新APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk
if [ $? -eq 0 ]; then
    echo "   ✅ 安装成功"
else
    echo "   ❌ 安装失败！"
    exit 1
fi
sleep 2

echo ""
echo "🔍 Step 4: 验证APK版本..."
adb shell dumpsys package com.quran.quranaudio.online | grep versionCode
echo ""

echo "🧹 Step 5: 清空日志..."
adb logcat -c
sleep 1

echo ""
echo "✅ 准备完成！现在是全新安装状态。"
echo ""
echo "📱 请在手机上进行以下测试:"
echo "   ═══════════════════════════════════════"
echo "   🔴 关键测试：全新安装首次运行"
echo "   1. 打开应用（首次启动）"
echo "   2. 完成引导流程"
echo "   3. 进入Quiz模块"
echo "   4. 故意答错一道题"
echo "   5. 观察错误结果页"
echo "   "
echo "   ✅ 预期: 只显示一个错误页面"
echo "   ❌ 不应: 出现两个错误页面"
echo "   ═══════════════════════════════════════"
echo ""
echo "   测试步骤详细说明："
echo "   "
echo "   A. 观察错误页面数量"
echo "      - 答错后，只应看到1个错误页面"
echo "      - 页面应该稳定显示，不闪烁"
echo "      - 不应该有第二个页面覆盖"
echo "   "
echo "   B. 测试Quit功能"
echo "      - 点击 'Quit Level'"
echo "      - 返回Quiz第一题"
echo "      - 进入主页"
echo "      - 等待10秒"
echo "      - 确认不会再弹出错误页"
echo "   "
echo "   C. 测试Skip功能"
echo "      - 点击 'Skip'"
echo "      - 观看激励广告"
echo "      - 确认跳转到下一题"
echo "   ═══════════════════════════════════════"
echo ""
echo "🔍 开始监控日志..."
echo "   查找: QuizReviewLearn, QuranQuestionFail, QuranQuestionRevival"
echo "================================================"
echo ""

# 监控关键日志
adb logcat | grep -E "QuizReviewLearn|QuranQuestionFail|QuranQuestionRevival|QuestionFragment.*Received|Activity.*com.quran.quranaudio.quiz.activity" --line-buffered --color=always

