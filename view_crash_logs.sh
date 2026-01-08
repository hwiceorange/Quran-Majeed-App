#!/bin/bash

# 🔍 Crash Logs Viewer - 实时查看应用崩溃日志
# 使用方法：./view_crash_logs.sh

echo "=================================================="
echo "🔍 Quran Majeed Crash Logs Viewer"
echo "=================================================="
echo ""
echo "📱 正在清除旧日志并开始监控..."
echo ""

# 清除旧日志
adb logcat -c

# 实时显示崩溃相关日志
# 监控以下标签：
# - CRASH_DEBUG: 应用初始化日志
# - CRASH_HANDLER: 全局异常捕获
# - AndroidRuntime: 系统崩溃日志  
# - DEBUG: 一般调试日志

echo "🚀 开始监控崩溃日志（按 Ctrl+C 停止）..."
echo "=================================================="
echo ""

adb logcat -s \
    CRASH_DEBUG:V \
    CRASH_HANDLER:V \
    AndroidRuntime:E \
    System.err:W \
    DEBUG:I \
    TAG:V \
    *:F

# 备注：
# :V = Verbose (所有日志)
# :E = Error (仅错误)
# :W = Warning (警告和错误)
# :I = Info (信息、警告和错误)
# :F = Fatal (致命错误)

