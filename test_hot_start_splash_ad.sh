#!/bin/bash

# 测试热启动开屏广告功能
# 用于验证后台恢复和前台热启动时是否正确展示开屏广告

echo "========================================"
echo "🧪 热启动开屏广告测试脚本"
echo "========================================"
echo ""

# 检查设备连接
if ! adb devices | grep -q "device$"; then
    echo "❌ 错误：没有检测到连接的设备"
    echo "请连接Android设备或启动模拟器"
    exit 1
fi

echo "✅ 设备已连接"
echo ""

# 包名
PACKAGE="com.quran.quranaudio.online"

echo "📦 测试包名: $PACKAGE"
echo ""

# 清理旧日志
adb logcat -c

echo "========================================"
echo "测试场景 1: 冷启动（首次启动）"
echo "========================================"
echo ""
echo "🔄 强制停止应用..."
adb shell am force-stop $PACKAGE
sleep 2

echo "🚀 启动应用（冷启动）..."
adb shell am start -n $PACKAGE/.SplashScreenActivity

echo ""
echo "📱 观察要点："
echo "  1. 应用启动页进度条到达100%"
echo "  2. 显示开屏广告"
echo "  3. 广告关闭后进入主界面"
echo ""
echo "⏰ 等待20秒观察..."
sleep 20

echo ""
echo "========================================"
echo "测试场景 2: 后台热启动"
echo "========================================"
echo ""
echo "🏠 按Home键将应用切到后台..."
adb shell input keyevent KEYCODE_HOME
sleep 3

echo "📱 应用已进入后台"
echo "⏰ 等待5秒模拟后台停留..."
sleep 5

echo "🔄 从最近任务恢复应用（热启动）..."
adb shell am start -n $PACKAGE/.prayertimes.ui.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER

echo ""
echo "📱 观察要点："
echo "  1. 应用从后台恢复到前台"
echo "  2. 应该显示开屏广告"
echo "  3. 广告关闭后回到之前的界面"
echo ""
echo "⏰ 等待20秒观察..."
sleep 20

echo ""
echo "========================================"
echo "测试场景 3: 再次后台热启动（验证广告预加载）"
echo "========================================"
echo ""
echo "🏠 再次按Home键将应用切到后台..."
adb shell input keyevent KEYCODE_HOME
sleep 3

echo "📱 应用已进入后台"
echo "⏰ 等待5秒..."
sleep 5

echo "🔄 再次从最近任务恢复应用..."
adb shell am start -n $PACKAGE/.prayertimes.ui.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER

echo ""
echo "📱 观察要点："
echo "  1. 应用从后台恢复到前台"
echo "  2. 应该显示开屏广告（验证预加载是否正常）"
echo "  3. 广告关闭后回到之前的界面"
echo ""
echo "⏰ 等待20秒观察..."
sleep 20

echo ""
echo "========================================"
echo "📊 日志分析"
echo "========================================"
echo ""
echo "🔍 查看开屏广告相关日志..."
echo ""
adb logcat -d | grep -E "(App|ActivitySplash|AdFactory)" | grep -E "(开屏|AppOpen|Hot start|app open ad)" | tail -50

echo ""
echo "========================================"
echo "✅ 测试完成"
echo "========================================"
echo ""
echo "📋 验证清单："
echo "  [ ] 冷启动时展示开屏广告"
echo "  [ ] 后台热启动时展示开屏广告"
echo "  [ ] 广告展示后应用功能正常"
echo "  [ ] 没有崩溃或ANR"
echo "  [ ] 广告关闭后正确预加载下一个广告"
echo ""
echo "💡 如果广告没有显示，请检查："
echo "  1. 网络连接是否正常"
echo "  2. 是否是测试广告（测试广告可能快速关闭）"
echo "  3. 广告位ID配置是否正确"
echo "  4. AdMob账号是否正常"
echo ""
echo "📝 完整日志查看命令："
echo "  adb logcat | grep -E '(App|ActivitySplash|AdFactory)'"
echo ""

