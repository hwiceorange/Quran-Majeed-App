#!/bin/bash

# Quiz Layout Fix Verification Script
# 验证答题模块布局修复

echo "================================================"
echo "Quiz Layout Fix Verification"
echo "答题模块布局修复验证"
echo "================================================"
echo ""

# 检查修复1: frag_main.xml 中是否移除了左右边距
echo "✓ 修复1: 检查答题模块宽度统一..."
if grep -q 'android:id="@+id/quiz_entry_view"' app/src/main/res/layout/frag_main.xml; then
    # 检查是否还有 marginStart 或 marginEnd
    if grep -A3 'android:id="@+id/quiz_entry_view"' app/src/main/res/layout/frag_main.xml | grep -q "marginStart\|marginEnd"; then
        echo "  ❌ 问题: quiz_entry_view 仍然有左右边距"
    else
        echo "  ✅ 已修复: quiz_entry_view 左右边距已移除"
    fi
else
    echo "  ⚠️  警告: 未找到 quiz_entry_view"
fi
echo ""

# 检查修复2: dimens.xml 中按钮高度是否为 44dp
echo "✓ 修复2: 检查按钮高度..."
button_height=$(grep "quiz_option_height" app/src/main/res/values/dimens.xml | grep -o "[0-9]\+dp")
if [ "$button_height" = "44dp" ]; then
    echo "  ✅ 已修复: 按钮高度 = 44dp"
elif [ "$button_height" = "52dp" ]; then
    echo "  ❌ 问题: 按钮高度仍为 52dp，需要修改为 44dp"
else
    echo "  ⚠️  当前按钮高度: $button_height"
fi
echo ""

# 检查修复3: view_daily_quran_quiz.xml 中最后一个按钮是否有底部边距
echo "✓ 修复3: 检查按钮底部边距..."
if grep -A15 'android:id="@+id/btn_option_d"' app/src/main/res/layout/view_daily_quran_quiz.xml | grep -q "layout_marginBottom"; then
    echo "  ✅ 已修复: btn_option_d 有底部边距"
else
    echo "  ❌ 问题: btn_option_d 没有底部边距"
fi

# 检查根布局是否移除了 paddingBottom
if grep -A5 "androidx.constraintlayout.widget.ConstraintLayout" app/src/main/res/layout/view_daily_quran_quiz.xml | grep -q "paddingBottom"; then
    echo "  ⚠️  注意: 根布局仍有 paddingBottom（可能重复）"
else
    echo "  ✅ 已优化: 根布局 paddingBottom 已移除"
fi
echo ""

echo "================================================"
echo "验证完成！"
echo ""
echo "预期效果："
echo "  • 答题模块宽度与其他卡片一致"
echo "  • 按钮高度缩减到 44dp"
echo "  • 按钮底部有 16dp 的美观间距"
echo ""
echo "下一步："
echo "  1. 重新编译应用"
echo "  2. 在英语环境下测试"
echo "  3. 检查布局是否符合预期"
echo "  4. 反馈交互测试结果"
echo "================================================"

