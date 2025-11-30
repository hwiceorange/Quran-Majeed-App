#!/bin/bash

# 快速编译和安装脚本

echo "🔨 编译应用..."
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ 编译成功！"
    echo ""
    echo "📲 安装到设备..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ 安装成功！"
        echo ""
        echo "🗑️  清除应用数据（模拟新用户）..."
        adb shell pm clear com.quran.quranaudio.online
        echo ""
        echo "📊 启动日志监控..."
        echo "请在设备上操作，然后查看日志输出。"
        echo "按 Ctrl+C 停止监控。"
        echo ""
        echo "-----------------------------------"
        adb logcat -c
        adb logcat | grep -E "FragOnboardQuranVersion|QuranTranslationFactory|TranslUtils"
    else
        echo "❌ 安装失败"
    fi
else
    echo "❌ 编译失败"
fi
