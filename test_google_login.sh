#!/bin/bash

echo "========================================"
echo "Google 登录测试脚本"
echo "========================================"
echo ""

# 清空日志
adb logcat -c

echo "📱 重启应用..."
adb shell am force-stop com.quran.quranaudio.online
sleep 1
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity

echo ""
echo "⏳ 等待应用启动（10秒）..."
echo "   请在设备上尝试 Google 登录..."
echo ""

sleep 10

echo "📋 分析登录日志..."
echo ""

# 检查登录相关日志
LOGIN_LOGS=$(adb logcat -d | grep -E "(GoogleAuth|Sign-in|StatusCode|12501|10)")

if echo "$LOGIN_LOGS" | grep -q "12501"; then
    echo "❌ 错误：发现 StatusCode 12501 (Sign-in canceled)"
    echo ""
    echo "可能原因："
    echo "1. google-services.json 未更新"
    echo "2. Firebase 中未添加 Debug SHA-1"
    echo "3. 应用未重新编译"
    echo ""
    echo "详细日志："
    echo "$LOGIN_LOGS" | grep -A 5 -B 5 "12501"
    
elif echo "$LOGIN_LOGS" | grep -q "signInWithCredential:success"; then
    echo "✅ 登录成功！"
    echo ""
    echo "已成功使用 Google 登录"
    
elif echo "$LOGIN_LOGS" | grep -q "ID Token: Present"; then
    echo "✅ ID Token 获取成功"
    echo ""
    echo "Google 登录流程正常"
    
else
    echo "⚠️  未检测到登录尝试"
    echo ""
    echo "请确认："
    echo "1. 是否已尝试在设备上点击 Google 登录？"
    echo "2. 应用是否正在运行？"
    echo ""
    echo "所有 Google Auth 相关日志："
    echo "$LOGIN_LOGS"
fi

echo ""
echo "========================================"
echo "完整登录日志"
echo "========================================"
adb logcat -d | grep -E "(GoogleAuth|FirebaseAuth)" | tail -30

echo ""
echo "========================================"
echo "测试完成"
echo "========================================"

