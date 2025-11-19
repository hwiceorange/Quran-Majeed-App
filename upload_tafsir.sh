#!/bin/bash

# 📤 Tafsir 清单文件上传脚本
# 用途: 上传 available_tafsirs_info.json 到 dochubai.com 服务器

set -e

echo "📤 Tafsir 清单文件上传脚本"
echo "================================"
echo ""

# 检查文件是否存在
if [ ! -f "available_tafsirs_server.json" ]; then
    echo "❌ 错误: available_tafsirs_server.json 文件不存在"
    exit 1
fi

echo "✅ 找到本地文件: available_tafsirs_server.json"
echo ""

# 显示文件内容预览
echo "📄 文件内容预览："
echo "================================"
head -n 10 available_tafsirs_server.json
echo "... (省略其余部分)"
echo "================================"
echo ""

# 选择上传方式
echo "请选择上传方式："
echo "1) SCP (需要 SSH 访问)"
echo "2) SFTP (需要 SFTP 访问)"
echo "3) FTP (需要 FTP 访问)"
echo "4) 仅显示手动上传说明"
echo "5) 测试 API 连接（不上传）"
echo ""

read -p "请输入选项 (1-5): " choice

case $choice in
    1)
        echo ""
        echo "📡 使用 SCP 上传"
        echo "================================"
        read -p "服务器地址 (例如: user@dochubai.com): " server
        read -p "目标路径 (例如: /var/www/html/quran/apis/tafsirs/): " target_dir
        
        target_file="${target_dir}/available_tafsirs_info.json"
        
        echo ""
        echo "正在上传到: ${server}:${target_file}"
        scp available_tafsirs_server.json "${server}:${target_file}"
        
        echo ""
        echo "✅ 上传完成！"
        echo ""
        echo "设置文件权限..."
        ssh "${server}" "chmod 644 ${target_file} && chown www-data:www-data ${target_file} 2>/dev/null || true"
        
        echo "✅ 权限设置完成！"
        ;;
        
    2)
        echo ""
        echo "📡 使用 SFTP 上传"
        echo "================================"
        read -p "服务器地址 (例如: dochubai.com): " server
        read -p "用户名: " username
        read -p "目标路径 (例如: /var/www/html/quran/apis/tafsirs/): " target_dir
        
        target_file="${target_dir}/available_tafsirs_info.json"
        
        echo ""
        echo "连接到 SFTP..."
        sftp "${username}@${server}" << EOF
cd ${target_dir}
put available_tafsirs_server.json available_tafsirs_info.json
chmod 644 available_tafsirs_info.json
bye
EOF
        
        echo "✅ 上传完成！"
        ;;
        
    3)
        echo ""
        echo "📡 使用 FTP 上传"
        echo "================================"
        read -p "FTP 服务器地址: " ftp_server
        read -p "用户名: " ftp_user
        read -s -p "密码: " ftp_pass
        echo ""
        read -p "目标目录 (例如: /public_html/quran/apis/tafsirs/): " ftp_dir
        
        echo ""
        echo "正在上传..."
        
        ftp -n "${ftp_server}" << EOF
user ${ftp_user} ${ftp_pass}
cd ${ftp_dir}
put available_tafsirs_server.json available_tafsirs_info.json
chmod 644 available_tafsirs_info.json
bye
EOF
        
        echo "✅ 上传完成！"
        ;;
        
    4)
        echo ""
        echo "📋 手动上传说明"
        echo "================================"
        echo ""
        echo "1. 打开您的 FTP 客户端或 cPanel 文件管理器"
        echo "2. 连接到 dochubai.com"
        echo "3. 导航到: /quran/apis/tafsirs/"
        echo "4. 上传文件: available_tafsirs_server.json"
        echo "5. 重命名为: available_tafsirs_info.json"
        echo "6. 设置权限为: 644"
        echo ""
        echo "目标 URL: https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json"
        echo ""
        ;;
        
    5)
        echo ""
        echo "🔍 测试 API 连接"
        echo "================================"
        
        test_url="https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json"
        
        echo "测试 URL: ${test_url}"
        echo ""
        
        if command -v curl &> /dev/null; then
            echo "使用 curl 测试..."
            response=$(curl -s -o /dev/null -w "%{http_code}" "${test_url}")
            
            if [ "$response" == "200" ]; then
                echo "✅ 文件已存在且可访问 (HTTP ${response})"
                echo ""
                echo "文件内容："
                curl -s "${test_url}" | head -n 20
            elif [ "$response" == "404" ]; then
                echo "⚠️  文件不存在 (HTTP ${response})"
                echo "需要上传文件到服务器"
            else
                echo "❌ 无法访问 (HTTP ${response})"
            fi
        else
            echo "❌ curl 未安装，无法测试"
        fi
        
        exit 0
        ;;
        
    *)
        echo "❌ 无效选项"
        exit 1
        ;;
esac

echo ""
echo "================================"
echo "🎉 操作完成！"
echo ""
echo "🔍 验证上传："
echo "--------------------------------"
echo "在浏览器中打开："
echo "https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json"
echo ""
echo "或使用命令验证："
echo "curl https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json"
echo ""
echo "🧪 测试应用："
echo "--------------------------------"
echo "1. 重新安装应用"
echo "2. 设置语言为印尼语"
echo "3. 打开 Tafsir（注释）"
echo "4. 检查日志："
echo "   adb logcat | grep TafsirManager"
echo ""
echo "期待看到："
echo "   ✅ Network load successful"
echo "   ✅ Parsed 3 language groups"
echo "   ✅ Auto-selected Tafsir: id-tafsir-kemenag"
echo ""
echo "================================"

