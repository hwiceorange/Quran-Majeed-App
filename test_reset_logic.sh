#!/bin/bash

echo "
================================================
🧪 每日任务重置逻辑测试
================================================
"

echo "1️⃣ 当前状态："
echo "----------------------------------------"
echo "📅 设备时间: $(adb shell 'date "+%Y-%m-%d %H:%M:%S %Z"' | tr -d '\r')"
echo "🌍 时区: $(adb shell 'getprop persist.sys.timezone' | tr -d '\r')"
echo ""

echo "📊 当前任务状态 (SharedPreferences):"
adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml 2>/dev/null | grep -E 'last_date|task_completed_today|pages_read_today'" || echo "  ❌ 文件不存在"
echo ""

# 获取昨天的日期
if [[ "$OSTYPE" == "darwin"* ]]; then
    YESTERDAY=$(date -v-1d '+%Y-%m-%d')
else
    YESTERDAY=$(date -d 'yesterday' '+%Y-%m-%d')
fi

echo "2️⃣ 备份当前数据："
echo "----------------------------------------"
adb shell "run-as com.quran.quranaudio.online cp /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml.backup 2>/dev/null" && echo "  ✅ 备份成功" || echo "  ⚠️  备份失败或文件不存在"
echo ""

echo "3️⃣ 创建测试数据（模拟昨天完成的任务）："
echo "----------------------------------------"
echo "  📅 将 last_date 设置为: ${YESTERDAY}"
echo "  ✅ 将 task_completed_today 设置为: true"
echo "  📖 将 pages_read_today 设置为: 10"
echo ""

# 创建测试数据
cat > /tmp/test_reading_prefs.xml << EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="pages_read_today" value="10" />
    <string name="last_date">${YESTERDAY}</string>
    <boolean name="task_completed_today" value="true" />
    <string name="completed_date">${YESTERDAY}</string>
</map>
EOF

# 推送测试数据
adb push /tmp/test_reading_prefs.xml /sdcard/test_prefs.xml > /dev/null 2>&1
adb shell "run-as com.quran.quranaudio.online cp /sdcard/test_prefs.xml /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml 2>/dev/null"
adb shell "run-as com.quran.quranaudio.online chmod 660 /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml 2>/dev/null"
adb shell "rm /sdcard/test_prefs.xml" 2>/dev/null

echo "  ✅ 测试数据已写入"
echo ""

echo "4️⃣ 验证测试数据："
echo "----------------------------------------"
adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml 2>/dev/null | grep -E 'last_date|task_completed_today|pages_read_today'"
echo ""

echo "5️⃣ 重启应用（触发重置逻辑）："
echo "----------------------------------------"
adb shell am force-stop com.quran.quranaudio.online
echo "  ⏸️  应用已停止"
sleep 1

# 清空日志
adb logcat -c

# 启动应用并在后台监控日志
echo "  🚀 启动应用..."
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity > /dev/null 2>&1

# 监控日志（后台运行）
adb logcat -v time | grep -E "(QuranReadingTracker|getTodayPagesRead|resetDailyProgress|Date check)" --color=never > /tmp/quest_reset_logs.txt &
LOG_PID=$!

echo "  ⏳ 等待8秒（应用启动+主页加载）..."
sleep 8

# 停止日志监控
kill $LOG_PID 2>/dev/null

echo ""
echo "6️⃣ 检查重置结果："
echo "----------------------------------------"

CURRENT_DATA=$(adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml 2>/dev/null")

echo "$CURRENT_DATA" | grep -E 'last_date|task_completed_today|pages_read_today'

CURRENT_DATE=$(adb shell 'date "+%Y-%m-%d"' | tr -d '\r')
SAVED_DATE=$(echo "$CURRENT_DATA" | grep 'last_date' | sed 's/.*<string name="last_date">\(.*\)<\/string>.*/\1/')
TASK_COMPLETED=$(echo "$CURRENT_DATA" | grep 'task_completed_today' | grep -o 'value="[^"]*"' | cut -d'"' -f2)
PAGES_READ=$(echo "$CURRENT_DATA" | grep 'pages_read_today' | grep -o 'value="[^"]*"' | cut -d'"' -f2)

echo ""
echo "7️⃣ 结果分析："
echo "----------------------------------------"
echo "  昨天日期: ${YESTERDAY}"
echo "  当前日期: ${CURRENT_DATE}"
echo "  保存日期: ${SAVED_DATE}"
echo "  任务完成: ${TASK_COMPLETED}"
echo "  已读页数: ${PAGES_READ}"
echo ""

if [ "${SAVED_DATE}" = "${CURRENT_DATE}" ]; then
    echo "  ✅ 日期已更新为今天"
else
    echo "  ❌ 日期未更新（期望: ${CURRENT_DATE}, 实际: ${SAVED_DATE}）"
fi

if [ "${TASK_COMPLETED}" = "false" ]; then
    echo "  ✅ 任务完成状态已重置"
elif [ -z "${TASK_COMPLETED}" ]; then
    echo "  ✅ 任务完成状态已重置（键已删除）"
else
    echo "  ❌ 任务完成状态未重置（仍为: ${TASK_COMPLETED}）"
fi

if [ "${PAGES_READ}" = "0" ] || [ -z "${PAGES_READ}" ]; then
    echo "  ✅ 已读页数已重置为0"
else
    echo "  ❌ 已读页数未重置（仍为: ${PAGES_READ}）"
fi

echo ""
echo "8️⃣ 相关日志："
echo "----------------------------------------"
if [ -f /tmp/quest_reset_logs.txt ]; then
    cat /tmp/quest_reset_logs.txt | head -20
    rm /tmp/quest_reset_logs.txt
else
    echo "  ⚠️  无日志记录"
fi

echo ""
echo "9️⃣ 恢复原始数据（可选）："
echo "----------------------------------------"
echo "  如需恢复备份，运行："
echo "  adb shell \"run-as com.quran.quranaudio.online cp /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml.backup /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml\""
echo ""

echo "================================================"
if [ "${SAVED_DATE}" = "${CURRENT_DATE}" ] && ([ "${TASK_COMPLETED}" = "false" ] || [ -z "${TASK_COMPLETED}" ]) && ([ "${PAGES_READ}" = "0" ] || [ -z "${PAGES_READ}" ]); then
    echo "✅ 测试通过：重置逻辑工作正常"
else
    echo "❌ 测试失败：重置逻辑可能存在问题"
fi
echo "================================================"


