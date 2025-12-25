#!/bin/bash
# 自动清理并捕获崩溃日志

echo "======================================"
echo "🔍 崩溃日志诊断工具"
echo "======================================"
echo ""

# 检查设备连接
echo "→ 检查设备连接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 未检测到设备，请确保："
    echo "   1. 设备已通过USB连接"
    echo "   2. 已启用USB调试"
    echo "   3. 已授权此电脑"
    exit 1
fi
echo "✅ 设备已连接"
echo ""

# 清空旧日志
echo "→ 清空旧日志..."
adb logcat -c
echo "✅ 日志已清空"
echo ""

# 卸载旧版本（可选）
echo "→ 卸载旧版本（如果存在）..."
adb uninstall com.quran.quranaudio.online 2>/dev/null
echo "✅ 旧版本已卸载"
echo ""

# 安装新APK
echo "→ 安装新APK..."
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    adb install -r app/build/outputs/apk/release/app-release.apk
    if [ $? -ne 0 ]; then
        echo "❌ APK安装失败"
        exit 1
    fi
    echo "✅ APK安装成功"
else
    echo "❌ 找不到APK文件: app/build/outputs/apk/release/app-release.apk"
    exit 1
fi
echo ""

# 启动日志记录（后台）
echo "→ 开始记录日志..."
adb logcat > crash_log_full.txt &
LOGCAT_PID=$!
echo "✅ 日志记录已启动 (PID: $LOGCAT_PID)"
echo ""

# 等待2秒确保logcat启动
sleep 2

# 启动应用
echo "→ 启动应用..."
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
echo "✅ 应用已启动"
echo ""

# 等待15秒观察是否崩溃
echo "⏳ 等待15秒观察崩溃..."
for i in {15..1}; do
    echo -ne "\r   剩余 $i 秒..."
    sleep 1
done
echo ""
echo ""

# 停止日志记录
echo "→ 停止日志记录..."
kill $LOGCAT_PID 2>/dev/null
wait $LOGCAT_PID 2>/dev/null
echo "✅ 日志记录已停止"
echo ""

# 提取诊断日志
echo "======================================"
echo "📊 诊断日志分析"
echo "======================================"
echo ""

# 提取DIAGNOSE日志
if grep -q "DIAGNOSE" crash_log_full.txt; then
    echo "✅ 检测到诊断日志"
    echo ""
    echo "🔍 关键步骤执行情况："
    echo "--------------------------------------"
    grep "DIAGNOSE" crash_log_full.txt > debug_report.txt
    cat debug_report.txt
    echo "--------------------------------------"
    echo ""
else
    echo "⚠️ 未检测到诊断日志（可能在日志记录前就崩溃了）"
    echo ""
fi

# 检查是否有崩溃
if grep -q "FATAL EXCEPTION" crash_log_full.txt; then
    echo "❌ 检测到崩溃！"
    echo ""
    echo "🔴 崩溃堆栈："
    echo "--------------------------------------"
    grep -A 100 "FATAL EXCEPTION" crash_log_full.txt | head -100 >> debug_report.txt
    grep -A 100 "FATAL EXCEPTION" crash_log_full.txt | head -100
    echo "--------------------------------------"
    echo ""
elif grep -q "AndroidRuntime.*FATAL" crash_log_full.txt; then
    echo "❌ 检测到运行时错误！"
    echo ""
    echo "🔴 错误信息："
    echo "--------------------------------------"
    grep -A 50 "AndroidRuntime.*FATAL" crash_log_full.txt | head -50 >> debug_report.txt
    grep -A 50 "AndroidRuntime.*FATAL" crash_log_full.txt | head -50
    echo "--------------------------------------"
    echo ""
else
    echo "✅ 未检测到崩溃"
    echo ""
    echo "📝 最近的应用日志："
    echo "--------------------------------------"
    grep -E "DIAGNOSE|App:|ActivitySplash:" crash_log_full.txt | tail -30 >> debug_report.txt
    grep -E "DIAGNOSE|App:|ActivitySplash:" crash_log_full.txt | tail -30
    echo "--------------------------------------"
    echo ""
fi

# 提取Caused by信息
if grep -q "Caused by:" crash_log_full.txt; then
    echo "🔍 根本原因 (Caused by)："
    echo "--------------------------------------"
    grep -A 10 "Caused by:" crash_log_full.txt | head -20 >> debug_report.txt
    grep -A 10 "Caused by:" crash_log_full.txt | head -20
    echo "--------------------------------------"
    echo ""
fi

echo "======================================"
echo "📁 日志文件已生成："
echo "======================================"
echo "   - crash_log_full.txt (完整日志)"
echo "   - debug_report.txt (诊断摘要)"
echo ""
echo "💡 查看完整日志："
echo "   cat crash_log_full.txt"
echo ""
echo "💡 查看诊断摘要："
echo "   cat debug_report.txt"
echo ""
echo "======================================"

