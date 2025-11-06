#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}╔═══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║     VMess/Hysteria2 代理服务器安装脚本        ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════╝${NC}"
echo ""

if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}错误: 请使用root权限运行此脚本${NC}"
    echo "使用命令: sudo bash install.sh"
    exit 1
fi

if ! command -v systemctl &> /dev/null; then
    echo -e "${RED}错误: 此脚本需要systemd支持${NC}"
    exit 1
fi

echo -e "${YELLOW}[1/5]${NC} 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "正在安装Java 17..."
    if command -v apt-get &> /dev/null; then
        apt-get update -qq
        apt-get install -y openjdk-17-jre-headless curl
    elif command -v yum &> /dev/null; then
        yum install -y java-17-openjdk-headless curl
    else
        echo -e "${RED}不支持的系统，请手动安装Java 17${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Java已安装${NC}"
fi

echo -e "${YELLOW}[2/5]${NC} 创建安装目录..."
mkdir -p /opt/proxy-server
cd /opt/proxy-server

echo -e "${YELLOW}[3/5]${NC} 下载最新版本..."
REPO="YOUR_USERNAME/java_vmess"
LATEST_URL=$(curl -s "https://api.github.com/repos/${REPO}/releases/latest" | grep "browser_download_url.*server.jar" | cut -d '"' -f 4)

if [ -z "$LATEST_URL" ]; then
    echo -e "${RED}错误: 无法获取下载链接，请检查仓库名称${NC}"
    exit 1
fi

curl -L -o server.jar "$LATEST_URL"
echo -e "${GREEN}✓ 下载完成${NC}"

echo -e "${YELLOW}[4/5]${NC} 配置系统服务..."
cat > /etc/systemd/system/proxy-server.service << 'EOFSVC'
[Unit]
Description=VMess/Hysteria2 Proxy Server
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/proxy-server
ExecStart=/usr/bin/java -Xms256M -Xmx512M -jar /opt/proxy-server/server.jar
Restart=on-failure
RestartSec=10s
StandardOutput=journal
StandardError=journal
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOFSVC

systemctl daemon-reload
systemctl enable proxy-server
echo -e "${GREEN}✓ 服务配置完成${NC}"

echo -e "${YELLOW}[5/5]${NC} 启动服务..."
systemctl start proxy-server

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              安装完成！                       ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════╝${NC}"
echo ""
echo "等待服务启动..."
sleep 8

echo ""
echo -e "${YELLOW}代理链接:${NC}"
journalctl -u proxy-server --no-pager -n 100 | grep -A 10 "代理链接" || echo "请使用以下命令查看: journalctl -u proxy-server -f"

echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看状态: systemctl status proxy-server"
echo "  查看日志: journalctl -u proxy-server -f"
echo "  重启服务: systemctl restart proxy-server"
echo "  停止服务: systemctl stop proxy-server"
echo ""
echo -e "${YELLOW}端口:${NC}"
echo "  VMess: 10086"
echo "  Hysteria2: 36712"
echo ""
echo -e "${YELLOW}防火墙设置 (如需要):${NC}"
echo "  ufw allow 10086/tcp"
echo "  ufw allow 36712/udp"
echo ""
