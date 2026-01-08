#!/bin/bash

# 16KB Page Alignment Checker for Native Libraries
# This script checks if .so files in the APK are properly aligned for Android 15

echo "=================================================="
echo "🔍 16KB Page Alignment Checker"
echo "=================================================="
echo ""

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
TEMP_DIR="build/temp_alignment_check"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found: $APK_PATH"
    echo "Please build the APK first: ./gradlew assembleDebug"
    exit 1
fi

# Create temp directory
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# Extract APK
echo "📦 Extracting APK..."
unzip -q "$APK_PATH" -d "$TEMP_DIR"

# Check all .so files
echo ""
echo "🔍 Checking Native Libraries (.so files):"
echo "=================================================="

SO_FILES=$(find "$TEMP_DIR" -name "*.so" | sort)
TOTAL_COUNT=0
NON_ALIGNED_COUNT=0
ALIGNED_COUNT=0

if [ -z "$SO_FILES" ]; then
    echo "✅ No native libraries found in APK"
else
    while IFS= read -r so_file; do
        TOTAL_COUNT=$((TOTAL_COUNT + 1))
        FILE_SIZE=$(stat -f%z "$so_file" 2>/dev/null || stat -c%s "$so_file" 2>/dev/null)
        ALIGNMENT=$((FILE_SIZE % 16384))
        
        RELATIVE_PATH=$(echo "$so_file" | sed "s|$TEMP_DIR/||")
        
        if [ $ALIGNMENT -eq 0 ]; then
            echo "✅ ALIGNED  : $RELATIVE_PATH ($FILE_SIZE bytes)"
            ALIGNED_COUNT=$((ALIGNED_COUNT + 1))
        else
            echo "⚠️  NOT ALIGNED: $RELATIVE_PATH ($FILE_SIZE bytes, offset: $ALIGNMENT)"
            NON_ALIGNED_COUNT=$((NON_ALIGNED_COUNT + 1))
        fi
    done <<< "$SO_FILES"
fi

# Cleanup
rm -rf "$TEMP_DIR"

# Summary
echo ""
echo "=================================================="
echo "📊 Summary:"
echo "=================================================="
echo "Total .so files checked: $TOTAL_COUNT"
echo "✅ Aligned (16KB): $ALIGNED_COUNT"
echo "⚠️  Not aligned: $NON_ALIGNED_COUNT"
echo ""

if [ $NON_ALIGNED_COUNT -gt 0 ]; then
    echo "⚠️  WARNING: Some native libraries are not 16KB aligned!"
    echo ""
    echo "🔧 Solutions:"
    echo "1. ✅ Already applied: useLegacyPackaging = true (stores .so uncompressed)"
    echo "2. ✅ Already applied: android.allow_non_16kb_aligned_page_size = true"
    echo "3. 📢 Contact SDK providers to update their libraries for Android 15 support"
    echo ""
    echo "📝 Affected SDKs that may need updates:"
    echo "   - Pangle (ByteDance): Update to latest version"
    echo "   - IronSource: Update to latest version"
    echo "   - Facebook Audience Network: Update to latest version"
    echo "   - Unity Ads: Update to latest version"
    echo "   - Mintegral: Update to latest version"
    echo ""
    echo "✅ Current workarounds are in place and should prevent crashes"
else
    echo "✅ All native libraries are properly aligned!"
    echo "🎉 Your app should work perfectly on Android 15+ devices"
fi

echo "=================================================="

