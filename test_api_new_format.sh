#!/bin/bash

# 测试新格式的 Tafsir API
# 使用方法: ./test_api_new_format.sh

echo "🧪 测试新格式的 Tafsir API..."
echo "========================================="
echo ""

# 测试 URL
BASE_URL="https://apis.dochubai.com/quran/apis/tafsirs"
NEW_FORMAT="${BASE_URL}/index.php"

echo "📍 API 格式: GET 参数方式"
echo "   格式: index.php?slug={slug}&ayah={ayah}"
echo ""

# 测试用例
declare -a tests=(
    "id-tafsir-kemenag:1:1"
    "id-tafsir-kemenag:1:2"
    "id-tafsir-kemenag:2:255"
    "id-tafsir-kemenag:3:2"
)

success_count=0
fail_count=0

for test in "${tests[@]}"; do
    IFS=':' read -r slug surah ayah <<< "$test"
    
    echo "-------------------------------------------"
    echo "🔍 测试: Surah $surah, Ayah $ayah"
    
    url="${NEW_FORMAT}?slug=${slug}&ayah=${surah}:${ayah}"
    echo "   URL: $url"
    
    # 发送请求
    response=$(curl -k -s -w "\n%{http_code}" "$url" 2>&1)
    
    # 分离响应体和状态码
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    # 检查状态码
    if [ "$http_code" == "200" ]; then
        # 检查是否包含 tafsir
        if echo "$body" | grep -q '"tafsir"'; then
            echo "   ✅ 成功: HTTP $http_code"
            # 显示前100个字符的内容
            preview=$(echo "$body" | jq -r '.tafsir.text' 2>/dev/null | head -c 100)
            if [ -n "$preview" ]; then
                echo "   📝 内容预览: ${preview}..."
            fi
            ((success_count++))
        else
            echo "   ❌ 失败: 响应中没有 tafsir 字段"
            echo "   📄 响应: $body"
            ((fail_count++))
        fi
    else
        echo "   ❌ 失败: HTTP $http_code"
        echo "   📄 响应: $body"
        ((fail_count++))
    fi
    echo ""
done

echo "========================================="
echo "📊 测试总结"
echo "========================================="
echo "✅ 成功: $success_count"
echo "❌ 失败: $fail_count"
echo ""

if [ $fail_count -eq 0 ]; then
    echo "🎉 所有测试通过！API 工作正常！"
    echo ""
    echo "下一步："
    echo "1. 编译应用 (Android Studio 或 ./gradlew :app:assembleDebug)"
    echo "2. 安装到设备"
    echo "3. 测试印尼语 Tafsir 功能"
else
    echo "⚠️  有测试失败，请检查："
    echo "1. index.php 是否已正确上传？"
    echo "2. 数据库配置是否正确？"
    echo "3. 数据是否已完整导入？"
fi

echo "========================================="

