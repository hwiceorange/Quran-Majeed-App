#!/bin/bash

# 编译并安装应用
# 使用方法: ./compile_and_install_app.sh

echo "🔨 开始编译应用..."
echo "========================================="

cd /Users/huwei/AndroidStudioProjects/quran0

# 清理
echo ""
echo "🧹 清理旧的构建文件..."
./gradlew clean

# 编译
echo ""
echo "⚙️  编译 Debug 版本..."
./gradlew :app:assembleDebug

# 检查编译结果
if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "✅ 编译成功！"
    echo "========================================="
    echo ""
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ -f "$APK_PATH" ]; then
        echo "📦 APK 文件: $APK_PATH"
        echo "📊 文件大小: $(ls -lh $APK_PATH | awk '{print $5}')"
        echo ""
        
        # 检查设备连接
        echo "🔍 检查设备连接..."
        adb devices
        echo ""
        
        # 询问是否安装
        echo "准备安装到设备？"
        echo "执行: adb install -r $APK_PATH"
        echo ""
        
        # 卸载旧版本并安装新版本
        echo "🗑️  卸载旧版本..."
        adb uninstall com.quran.quranaudio.online 2>/dev/null
        
        echo "📲 安装新版本..."
        adb install -r "$APK_PATH"
        
        if [ $? -eq 0 ]; then
            echo ""
            echo "========================================="
            echo "✅ 安装成功！"
            echo "========================================="
            echo ""
            echo "🧪 测试步骤："
            echo "1. 打开应用"
            echo "2. 设置语言为印尼语（如果还没设置）"
            echo "3. 打开古兰经任意章节"
            echo "4. 点击注释按钮"
            echo "5. 验证是否显示印尼语注释"
            echo ""
            echo "📊 监控日志："
            echo "adb logcat | grep -E 'ActivityTafsir|CustomTafsir|API_REQUEST|API_RESPONSE'"
        else
            echo ""
            echo "❌ 安装失败！"
            echo "请手动安装："
            echo "adb install -r $APK_PATH"
        fi
    else
        echo "❌ APK 文件未找到: $APK_PATH"
    fi
else
    echo ""
    echo "========================================="
    echo "❌ 编译失败！"
    echo "========================================="
    echo ""
    echo "请检查错误信息，或在 Android Studio 中编译。"
fi

