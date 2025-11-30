#!/bin/bash

# 多语言翻译测试脚本
# 用于验证按需下载功能是否正常工作

echo "🧪 Quran0 多语言翻译测试脚本"
echo "=================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 测试步骤计数器
STEP=1

print_step() {
    echo ""
    echo -e "${YELLOW}📋 步骤 ${STEP}: $1${NC}"
    STEP=$((STEP + 1))
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo "ℹ️  $1"
}

# 检查设备连接
print_step "检查设备连接"
if ! adb devices | grep -q "device$"; then
    print_error "未检测到 Android 设备"
    print_info "请确保设备已连接并开启 USB 调试"
    exit 1
fi
print_success "设备已连接"

# 获取包名
PACKAGE_NAME="com.quran.quranaudio.online"

# 编译应用
print_step "编译应用"
print_info "正在编译 Debug 版本..."
cd "$(dirname "$0")" || exit
if ./gradlew assembleDebug --quiet; then
    print_success "编译成功"
else
    print_error "编译失败"
    exit 1
fi

# 安装应用
print_step "安装应用"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    print_error "APK 文件不存在: $APK_PATH"
    exit 1
fi

print_info "正在安装..."
if adb install -r "$APK_PATH" 2>&1 | grep -q "Success"; then
    print_success "安装成功"
else
    print_error "安装失败"
    exit 1
fi

# 清除应用数据（可选）
print_step "清除应用数据"
echo "是否要清除应用数据以模拟新用户？(y/n)"
read -r CLEAR_DATA
if [ "$CLEAR_DATA" = "y" ]; then
    adb shell pm clear $PACKAGE_NAME > /dev/null 2>&1
    print_success "应用数据已清除"
else
    print_info "保持现有数据"
fi

# 启动应用
print_step "启动应用"
print_info "正在启动应用..."
adb shell am start -n "${PACKAGE_NAME}/.quran_module.activities.ActivitySplash" > /dev/null 2>&1
print_success "应用已启动"

# 开始日志监控
print_step "监控日志"
echo ""
echo "🔍 实时日志监控（按 Ctrl+C 停止）"
echo "=================================="
echo ""
print_info "请按照以下步骤操作："
echo "  1️⃣  选择语言：Bengali (বাংলা)"
echo "  2️⃣  选择翻译：তাইসীরুল কুরআন"
echo "  3️⃣  点击 Continue 按钮"
echo "  4️⃣  等待下载完成（约2-3秒）"
echo "  5️⃣  进入主页，打开古兰经"
echo "  6️⃣  验证经文显示为孟加拉语"
echo ""
echo "📊 日志输出："
echo "-----------------------------------"

# 监控关键日志
adb logcat -c  # 清除旧日志
adb logcat | grep -E "FragOnboardQuranVersion|QuranTranslDBHelper|TranslUtils" --line-buffered | while read -r line; do
    # 高亮重要信息
    if echo "$line" | grep -q "Auto-selected translation"; then
        echo -e "${GREEN}$line${NC}"
    elif echo "$line" | grep -q "下载完成\|download.*complete\|Translation downloaded"; then
        echo -e "${GREEN}$line${NC}"
    elif echo "$line" | grep -q "ERROR\|Exception\|Failed"; then
        echo -e "${RED}$line${NC}"
    elif echo "$line" | grep -q "保存\|saved\|Stored"; then
        echo -e "${YELLOW}$line${NC}"
    else
        echo "$line"
    fi
done

echo ""
print_success "测试完成！"

