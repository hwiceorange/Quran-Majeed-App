#!/bin/bash

echo "🔧 Quiz Skip功能测试脚本"
echo "================================"
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
echo "   1. 打开Quiz模块"
echo "   2. 故意答错一题"
echo "   3. 在错误结果页点击 Skip"
echo "   4. 看完激励视频广告"
echo "   5. 观察是否自动跳转到下一题"
echo ""
echo "📊 监控日志中..."
echo "   关键词: 🎁 广告完成, 📬 接收结果, Skip"
echo "================================"
echo ""

adb logcat | grep -E "QuizReviewLearn|QuestionFragment|📬|🎁|SKIP"
