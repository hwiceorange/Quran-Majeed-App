#!/bin/bash

# 🔍 崩溃数据收集脚本 - 收集所有关键日志用于分析

echo "╔════════════════════════════════════════════════════════════╗"
echo "║           🔍 崩溃数据收集 - 完整日志分析                   ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

PACKAGE_NAME="com.quran.quranaudio.online"
LOG_FILE="crash_analysis_$(date +%Y%m%d_%H%M%S).log"

echo "📱 检查设备连接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 没有检测到设备"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
SDK_VERSION=$(adb shell getprop ro.build.version.sdk)

echo "✅ 设备已连接"
echo "   机型: $DEVICE_MODEL"
echo "   Android: $ANDROID_VERSION (API $SDK_VERSION)"
echo ""

# 检查应用是否已安装
if ! adb shell pm list packages | grep -q $PACKAGE_NAME; then
    echo "❌ 应用未安装"
    echo "请先安装: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    exit 1
fi

echo "✅ 应用已安装"
echo ""

# 清除旧日志
echo "🧹 清除旧日志..."
adb logcat -c
sleep 1

# 强制停止应用
echo "🛑 停止应用..."
adb shell am force-stop $PACKAGE_NAME
sleep 1

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║              🚀 启动应用并收集日志                         ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║  收集内容：                                                ║"
echo "║  • Native crashes (libc, linker)                           ║"
echo "║  • Java exceptions (AndroidRuntime)                        ║"
echo "║  • All app logs (CRASH_DEBUG, CRASH_HANDLER)               ║"
echo "║  • System errors (System.err)                              ║"
echo "║                                                            ║"
echo "║  ⏱️  收集时长：30秒（请在此期间重现崩溃）                   ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# 启动日志收集（后台）
echo "📝 开始收集日志到文件: $LOG_FILE"
adb logcat -v time > "$LOG_FILE" &
LOGCAT_PID=$!

# 启动应用
echo "🚀 启动应用..."
adb shell am start -n "${PACKAGE_NAME}/.SplashScreenActivity"
echo ""

echo "⏱️  收集日志中（30秒）..."
echo "请按以下步骤操作："
echo "1. 等待应用启动"
echo "2. 进入多语言选择页面"
echo "3. 等待1-2秒，观察是否崩溃"
echo ""
echo "倒计时："

for i in {30..1}; do
    echo -ne "\r   剩余 $i 秒... "
    sleep 1
done
echo ""

# 停止日志收集
echo ""
echo "🛑 停止日志收集..."
kill $LOGCAT_PID 2>/dev/null

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                   📊 分析日志数据                          ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# 检查是否有崩溃
if grep -q "FATAL EXCEPTION" "$LOG_FILE"; then
    echo "❌ 检测到 Java 崩溃！"
    echo ""
    echo "═══════════════ FATAL EXCEPTION ═══════════════"
    grep -A 30 "FATAL EXCEPTION" "$LOG_FILE" | head -35
    echo "═══════════════════════════════════════════════"
fi

if grep -q "libc.*Fatal signal" "$LOG_FILE"; then
    echo "❌ 检测到 Native 崩溃！"
    echo ""
    echo "═══════════════ NATIVE CRASH ════════════════════"
    grep -A 20 "libc.*Fatal signal" "$LOG_FILE" | head -25
    echo "═══════════════════════════════════════════════"
fi

if grep -q "UnsatisfiedLinkError" "$LOG_FILE"; then
    echo "❌ 检测到 Native 库加载失败！"
    echo ""
    echo "═══════════════ UnsatisfiedLinkError ══════════════"
    grep -B 5 -A 10 "UnsatisfiedLinkError" "$LOG_FILE" | head -20
    echo "═══════════════════════════════════════════════"
fi

if grep -q "dlopen failed" "$LOG_FILE"; then
    echo "❌ 检测到 dlopen 失败（16KB对齐问题）！"
    echo ""
    echo "═══════════════ DLOPEN FAILED ════════════════════"
    grep -A 5 "dlopen failed" "$LOG_FILE" | head -10
    echo "═══════════════════════════════════════════════"
fi

# 检查具体的 .so 文件
echo ""
echo "🔍 检查问题 Native 库..."
PROBLEM_LIBS=$(grep -E "libapm|libtob|libbuffer|libEncryptor|libfile_lock|libnms|libIronSource|libis" "$LOG_FILE" | head -10)
if [ ! -z "$PROBLEM_LIBS" ]; then
    echo "发现以下可疑库："
    echo "$PROBLEM_LIBS"
else
    echo "✅ 未发现明确的问题库引用"
fi

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                  📁 日志文件已保存                         ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║  文件: $LOG_FILE"
echo "║  大小: $(ls -lh "$LOG_FILE" | awk '{print $5}')"
echo "║                                                            ║"
echo "║  关键标签搜索：                                            ║"
echo "║  • FATAL:     $(grep -c "FATAL EXCEPTION" "$LOG_FILE" || echo 0) 次"
echo "║  • Native:    $(grep -c "libc.*Fatal" "$LOG_FILE" || echo 0) 次"
echo "║  • dlopen:    $(grep -c "dlopen failed" "$LOG_FILE" || echo 0) 次"
echo "║  • UnsatisfiedLinkError: $(grep -c "UnsatisfiedLinkError" "$LOG_FILE" || echo 0) 次"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# 提取关键信息到摘要文件
SUMMARY_FILE="crash_summary_$(date +%Y%m%d_%H%M%S).txt"
echo "生成摘要文件: $SUMMARY_FILE"
{
    echo "═══════════════════════════════════════════════════════"
    echo "崩溃分析摘要 - $(date)"
    echo "═══════════════════════════════════════════════════════"
    echo ""
    echo "设备信息："
    echo "  型号: $DEVICE_MODEL"
    echo "  Android: $ANDROID_VERSION (API $SDK_VERSION)"
    echo ""
    echo "崩溃统计："
    echo "  FATAL EXCEPTION: $(grep -c "FATAL EXCEPTION" "$LOG_FILE" || echo 0)"
    echo "  Native Crash: $(grep -c "libc.*Fatal" "$LOG_FILE" || echo 0)"
    echo "  dlopen failed: $(grep -c "dlopen failed" "$LOG_FILE" || echo 0)"
    echo "  UnsatisfiedLinkError: $(grep -c "UnsatisfiedLinkError" "$LOG_FILE" || echo 0)"
    echo ""
    echo "═══════════════ 完整 FATAL EXCEPTION ═══════════════"
    grep -A 50 "FATAL EXCEPTION" "$LOG_FILE" || echo "无"
    echo ""
    echo "═══════════════ 完整 Native Crash ═══════════════"
    grep -A 50 "libc.*Fatal signal" "$LOG_FILE" || echo "无"
    echo ""
    echo "═══════════════ UnsatisfiedLinkError ═══════════════"
    grep -B 10 -A 20 "UnsatisfiedLinkError" "$LOG_FILE" || echo "无"
    echo ""
    echo "═══════════════ dlopen failed ═══════════════"
    grep -B 5 -A 10 "dlopen failed" "$LOG_FILE" || echo "无"
    echo ""
    echo "═══════════════ CRASH_DEBUG 日志 ═══════════════"
    grep "CRASH_DEBUG" "$LOG_FILE" | tail -50 || echo "无"
} > "$SUMMARY_FILE"

echo ""
echo "✅ 摘要文件已生成: $SUMMARY_FILE"
echo ""
echo "请将以下文件发送给开发者分析："
echo "  1. $LOG_FILE (完整日志)"
echo "  2. $SUMMARY_FILE (摘要)"
echo ""

