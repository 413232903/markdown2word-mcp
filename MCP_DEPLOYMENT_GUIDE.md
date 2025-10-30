# md2doc MCP 服务部署指南

## 目录
- [环境要求](#环境要求)
- [本地开发部署](#本地开发部署)
- [远程服务器部署](#远程服务器部署)
- [MCP 客户端配置](#mcp-客户端配置)
- [测试验证](#测试验证)
- [故障排除](#故障排除)

---

## 环境要求

### 必需软件
- **Java 17** (项目要求)
- **Maven 3.8.1+**
- **Git** (用于克隆和版本控制)

### 检查当前环境
```bash
java -version  # 应显示 Java 17
mvn -version   # 应显示 Maven 3.8.1+
```

### 安装 Java 17 (如果需要)

#### macOS
```bash
# 使用 Homebrew
brew install openjdk@17

# 设置环境变量
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
```

#### Linux
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel
```

#### Windows
从 [Adoptium](https://adoptium.net/) 下载并安装 Java 17。

---

## 本地开发部署

### 1. 构建项目

```bash
# 在项目根目录
cd /path/to/md2doc-plus

# 清理并编译整个项目
mvn clean install
```

### 2. 启动 MCP 服务

```bash
# 方式 1: 使用 Maven 启动
cd md2doc-service
mvn spring-boot:run

# 方式 2: 使用 JAR 启动
java -jar md2doc-service/target/md2doc-service-1.0.jar

# 方式 3: 后台运行
nohup java -jar md2doc-service/target/md2doc-service-1.0.jar > logs/mcp-server.log 2>&1 &
```

### 3. 验证服务启动

```bash
# 检查服务是否运行
curl http://localhost:8080/actuator/health

# 检查 MCP 端点
curl -N -H "Accept: text/event-stream" http://localhost:8080/mcp/messages
```

服务默认运行在:
- REST API: `http://localhost:8080`
- MCP SSE 端点: `http://localhost:8080/mcp/messages`

---

## 远程服务器部署

### 部署架构

对于远程大模型调用,推荐使用以下部署方式:

```
远程大模型 (如 Claude API)
    ↓ HTTPS
云服务器 (公网 IP)
    ↓ MCP over HTTPS
md2doc MCP 服务
```

### 1. 准备云服务器

推荐配置:
- **云平台**: 阿里云、腾讯云、AWS、Azure 等
- **操作系统**: Ubuntu 20.04+ / CentOS 7+
- **配置**: 2核 4GB 内存 (最低 1核 2GB)
- **网络**: 公网 IP,开放 8080 或 80/443 端口

### 2. 服务器环境配置

```bash
# 1. 更新系统
sudo apt update && sudo apt upgrade -y

# 2. 安装 Java 17
sudo apt install openjdk-17-jdk -y

# 3. 安装 Maven (可选,用于构建)
sudo apt install maven -y

# 4. 创建应用目录
sudo mkdir -p /opt/md2doc-plus
sudo chown $(whoami):$(whoami) /opt/md2doc-plus

# 5. 创建日志目录
sudo mkdir -p /var/log/md2doc
sudo chown $(whoami):$(whoami) /var/log/md2doc
```

### 3. 部署应用文件

#### 方式 A: 直接上传 JAR (推荐)

```bash
# 本地构建
mvn clean package -DskipTests

# 上传到服务器
scp md2doc-service/target/md2doc-service-1.0.jar user@your-server:/opt/md2doc-plus/
scp md2doc-service/src/main/resources/application.yml user@your-server:/opt/md2doc-plus/
```

#### 方式 B: Git 克隆并构建

```bash
# 在服务器上
cd /opt/md2doc-plus
git clone <your-repo-url> .
mvn clean package -DskipTests
```

### 4. 配置生产环境

创建生产配置文件 `/opt/md2doc-plus/application-prod.yml`:

```yaml
server:
  port: 8080
  # 如果使用域名和 HTTPS,配置如下:
  # ssl:
  #   enabled: true
  #   key-store: /path/to/keystore.p12
  #   key-store-password: your-password
  #   key-store-type: PKCS12

spring:
  application:
    name: md2doc-service

  ai:
    mcp:
      server:
        name: md2doc-mcp-server
        version: "1.0"
        instructions: Markdown to Word Document Conversion MCP Server
        type: SYNC
        sse-message-endpoint: /mcp/messages

logging:
  level:
    cn.daydayup.dev.md2doc: INFO
    org.springframework.ai.mcp: INFO
  file:
    name: /var/log/md2doc/application.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 5. 创建系统服务 (Systemd)

创建服务文件 `/etc/systemd/system/md2doc-mcp.service`:

```ini
[Unit]
Description=md2doc MCP Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/md2doc-plus
ExecStart=/usr/bin/java -jar /opt/md2doc-plus/md2doc-service-1.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=md2doc-mcp

# JVM 参数
Environment="JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC"

[Install]
WantedBy=multi-user.target
```

启用并启动服务:

```bash
# 重载 systemd 配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start md2doc-mcp

# 设置开机自启
sudo systemctl enable md2doc-mcp

# 查看状态
sudo systemctl status md2doc-mcp

# 查看日志
sudo journalctl -u md2doc-mcp -f
```

### 6. 配置防火墙

```bash
# Ubuntu (UFW)
sudo ufw allow 8080/tcp
sudo ufw reload

# CentOS (Firewalld)
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# 云服务器安全组
# 在云控制台添加入站规则: TCP 8080
```

### 7. 配置反向代理 (可选但推荐)

使用 Nginx 提供 HTTPS 支持:

```bash
# 安装 Nginx
sudo apt install nginx -y
```

创建 Nginx 配置 `/etc/nginx/sites-available/md2doc-mcp`:

```nginx
upstream md2doc_backend {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name your-domain.com;  # 替换为你的域名

    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 证书 (使用 Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # MCP SSE 端点
    location /mcp/ {
        proxy_pass http://md2doc_backend;
        proxy_http_version 1.1;

        # SSE 必需的头部
        proxy_set_header Connection '';
        proxy_set_header Cache-Control 'no-cache';
        proxy_set_header X-Accel-Buffering 'no';

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_read_timeout 1800s;
        proxy_send_timeout 1800s;

        # 禁用缓冲
        proxy_buffering off;
        chunked_transfer_encoding off;
    }

    # REST API 端点
    location / {
        proxy_pass http://md2doc_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置:

```bash
sudo ln -s /etc/nginx/sites-available/md2doc-mcp /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

获取免费 SSL 证书:

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

---

## MCP 客户端配置

### Claude Desktop 配置

#### 本地服务

编辑 Claude Desktop 配置文件:
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "md2doc-local": {
      "command": "java",
      "args": [
        "-jar",
        "/opt/md2doc-plus/md2doc-service-1.0.jar",
        "--server.port=9090"
      ],
      "env": {
        "SPRING_PROFILES_ACTIVE": "mcp"
      }
    }
  }
}
```

#### 远程服务 (HTTP)

```json
{
  "mcpServers": {
    "md2doc-remote": {
      "command": "curl",
      "args": [
        "-N",
        "-H", "Accept: text/event-stream",
        "http://your-server-ip:8080/mcp/messages"
      ]
    }
  }
}
```

#### 远程服务 (HTTPS + 域名)

```json
{
  "mcpServers": {
    "md2doc-secure": {
      "command": "curl",
      "args": [
        "-N",
        "-H", "Accept: text/event-stream",
        "https://your-domain.com/mcp/messages"
      ]
    }
  }
}
```

### 其他 MCP 客户端

对于支持 SSE 的 MCP 客户端,直接连接到:
- 本地: `http://localhost:8080/mcp/messages`
- 远程: `https://your-domain.com/mcp/messages`

---

## 测试验证

### 1. 测试 REST API

```bash
# 测试健康检查
curl http://localhost:8080/actuator/health

# 测试文本转换
curl -X POST http://localhost:8080/api/markdown/convert/text \
  -H "Content-Type: application/json" \
  -d '{"content": "# 测试标题\n\n这是测试内容"}' \
  -o test-output.docx
```

### 2. 测试 MCP 连接

```bash
# 测试 SSE 端点
curl -N -H "Accept: text/event-stream" \
  http://localhost:8080/mcp/messages
```

应该看到 SSE 连接建立并保持打开。

### 3. 在 Claude Desktop 中测试

重启 Claude Desktop,然后在对话中询问:

```
你有哪些可用的工具?
```

应该能看到 md2doc 相关的工具列表。

### 4. 功能测试

```
请帮我将以下 Markdown 转换为 Word:

# 项目报告
## 1. 概述
这是一个测试报告。

## 2. 数据
| 指标 | 数值 |
|------|------|
| 用户数 | 1000 |
| 收入 | 50000 |
```

---

## 故障排除

### 问题 1: 编译失败 - Java 版本不匹配

**错误信息:**
```
Fatal error compiling: java.lang.ExceptionInInitializerError
```

**解决方案:**
```bash
# 确认使用 Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn clean compile
```

### 问题 2: 端口被占用

**错误信息:**
```
Port 8080 was already in use
```

**解决方案:**
```bash
# 查找占用进程
lsof -i :8080

# 杀死进程
kill -9 <PID>

# 或使用其他端口
java -jar md2doc-service-1.0.jar --server.port=9090
```

### 问题 3: MCP 连接失败

**可能原因:**
1. 防火墙阻止
2. SSE 端点路径错误
3. 网络连接问题

**解决方案:**
```bash
# 检查服务是否运行
sudo systemctl status md2doc-mcp

# 检查端口监听
sudo netstat -tlnp | grep 8080

# 检查日志
sudo journalctl -u md2doc-mcp -n 100

# 测试 SSE 端点
curl -v -N -H "Accept: text/event-stream" \
  http://localhost:8080/mcp/messages
```

### 问题 4: 内存不足

**错误信息:**
```
java.lang.OutOfMemoryError: Java heap space
```

**解决方案:**
调整 JVM 内存参数:

```bash
# 直接启动
java -Xms512m -Xmx2g -jar md2doc-service-1.0.jar

# 在 systemd 服务中修改
Environment="JAVA_OPTS=-Xms512m -Xmx2g"
```

### 问题 5: SSL 证书问题

**错误信息:**
```
PKIX path building failed
```

**解决方案:**
```bash
# 导入证书到 Java 信任库
sudo keytool -import -alias md2doc-cert \
  -file /path/to/cert.crt \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit
```

### 查看详细日志

```bash
# 应用日志
tail -f /var/log/md2doc/application.log

# 系统日志
sudo journalctl -u md2doc-mcp -f

# 增加日志级别
# 在 application.yml 中设置:
# logging.level.cn.daydayup.dev.md2doc: DEBUG
```

---

## 监控和维护

### 日志管理

```bash
# 日志轮转配置 /etc/logrotate.d/md2doc
/var/log/md2doc/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 0644 ubuntu ubuntu
}
```

### 性能监控

使用 Spring Boot Actuator:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info,prometheus
  endpoint:
    health:
      show-details: always
```

访问监控端点:
- 健康检查: `http://localhost:8080/actuator/health`
- 指标数据: `http://localhost:8080/actuator/metrics`
- Prometheus: `http://localhost:8080/actuator/prometheus`

### 备份策略

```bash
# 备份应用
tar -czf md2doc-backup-$(date +%Y%m%d).tar.gz \
  /opt/md2doc-plus \
  /var/log/md2doc \
  /etc/systemd/system/md2doc-mcp.service
```

---

## 安全建议

1. **启用 HTTPS**: 生产环境必须使用 HTTPS
2. **API 认证**: 添加 API Key 或 OAuth2 认证
3. **限流**: 使用 Spring Security 或 Nginx 限流
4. **防火墙**: 只开放必要端口
5. **定期更新**: 及时更新依赖和安全补丁

```yaml
# 添加 API 认证示例
spring:
  security:
    user:
      name: admin
      password: change-this-password
```

---

## 参考资源

- [Spring AI MCP 文档](https://docs.spring.io/spring-ai/reference/api/mcp.html)
- [Model Context Protocol 规范](https://spec.modelcontextprotocol.io/)
- [Apache POI 文档](https://poi.apache.org/)
- [Spring Boot 部署指南](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)

---

## 联系支持

如遇到问题:
1. 检查日志文件
2. 查看 GitHub Issues
3. 提交新 Issue 并附上详细日志

**祝部署顺利!** 🚀
