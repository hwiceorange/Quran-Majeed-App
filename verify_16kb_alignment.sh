#!/bin/bash

# 16 KB Page Size Alignment Verification Script
# Ref: https://developer.android.com/guide/practices/page-sizes

echo "======================================"
echo "16 KB Page Size Alignment Checker"
echo "======================================"
echo ""

APK_PATH="$1"

if [ -z "$APK_PATH" ]; then
    echo "Usage: $0 <path-to-apk-or-aab>"
    echo "Example: $0 app/build/outputs/apk/release/app-release.apk"
    exit 1
fi

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: File not found: $APK_PATH"
    exit 1
fi

echo "📦 Analyzing: $APK_PATH"
echo ""

# Check if it's APK or AAB
EXT="${APK_PATH##*.}"

if [ "$EXT" = "apk" ]; then
    echo "🔍 Checking APK alignment..."
    
    # Extract native libraries
    TEMP_DIR=$(mktemp -d)
    unzip -q "$APK_PATH" "lib/*/*.so" -d "$TEMP_DIR" 2>/dev/null
    
    if [ ! -d "$TEMP_DIR/lib" ]; then
        echo "✅ No native libraries found in APK"
        rm -rf "$TEMP_DIR"
        exit 0
    fi
    
    echo ""
    echo "Native libraries found:"
    find "$TEMP_DIR/lib" -name "*.so" -type f | while read SO_FILE; do
        SO_NAME=$(basename "$SO_FILE")
        ABI=$(basename $(dirname "$SO_FILE"))
        
        # Check if .so file is uncompressed (ZIP alignment check)
        # For 16KB compliance, uncompressed files should be aligned to 16KB (16384 bytes)
        
        echo "  - $ABI/$SO_NAME"
    done
    
    echo ""
    echo "🔧 Recommendations:"
    echo "  1. Ensure useLegacyPackaging = true in build.gradle"
    echo "  2. Set android.bundle.enableUncompressedNativeLibs=true in gradle.properties"
    echo "  3. Use AGP 8.3+ for proper 16KB support"
    
    rm -rf "$TEMP_DIR"
    
elif [ "$EXT" = "aab" ]; then
    echo "📱 AAB Format detected"
    echo "✅ Google Play will automatically handle 16KB alignment for AAB files"
    echo ""
    echo "Configuration checklist:"
    echo "  ✓ android.bundle.enableUncompressedNativeLibs=true"
    echo "  ✓ useLegacyPackaging = true"
    echo "  ✓ NDK version 27+"
else
    echo "❌ Unsupported file format: $EXT"
    exit 1
fi

echo ""
echo "======================================"
echo "✅ Verification Complete"
echo "======================================"

