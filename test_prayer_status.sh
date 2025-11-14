#!/bin/bash

# 🧪 测试祷告状态功能
# 用途：验证祷告4种状态是否正确显示和交互

echo "📱 ===== 祷告状态测试脚本 ====="
echo ""
echo "✅ 请按以下步骤测试："
echo ""
echo "📋 测试步骤："
echo ""
echo "1️⃣  【Pending 状态测试】"
echo "   - 打开 Salat 页面"
echo "   - 未记录的祷告应显示绿色 'TRACK' 按钮"
echo "   - 点击 TRACK 按钮应弹出记录对话框"
echo ""
echo "2️⃣  【Ada' 状态测试 - 准时完成】"
echo "   - 在记录对话框选择 'Ada' (准时)"
echo "   - 保存后，应显示：✅ 白色圆圈+绿色打勾图标"
echo "   - 点击图标应进入编辑模式（可改为 Qada'）"
echo ""
echo "3️⃣  【Qada' 状态测试 - 已弥补】"
echo "   - 将 Ada' 改为 'Qada' 并保存"
echo "   - 应显示：⚠️ 橙色警告图标"
echo "   - 点击图标应进入编辑模式（可修改时间/备注）"
echo ""
echo "4️⃣  【Missed 状态测试 - 错过】"
echo "   - 将状态改为 'Missed' 并保存"
echo "   - 应显示：❌ 红色错误图标"
echo "   - 点击图标应立即进入 Qada' Log 对话框（默认选择 Qada'）"
echo ""
echo "🔍 开始监控日志..."
echo ""
echo "================================================"
echo ""

# 清空日志
adb logcat -c

# 实时监控相关日志
adb logcat | grep -E "PrayersFragment|PrayerLog|updatePrayerStatusUI|onSalahTrackClicked" --line-buffered | while IFS= read -r line
do
    # 高亮关键信息
    if echo "$line" | grep -q "updatePrayerStatusUI"; then
        echo "🎨 $line"
    elif echo "$line" | grep -q "onSalahTrackClicked"; then
        echo "🔘 $line"
    elif echo "$line" | grep -q "Ada'"; then
        echo "✅ $line"
    elif echo "$line" | grep -q "Qada'"; then
        echo "⚠️ $line"
    elif echo "$line" | grep -q "Missed"; then
        echo "❌ $line"
    elif echo "$line" | grep -q "Pending"; then
        echo "📝 $line"
    else
        echo "$line"
    fi
done


