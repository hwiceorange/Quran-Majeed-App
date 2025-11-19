#!/bin/bash

# 检查部署文件准备状态
# 使用方法: ./check_deployment_files.sh

echo "🔍 检查 Tafsir API 部署文件状态..."
echo "========================================="

PROJECT_DIR="/Users/huwei/AndroidStudioProjects/quran0"
DEPLOY_DIR="$PROJECT_DIR/server_deploy"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_file() {
    local file_path="$1"
    local file_name=$(basename "$file_path")
    
    if [ -f "$file_path" ]; then
        local file_size=$(ls -lh "$file_path" | awk '{print $5}')
        echo -e "${GREEN}✅${NC} $file_name (${file_size})"
        
        # 检查数据库配置
        if [[ "$file_name" == "index.php" ]] || [[ "$file_name" == "test_db_connection.php" ]]; then
            if grep -q "your_password_here\|YOUR_PASSWORD\|your_db_user\|YOUR_USERNAME" "$file_path"; then
                echo -e "   ${YELLOW}⚠️  警告: 数据库配置尚未修改${NC}"
                return 1
            else
                echo -e "   ${GREEN}✓ 数据库配置已修改${NC}"
            fi
        fi
        return 0
    else
        echo -e "${RED}❌${NC} $file_name (未找到)"
        return 1
    fi
}

# 检查目录
echo ""
echo "📂 检查部署目录..."
if [ -d "$DEPLOY_DIR" ]; then
    echo -e "${GREEN}✅${NC} server_deploy/ 目录存在"
else
    echo -e "${RED}❌${NC} server_deploy/ 目录不存在"
    exit 1
fi

# 检查必需文件
echo ""
echo "📄 检查必需文件..."

check_file "$DEPLOY_DIR/index.php"
php_status=$?

check_file "$DEPLOY_DIR/.htaccess"
htaccess_status=$?

check_file "$DEPLOY_DIR/test_db_connection.php"
test_status=$?

check_file "$PROJECT_DIR/tafsir_indonesian_complete.sql"
sql_status=$?

# 总结
echo ""
echo "========================================="
echo "📊 检查总结"
echo "========================================="

all_ready=true

if [ $php_status -eq 0 ] && [ $htaccess_status -eq 0 ] && [ $test_status -eq 0 ]; then
    echo -e "${GREEN}✅ 所有必需文件已准备就绪${NC}"
else
    echo -e "${RED}❌ 有文件缺失或未配置${NC}"
    all_ready=false
fi

if [ $sql_status -eq 0 ]; then
    echo -e "${GREEN}✅ SQL 数据文件已准备${NC}"
else
    echo -e "${RED}❌ SQL 数据文件缺失${NC}"
    all_ready=false
fi

# 检查数据库配置
echo ""
echo "🔍 检查数据库配置..."
if grep -q "your_password_here\|YOUR_PASSWORD\|your_db_user\|YOUR_USERNAME" "$DEPLOY_DIR/index.php" 2>/dev/null; then
    echo -e "${YELLOW}⚠️  index.php 数据库配置尚未修改${NC}"
    all_ready=false
else
    echo -e "${GREEN}✅ index.php 数据库配置已修改${NC}"
fi

if grep -q "your_password_here\|YOUR_PASSWORD\|your_db_user\|YOUR_USERNAME" "$DEPLOY_DIR/test_db_connection.php" 2>/dev/null; then
    echo -e "${YELLOW}⚠️  test_db_connection.php 数据库配置尚未修改${NC}"
    all_ready=false
else
    echo -e "${GREEN}✅ test_db_connection.php 数据库配置已修改${NC}"
fi

# 最终状态
echo ""
echo "========================================="
if [ "$all_ready" = true ]; then
    echo -e "${GREEN}🎉 所有文件已准备完毕，可以上传到服务器！${NC}"
    echo ""
    echo "下一步："
    echo "1. 登录 Hostinger 文件管理器"
    echo "2. 导航到 /public_html/quran/apis/tafsirs/"
    echo "3. 上传以下文件："
    echo "   - server_deploy/index.php"
    echo "   - server_deploy/.htaccess"
    echo "   - server_deploy/test_db_connection.php"
    echo "4. 在浏览器测试："
    echo "   https://apis.dochubai.com/quran/apis/tafsirs/test_db_connection.php"
else
    echo -e "${YELLOW}⚠️  请先完成以下操作：${NC}"
    echo ""
    if grep -q "your_password_here\|YOUR_PASSWORD" "$DEPLOY_DIR/index.php" 2>/dev/null; then
        echo "1. 修改 server_deploy/index.php 的数据库配置（第 26-29 行）"
    fi
    if grep -q "your_password_here\|YOUR_PASSWORD" "$DEPLOY_DIR/test_db_connection.php" 2>/dev/null; then
        echo "2. 修改 server_deploy/test_db_connection.php 的数据库配置（第 14-17 行）"
    fi
    echo ""
    echo "提示: 在 Hostinger hPanel → 数据库 → 查看 MySQL 数据库信息"
fi
echo "========================================="

