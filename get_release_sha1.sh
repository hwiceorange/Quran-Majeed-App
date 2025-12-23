#!/bin/bash

echo "=========================================="
echo "🔍 获取 Release Keystore SHA-1 指纹"
echo "=========================================="
echo ""

# Keystore 路径
KEYSTORE_PATH="/Users/huwei_kt126.com/Documents/Quran-Majeed-App/app/quran_keystore"
KEY_ALIAS="key0"
STORE_PASS="Huwei123"
KEY_PASS="Huwei123"

# 检查 keystore 是否存在
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "❌ Keystore 文件不存在: $KEYSTORE_PATH"
    echo ""
    echo "请确认 keystore 文件路径是否正确"
    exit 1
fi

echo "📂 Keystore 路径: $KEYSTORE_PATH"
echo "🔑 Key Alias: $KEY_ALIAS"
echo ""
echo "=========================================="
echo "📊 证书详细信息"
echo "=========================================="
echo ""

# 获取完整证书信息
keytool -list -v -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" 2>&1

echo ""
echo "=========================================="
echo "🎯 SHA-1 指纹（用于 Firebase）"
echo "=========================================="
echo ""

# 提取 SHA-1（带冒号格式）
SHA1_WITH_COLON=$(keytool -list -v -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" 2>/dev/null | \
    grep "SHA1:" | cut -d' ' -f3)

if [ -z "$SHA1_WITH_COLON" ]; then
    echo "❌ 无法获取 SHA-1 指纹"
    echo ""
    echo "可能的原因："
    echo "  1. Keystore 密码错误"
    echo "  2. Key alias 不存在"
    echo "  3. Keystore 文件损坏"
    exit 1
fi

# 转换为小写无冒号格式（Firebase 要求）
SHA1_NO_COLON=$(echo "$SHA1_WITH_COLON" | tr -d ':' | tr '[:upper:]' '[:lower:]')

echo "✅ SHA-1（带冒号）: $SHA1_WITH_COLON"
echo ""
echo "✅ SHA-1（Firebase 格式）: $SHA1_NO_COLON"
echo ""

# 提取 SHA-256
SHA256_WITH_COLON=$(keytool -list -v -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" 2>/dev/null | \
    grep "SHA256:" | cut -d' ' -f3)

SHA256_NO_COLON=$(echo "$SHA256_WITH_COLON" | tr -d ':' | tr '[:upper:]' '[:lower:]')

echo "✅ SHA-256（带冒号）: $SHA256_WITH_COLON"
echo ""
echo "✅ SHA-256（Firebase 格式）: $SHA256_NO_COLON"
echo ""

echo "=========================================="
echo "📋 Firebase 配置中的 SHA-1"
echo "=========================================="
echo ""

# 检查 google-services.json 中的 SHA-1
GOOGLE_SERVICES="/Users/huwei_kt126.com/Documents/Quran-Majeed-App/app/google-services.json"

if [ -f "$GOOGLE_SERVICES" ]; then
    echo "Firebase 中已注册的 SHA-1:"
    echo ""
    grep "certificate_hash" "$GOOGLE_SERVICES" | \
        sed 's/.*"certificate_hash": "\(.*\)".*/  - \1/'
    echo ""
    
    # 检查是否匹配
    if grep -q "$SHA1_NO_COLON" "$GOOGLE_SERVICES"; then
        echo "✅ 当前 Release Keystore 的 SHA-1 已在 Firebase 中注册！"
        echo ""
        echo "如果登录仍然失败，请检查："
        echo "  1. 网络连接"
        echo "  2. Google Play Services 版本"
        echo "  3. Firebase 配置是否最新"
    else
        echo "❌ 当前 Release Keystore 的 SHA-1 未在 Firebase 中注册！"
        echo ""
        echo "⚠️ 这是导致登录失败的主要原因"
    fi
else
    echo "⚠️ google-services.json 文件不存在"
fi

echo ""
echo "=========================================="
echo "📝 后续步骤"
echo "=========================================="
echo ""
echo "1. 复制上面的 SHA-1（Firebase 格式）:"
echo "   $SHA1_NO_COLON"
echo ""
echo "2. 登录 Firebase Console:"
echo "   https://console.firebase.google.com/project/quran-majeed-aa3d2"
echo ""
echo "3. 进入: Project Settings → General"
echo ""
echo "4. 找到 Android app: com.quran.quranaudio.online"
echo ""
echo "5. 在 'SHA certificate fingerprints' 部分:"
echo "   - 检查是否已存在上述 SHA-1"
echo "   - 如果不存在，点击 'Add fingerprint'"
echo "   - 粘贴 SHA-1: $SHA1_NO_COLON"
echo "   - 点击 'Save'"
echo ""
echo "6. 下载新的 google-services.json:"
echo "   - 点击 'Download google-services.json'"
echo "   - 替换: app/google-services.json"
echo ""
echo "7. 重新编译 Release APK:"
echo "   ./gradlew clean"
echo "   ./gradlew :app:assembleRelease"
echo ""
echo "8. 安装并测试:"
echo "   adb install -r app/build/outputs/apk/release/app-release.apk"
echo ""
echo "9. 等待 5-10 分钟让 Firebase 配置生效"
echo ""
echo "=========================================="
echo "✅ 诊断完成"
echo "=========================================="


