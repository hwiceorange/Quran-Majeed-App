#!/bin/bash

echo "================================================"
echo "🎧 听力任务完整测试 - 15分钟"
echo "================================================"
echo ""
echo "📱 请按以下步骤操作："
echo ""
echo "1️⃣  确认主页显示的 'Quran Listening' 目标是 15 分钟"
echo "2️⃣  点击 'Quran Listening' 任务的 'Go' 按钮"
echo "3️⃣  点击播放 ▶️ 开始播放"
echo "4️⃣  保持播放状态 15 分钟"
echo "     - 可以放在一边，不需要盯着屏幕"
echo "     - 如果中途暂停，暂停时间不会计入有效时长"
echo "5️⃣  15分钟后，按返回键回到主页"
echo "6️⃣  观察主页任务状态是否更新为 ✓"
echo ""
echo "================================================"
echo "⏰ 当前时间: $(date '+%H:%M:%S')"
echo "⏰ 预计完成时间: $(date -v+15M '+%H:%M:%S' 2>/dev/null || date -d '+15 minutes' '+%H:%M:%S' 2>/dev/null || echo '15分钟后')"
echo "================================================"
echo ""
echo "🔍 后台日志监控已启动，请开始操作..."
echo ""

# 启动日志监控
adb logcat -v time | grep -E "(QuranListeningTracker|ActivityReader.*🎧|QuestRepository.*task2|markTaskComplete|HomeQuestsViewModel.*progress|DailyQuestsManager.*Task 2|🔥|🔍|🚀|✅.*Task 2|❌)" --line-buffered | while read line; do
    echo "[$(date '+%H:%M:%S')] $line"
done

