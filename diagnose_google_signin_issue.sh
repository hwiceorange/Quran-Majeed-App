#!/bin/bash
echo "════════════════════════════════════════════════"
echo "🔍 Google Sign-In 问题诊断"
echo "════════════════════════════════════════════════"
echo ""

# 1. 检查 Debug Keystore SHA-1
echo "【1. Debug Keystore SHA-1】"
if [ -f ~/.android/debug.keystore ]; then
    echo "✅ Debug keystore 存在"
    echo ""
    keytool -list -v -keystore ~/.android/debug.keystore \
      -alias androiddebugkey -storepass android -keypass android 2>/dev/null | \
      grep -A 1 "证书指纹" | tail -1
    echo ""
    DEBUG_SHA1=$(keytool -list -v -keystore ~/.android/debug.keystore \
      -alias androiddebugkey -storepass android -keypass android 2>/dev/null | \
      grep "SHA1:" | cut -d' ' -f3 | tr -d ':' | tr '[:upper:]' '[:lower:]')
    echo "Debug SHA-1 (无冒号): $DEBUG_SHA1"
else
    echo "❌ Debug keystore 不存在"
fi
echo ""

# 2. 检查 Release Keystore SHA-1
echo "【2. Release Keystore SHA-1】"
if [ -f app/quran_keystore ]; then
    echo "✅ Release keystore 存在"
    echo ""
    keytool -list -v -keystore app/quran_keystore \
      -alias key0 -storepass Huwei123 -keypass Huwei123 2>/dev/null | \
      grep -A 1 "证书指纹" | tail -1
    echo ""
    RELEASE_SHA1=$(keytool -list -v -keystore app/quran_keystore \
      -alias key0 -storepass Huwei123 -keypass Huwei123 2>/dev/null | \
      grep "SHA1:" | cut -d' ' -f3 | tr -d ':' | tr '[:upper:]' '[:lower:]')
    echo "Release SHA-1 (无冒号): $RELEASE_SHA1"
else
    echo "❌ Release keystore 不存在"
fi
echo ""

# 3. 检查 Firebase 配置
echo "【3. Firebase 配置中的 SHA-1】"
cat app/google-services.json | grep "certificate_hash" | sed 's/.*": "//;s/".*//' | while read sha1; do
    echo "  • $sha1"
done
echo ""

# 4. 对比分析
echo "【4. SHA-1 匹配分析】"
FIREBASE_SHA1_1="6dc10985e207824215ec7610200f3741eb4640ab"
FIREBASE_SHA1_2="8ae5e2c39e284c7c3277ed2e8957bf08ab4f9e45"

if [ "$DEBUG_SHA1" == "$FIREBASE_SHA1_1" ] || [ "$DEBUG_SHA1" == "$FIREBASE_SHA1_2" ]; then
    echo "✅ Debug SHA-1 已在 Firebase 中注册"
else
    echo "❌ Debug SHA-1 未在 Firebase 中注册！"
    echo "   Debug:    $DEBUG_SHA1"
    echo "   Firebase: $FIREBASE_SHA1_1"
    echo "   Firebase: $FIREBASE_SHA1_2"
    echo ""
    echo "⚠️  这是导致登录失败的主要原因！"
fi
echo ""

if [ "$RELEASE_SHA1" == "$FIREBASE_SHA1_1" ] || [ "$RELEASE_SHA1" == "$FIREBASE_SHA1_2" ]; then
    echo "✅ Release SHA-1 已在 Firebase 中注册"
else
    echo "❌ Release SHA-1 未在 Firebase 中注册"
fi
echo ""

# 5. 检查 Web Client ID
echo "【5. Web Client ID 配置】"
WEB_CLIENT_ID=$(cat app/google-services.json | grep -A 1 '"client_type": 3' | grep "client_id" | sed 's/.*": "//;s/".*//')
CODE_CLIENT_ID=$(grep "requestIdToken" app/src/main/java/com/quran/quranaudio/online/Utils/GoogleAuthManager.java | sed 's/.*requestIdToken("//;s/").*//')

echo "  Firebase: $WEB_CLIENT_ID"
echo "  代码中:   $CODE_CLIENT_ID"

if [ "$WEB_CLIENT_ID" == "$CODE_CLIENT_ID" ]; then
    echo "  ✅ Web Client ID 匹配"
else
    echo "  ❌ Web Client ID 不匹配！"
fi
echo ""

# 6. 修复建议
echo "【6. 修复建议】"
echo ""

if [ "$DEBUG_SHA1" != "$FIREBASE_SHA1_1" ] && [ "$DEBUG_SHA1" != "$FIREBASE_SHA1_2" ]; then
    echo "🔧 立即修复步骤："
    echo ""
    echo "1. 在 Firebase Console 添加 Debug SHA-1:"
    echo "   → https://console.firebase.google.com/project/quran-majeed-aa3d2/settings/general"
    echo "   → 找到 Android app"
    echo "   → 点击 'Add fingerprint'"
    echo "   → 粘贴: $DEBUG_SHA1"
    echo "   → 点击 'Save'"
    echo ""
    echo "2. 下载新的 google-services.json"
    echo ""
    echo "3. 替换项目文件:"
    echo "   cp ~/Downloads/google-services.json app/google-services.json"
    echo ""
    echo "4. 重新编译:"
    echo "   ./gradlew clean :app:assembleDebug"
    echo "   adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "5. 等待 5-10 分钟后测试"
else
    echo "✅ SHA-1 配置正确"
    echo "   请检查其他可能的原因："
    echo "   • 网络连接"
    echo "   • Google Play Services 版本"
    echo "   • Firebase 项目状态"
fi

echo ""
echo "════════════════════════════════════════════════"
echo "✅ 诊断完成"
echo "════════════════════════════════════════════════"

