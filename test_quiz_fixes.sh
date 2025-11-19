#!/bin/bash

# Quiz 4个问题修复 - 测试脚本
# 2025-11-18

echo "🎮 Quiz模块修复测试脚本"
echo "========================"
echo ""
echo "📋 修复内容："
echo "  1. ✅ 答案随机化 - 选项不再总是A正确"
echo "  2. ✅ 经文翻译显示 - 完全重写，本地0延迟加载"
echo "  3. ✅ Skip功能完整 - 跳转逻辑正确"
echo "  4. ✅ 页面叠压修复 - 回退只需1次"
echo "  5. ✅ Quran初始化 - 自动检查并初始化"
echo ""

# 检查设备连接
if ! adb devices | grep -q "device$"; then
    echo "❌ 错误：未检测到Android设备"
    echo "   请确保设备已连接并开启USB调试"
    exit 1
fi

echo "✅ 检测到Android设备"
echo ""

# 卸载旧版本
echo "🗑️  卸载旧版本..."
adb uninstall com.quran.quranaudio.online 2>/dev/null

# 安装新版本
echo "📦 安装新版本..."
cd /Users/huwei/AndroidStudioProjects/quran0
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ 错误：APK文件不存在"
    echo "   路径: $APK_PATH"
    echo "   请先运行: ./gradlew :app:assembleDebug"
    exit 1
fi

adb install "$APK_PATH"

if [ $? -ne 0 ]; then
    echo "❌ APK安装失败"
    exit 1
fi

echo "✅ APK安装成功"
echo ""

# 清空日志
adb logcat -c

echo "📱 启动应用测试"
echo "==============="
echo ""
echo "请按照以下步骤测试："
echo ""
echo "【测试 1: 答案随机化】"
echo "  1. 在应用中进入Quiz模块"
echo "  2. 观察多道题目"
echo "  3. 验证正确答案位置是否随机（不总是A）"
echo ""
echo "【测试 2: 经文翻译】"
echo "  1. 确保应用语言为英语"
echo "  2. 答错一道题"
echo "  3. 查看经文卡片中是否显示英语翻译"
echo "  4. 如果没有，检查是否已下载英语古兰经翻译"
echo ""
echo "【测试 3: Skip功能】"
echo "  1. 答错一道题，进入Review页面"
echo "  2. 点击Skip按钮"
echo "  3. 观看激励视频广告"
echo "  4. 验证是否正确跳转到下一题或升级页面"
echo ""
echo "【测试 4: 页面叠压】"
echo "  1. 在倒计时快结束时（最后2-3秒）故意选错"
echo "  2. 观察是否只打开1个Review页面"
echo "  3. 点击顶部返回箭头，应该只需点1次"
echo ""
echo "========================"
echo "🔍 实时监控日志（按Ctrl+C停止）："
echo ""

# 实时显示相关日志
adb logcat | grep -E "QuestionTools|VerseLoaderHelper|QuizReview|QuranQuestionFragment" --color=always

