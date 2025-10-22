#!/bin/bash

echo "
================================================
📊 每日任务重置逻辑检查报告
================================================
"

echo "1️⃣ 设备时间信息："
echo "----------------------------------------"
adb shell "date '+当前时间: %Y-%m-%d %H:%M:%S %Z'"
adb shell "date -u '+UTC时间: %Y-%m-%d %H:%M:%S'"
echo ""

echo "2️⃣ SharedPreferences中的任务状态："
echo "----------------------------------------"
echo "📖 阅读任务 (QuranReadingQuestPrefs):"
adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml 2>/dev/null | grep -E 'last_date|task_completed_today|completed_date|pages_read_today'" || echo "  ❌ 文件不存在"
echo ""

echo "🎧 听力任务 (QuranListeningQuestPrefs):"
adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranListeningQuestPrefs.xml 2>/dev/null | grep -E 'last_date|task_completed_today|completed_date|seconds_listened_today|minutes_listened_today'" || echo "  ❌ 文件不存在"
echo ""

echo "3️⃣ 代码中的重置逻辑分析："
echo "----------------------------------------"
echo "📍 重置触发点："
echo "  - QuranReadingTracker.getTodayPagesRead()"
echo "  - QuranListeningTracker.getTodaySecondsListened()"
echo "  - 触发条件: LocalDate 不匹配 (last_date != today)"
echo ""
echo "📍 日期获取方式："
echo "  - SimpleDateFormat('yyyy-MM-dd', Locale.US).format(new Date())"
echo "  - 使用系统本地时区（北京时间 UTC+8）"
echo ""

echo "4️⃣ Firebase Firestore 文档路径："
echo "----------------------------------------"
CURRENT_DATE=$(adb shell "date '+%Y-%m-%d'" | tr -d '\r')
echo "  今日文档路径: users/{userId}/dailyProgress/${CURRENT_DATE}"
echo "  昨日文档路径: users/{userId}/dailyProgress/$(date -v-1d '+%Y-%m-%d' 2>/dev/null || date -d 'yesterday' '+%Y-%m-%d')"
echo ""

echo "5️⃣ 重置时间点分析："
echo "----------------------------------------"
echo "  🕐 重置时间: 每天 00:00:00 (本地时区)"
echo "  📅 重置方式: 被动重置（下次访问时检查日期）"
echo "  ⚠️  注意: 不是主动定时重置！"
echo ""
echo "  工作原理："
echo "    1. 用户打开应用"
echo "    2. 调用 getTodayPagesRead() / getTodaySecondsListened()"
echo "    3. 比较 last_date 和当前日期"
echo "    4. 如果不同 → 重置 SharedPreferences"
echo "    5. Firebase自动按日期创建新文档"
echo ""

echo "6️⃣ 当前状态判断："
echo "----------------------------------------"
STORED_DATE=$(adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml 2>/dev/null" | grep 'last_date' | sed 's/.*<string name="last_date">\(.*\)<\/string>.*/\1/')
CURRENT_DATE_DEVICE=$(adb shell "date '+%Y-%m-%d'" | tr -d '\r')

echo "  存储日期: ${STORED_DATE}"
echo "  当前日期: ${CURRENT_DATE_DEVICE}"
echo ""

if [ "${STORED_DATE}" = "${CURRENT_DATE_DEVICE}" ]; then
    echo "  ✅ 日期匹配 → 任务状态应保持（今天已完成的任务继续显示完成）"
    echo "  💡 重置时间: 明天 00:00:00（下次打开应用时）"
else
    echo "  🔄 日期不匹配 → 下次启动应用时会自动重置"
fi
echo ""

echo "7️⃣ 用户场景分析："
echo "----------------------------------------"
echo "  ❓ 用户报告: '昨晚23点完成任务，现在12点还显示完成'"
echo ""
echo "  可能情况："
echo "    A) 用户在 10-22 00:00 之后完成任务"
echo "       → 记录日期是 2025-10-22"
echo "       → 现在也是 2025-10-22"
echo "       → 所以显示完成是正确的 ✅"
echo ""
echo "    B) 用户在 10-21 23:xx 完成任务"
echo "       → 记录日期应该是 2025-10-21"
echo "       → 现在是 2025-10-22"
echo "       → 应该已重置，但未重置 ❌"
echo ""

echo "8️⃣ 验证步骤："
echo "----------------------------------------"
echo "  请用户确认："
echo "    1. 昨晚完成任务的确切时间（是10-21还是10-22？）"
echo "    2. 今天是否重新启动过应用？"
echo "    3. 主页任务卡是否显示完成状态（绿色✓）？"
echo ""

echo "9️⃣ 实时测试重置逻辑："
echo "----------------------------------------"
echo "  即将手动修改 last_date 为昨天，然后重启应用..."
read -p "  按Enter继续测试，或Ctrl+C取消: "

YESTERDAY=$(date -v-1d '+%Y-%m-%d' 2>/dev/null || date -d 'yesterday' '+%Y-%m-%d')

# 备份当前数据
adb shell "run-as com.quran.quranaudio.online cp /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml.backup" 2>/dev/null

# 创建测试数据（昨天完成的任务）
cat > /tmp/test_reading_prefs.xml << EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="pages_read_today" value="10" />
    <string name="last_date">${YESTERDAY}</string>
    <boolean name="task_completed_today" value="true" />
    <string name="completed_date">${YESTERDAY}</string>
</map>
EOF

echo "  📤 推送测试数据（昨天的完成记录）..."
adb push /tmp/test_reading_prefs.xml /data/local/tmp/test_prefs.xml > /dev/null
adb shell "run-as com.quran.quranaudio.online cp /data/local/tmp/test_prefs.xml /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml" 2>/dev/null
adb shell "run-as com.quran.quranaudio.online chmod 660 /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml" 2>/dev/null

echo "  🔄 重启应用..."
adb shell am force-stop com.quran.quranaudio.online
sleep 1
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity > /dev/null

echo "  ⏳ 等待5秒..."
sleep 5

echo "  📊 检查重置结果..."
adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml 2>/dev/null | grep -E 'last_date|task_completed_today|pages_read_today'"

echo ""
echo "================================================"
echo "✅ 测试完成！"
echo "================================================"


