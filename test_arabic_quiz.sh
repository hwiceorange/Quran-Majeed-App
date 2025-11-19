#!/bin/bash

echo "🔧 Quiz阿拉伯语支持测试"
echo "================================================"
echo ""

echo "📦 验证 quiz.zip 内容..."
echo ""
unzip -l quiz/src/main/assets/quiz.zip
echo ""

echo "================================================"
echo ""
echo "📱 请在手机上进行以下测试:"
echo ""
echo "   步骤 1: 切换到阿拉伯语"
echo "   ─────────────────────────────────────"
echo "   a. 打开应用"
echo "   b. 进入 Settings → Language"
echo "   c. 选择 العربية (Arabic)"
echo "   d. 应用会重启"
echo ""
echo "   步骤 2: 验证 Quiz 模块"
echo "   ─────────────────────────────────────"
echo "   e. 进入 Quiz 模块"
echo "   f. 观察题目内容"
echo ""
echo "   ✅ 预期结果："
echo "      - 题目应该显示阿拉伯语内容"
echo "      - 题目内容从右到左显示（RTL）"
echo ""
echo "================================================"
echo ""
echo "🔍 监控日志..."
echo "   查找: 应用语言, 题目文件, quiz_all_ar"
echo "================================================"
echo ""

adb logcat -c
sleep 1

echo "✅ 日志已清空，准备监控"
echo ""
echo "请现在打开应用并进入 Quiz 模块..."
echo ""

adb logcat | grep -E "QuestionTools|AppConfig.*语言|quiz_all_ar" --line-buffered --color=always

