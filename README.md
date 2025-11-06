# java_vmess

自动部署 VMess 和 Hysteria2 代理服务器，类似 PaperMC 的构建流程。

## 特性

- ✅ 自动安装 Xray-core 和 Hysteria2
- ✅ 自动生成配置和链接
- ✅ systemd 开机自启
- ✅ GitHub Actions 自动构建
- ✅ 一键安装部署

## 快速开始

### 一键安装

```bash
curl -fsSL https://raw.githubusercontent.com/YOUR_USERNAME/java_vmess/main/install.sh | sudo bash
```

### 手动安装

1. **下载最新版本**

```bash
wget https://github.com/YOUR_USERNAME/java_vmess/releases/latest/download/server.jar
```

2. **运行**

```bash
sudo java -jar server.jar
```

3. **配置开机自启**

```bash
# 创建目录
sudo mkdir -p /opt/proxy-server
sudo cp server.jar /opt/proxy-server/

# 下载并安装服务
sudo curl -o /etc/systemd/system/proxy-server.service \
  https://raw.githubusercontent.com/YOUR_USERNAME/java_vmess/main/proxy-server.service

# 启用服务
sudo systemctl daemon-reload
sudo systemctl enable proxy-server
sudo systemctl start proxy-server
```

## 管理命令

```bash
# 查看服务状态
systemctl status proxy-server

# 查看实时日志
journalctl -u proxy-server -f

# 查看代理链接
journalctl -u proxy-server -n 100 | grep -A 10 "代理链接"

# 重启服务
systemctl restart proxy-server

# 停止服务
systemctl stop proxy-server

# 禁用开机自启
systemctl disable proxy-server
```

## 配置说明

### 端口

- VMess: `10086/tcp`
- Hysteria2: `36712/udp`

### 配置文件位置

- VMess 配置: `/etc/proxy-server/vmess.json`
- Hysteria2 配置: `/etc/proxy-server/hysteria2.yaml`
- Hysteria2 密码: `/etc/proxy-server/hy2_password.txt`
- TLS 证书: `/etc/proxy-server/cert.pem` 和 `/etc/proxy-server/key.pem`

### 防火墙设置

```bash
# UFW
sudo ufw allow 10086/tcp
sudo ufw allow 36712/udp

# firewalld
sudo firewall-cmd --permanent --add-port=10086/tcp
sudo firewall-cmd --permanent --add-port=36712/udp
sudo firewall-cmd --reload

# iptables
sudo iptables -A INPUT -p tcp --dport 10086 -j ACCEPT
sudo iptables -A INPUT -p udp --dport 36712 -j ACCEPT
```

## 开发

### 环境要求

- JDK 21+
- Gradle 8.5+

### 本地构建

```bash
# 克隆仓库
git clone https://github.com/YOUR_USERNAME/java_vmess.git
cd java_vmess

# 构建
./gradlew clean build

# 生成的JAR文件
ls -lh build/libs/server.jar
```

### 发布 Release

推送标签会自动触发 GitHub Actions 构建并发布：

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 项目结构

```
java_vmess/
├── .github/
│   └── workflows/
│       └── release.yml          # GitHub Actions 工作流
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── vmess/
│                   └── ProxyServer.java
├── build.gradle                 # Gradle 构建配置
├── settings.gradle
├── gradlew                      # Gradle Wrapper (Unix)
├── gradlew.bat                  # Gradle Wrapper (Windows)
├── proxy-server.service         # systemd 服务文件
├── install.sh                   # 一键安装脚本
└── README.md
```

## 使用说明

1. 服务启动后会自动输出 VMess 和 Hysteria2 链接
2. 复制链接导入到客户端使用
3. VMess 使用 TCP 传输
4. Hysteria2 使用自签名证书（客户端需启用 insecure）

## 客户端推荐

- **Windows/macOS/Linux**: [v2rayN](https://github.com/2dust/v2rayN) / [NekoRay](https://github.com/MatsuriDayo/nekoray)
- **Android**: [v2rayNG](https://github.com/2dust/v2rayNG)
- **iOS**: Shadowrocket / Surge

## 故障排查

### 服务无法启动

```bash
# 查看详细日志
journalctl -u proxy-server -xe

# 检查端口占用
netstat -tunlp | grep -E '10086|36712'

# 手动测试
sudo java -jar /opt/proxy-server/server.jar
```

### 连接失败

1. 检查防火墙是否开放端口
2. 检查云服务商安全组规则
3. 确认服务器 IP 地址正确
4. 查看服务日志排查错误

## 卸载

```bash
# 停止并禁用服务
sudo systemctl stop proxy-server
sudo systemctl disable proxy-server

# 删除文件
sudo rm -f /etc/systemd/system/proxy-server.service
sudo rm -rf /opt/proxy-server
sudo rm -rf /etc/proxy-server

# 重载 systemd
sudo systemctl daemon-reload
```

## License

MIT License

## 免责声明

本项目仅供学习交流使用，请遵守当地法律法规。
