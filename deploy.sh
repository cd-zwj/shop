#!/bin/bash

# 支付系统部署脚本

echo "=========================================="
echo "支付系统部署脚本"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}错误: 未安装 Node.js${NC}"
    exit 1
fi

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}错误: 未安装 Maven${NC}"
    exit 1
fi

# 检查 Nginx
if ! command -v nginx &> /dev/null; then
    echo -e "${YELLOW}警告: 未安装 Nginx，请手动安装${NC}"
fi

echo ""
echo "1. 构建后端项目..."
cd payment-system
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}后端构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}后端构建成功${NC}"
cd ..

echo ""
echo "2. 构建商家管理后台..."
cd payment-frontend-admin
npm install
npm run build
if [ $? -ne 0 ]; then
    echo -e "${RED}商家管理后台构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}商家管理后台构建成功${NC}"
cd ..

echo ""
echo "3. 构建用户端应用..."
cd payment-frontend-user
npm install
npm run build
if [ $? -ne 0 ]; then
    echo -e "${RED}用户端应用构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}用户端应用构建成功${NC}"
cd ..

echo ""
echo "4. 部署到 Nginx..."

# 创建 Nginx 目录
NGINX_HTML_DIR="/usr/share/nginx/html"
if [ ! -d "$NGINX_HTML_DIR" ]; then
    NGINX_HTML_DIR="./nginx/html"
    mkdir -p $NGINX_HTML_DIR
fi

# 复制前端文件
echo "复制商家管理后台..."
mkdir -p $NGINX_HTML_DIR/admin
cp -r payment-frontend-admin/dist/* $NGINX_HTML_DIR/admin/

echo "复制用户端应用..."
mkdir -p $NGINX_HTML_DIR/user
cp -r payment-frontend-user/dist/* $NGINX_HTML_DIR/user/

# 复制 Nginx 配置
if [ -f "/etc/nginx/nginx.conf" ]; then
    echo "备份原 Nginx 配置..."
    sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup
    echo "复制新 Nginx 配置..."
    sudo cp nginx.conf /etc/nginx/nginx.conf
    echo "重启 Nginx..."
    sudo nginx -t && sudo nginx -s reload
    echo -e "${GREEN}Nginx 配置已更新${NC}"
else
    echo -e "${YELLOW}请手动配置 Nginx，配置文件位于: nginx.conf${NC}"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}部署完成！${NC}"
echo "=========================================="
echo ""
echo "访问地址："
echo "  商家管理后台: http://localhost (或 http://admin.payment.local)"
echo "  用户端应用:   http://user.payment.local"
echo ""
echo "后端服务："
echo "  启动命令: cd payment-system && java -jar target/payment-system-1.0.0.jar"
echo "  访问地址: http://localhost:8080"
echo ""
