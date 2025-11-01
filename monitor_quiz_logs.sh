#!/bin/bash

# Quiz Module Log Monitor Script
# 用于实时监控 Quiz 模块的日志输出

echo "=========================================="
echo "Quiz Module Log Monitor"
echo "=========================================="
echo ""
echo "监控 Quiz 相关日志..."
echo "提示: 按 Ctrl+C 停止监控"
echo ""
echo "=========================================="
echo ""

# 使用 adb logcat 监控日志
adb logcat -c  # 清除之前的日志
adb logcat | grep -E "Quiz|quiz|FragMain.*Quiz|initializeQuizModule|bindCurrentQuizQuestion|isQuizSupportedLanguage|handleQuizOptionSelected" --color=always

