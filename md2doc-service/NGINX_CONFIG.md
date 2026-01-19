# md2doc MCP 服务反向代理配置指南

## 问题诊断结果

### 已确认的问题

1. **SSE 连接被提前关闭**
   - 反向代理返回 `Connection: close`
   - 导致 MCP 客户端无法保持长连接

2. **MCP 客户端 POST 请求失败**
   - 客户端尝试向 SSE 端点发送 POST 请求
   - SSE 端点只支持 GET（单向推送）

3. **协议版本警告（非致命）**
   - 客户端请求协议版本 2025-06-18
   - 服务端建议使用 2024-11-05

---

## 必需的 Nginx/Tengine 配置

### 完整配置示例

```nginx
# MCP SSE 端点配置 - md2doc 服务
location /dataReport/ {
    # 转发到后端服务
    proxy_pass http://192.9.253.106:8080/dataReport/;

    # HTTP/1.1 必需（SSE 需要）
    proxy_http_version 1.1;

    # 关键配置：清空 Connection 头，让后端决定连接方式
    proxy_set_header Connection "";

    # 其他必需头
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # SSE 必需配置
    proxy_buffering off;        # 禁用缓冲
    proxy_cache off;           # 禁用缓存
    proxy_read_timeout 86400s;  # 读取超时 24 小时
    proxy_send_timeout 86400s;  # 发送超时 24 小时

    # SSE 不支持压缩
    gzip off;

    # 禁用分块传输编码（某些配置下需要）
    chunked_transfer_encoding off;
}

# REST API 配置 - md2doc 服务
location /api/markdown/ {
    proxy_pass http://192.9.253.106:8080/api/markdown/;

    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # REST API 可以缓存（可选）
    proxy_buffering off;
    proxy_read_timeout 60s;
    proxy_send_timeout 60s;
}
```

### 配置说明

| 配置项 | 作用 | 必需 |
|--------|------|------|
| `proxy_http_version 1.1` | SSE 需要的 HTTP 版本 | ✅ |
| `proxy_set_header Connection ""` | 最关键：防止连接被关闭 | ✅ |
| `proxy_buffering off` | SSE 必须禁用缓冲 | ✅ |
| `proxy_cache off` | SSE 必须禁用缓存 | ✅ |
| `proxy_read_timeout 86400s` | 保持连接 24 小时 | ✅ |
| `proxy_send_timeout 86400s` | 保持发送能力 24 小时 | ✅ |
| `gzip off` | SSE 不支持压缩 | ✅ |

---

## 应用服务器配置

### 已更新的 application.yml

```yaml
server:
  port: 8080
  # SSE 连接保持配置
  tomcat:
    threads:
      max: 200
    connection-timeout: -1  # 无限超时（SSE 需要）
    keep-alive-timeout: 86400000  # 24 小时
    max-keep-alive-requests: 1000

spring:
  ai:
    mcp:
      server:
        # SSE 端点配置
        sse-endpoint: /dataReport/md2doc
        sse-message-endpoint: /dataReport/mcp/message

        # SSE 连接配置
        sse:
          keep-alive: true
          heartbeat-interval: 15000  # 15 秒心跳
```

---

## 部署步骤

### 1. 更新 Nginx/Tengine 配置

找到配置文件（通常位置）：
- `/etc/nginx/nginx.conf`
- `/etc/nginx/conf.d/*.conf`
- `/usr/local/nginx/conf/nginx.conf`

添加上述配置。

### 2. 测试配置

```bash
# 检查配置语法
nginx -t

# 或
tengine -t
```

### 3. 重载配置

```bash
# 重载配置（不中断服务）
nginx -s reload

# 或
systemctl reload nginx

# 或
systemctl reload tengine
```

### 4. 重启 md2doc 服务

```bash
# 停止旧服务
# (使用 kill 或 systemctl)

# 启动新服务
cd /path/to/md2doc-service
java -jar target/md2doc-service-1.0.jar \
  --md2doc.download-base-url=https://mcpapi2.qjyy.com

# 或使用环境变量
export MD2DOC_DOWNLOAD_BASE_URL=https://mcpapi2.qjyy.com
java -jar target/md2doc-service-1.0.jar
```

---

## 验证测试

### 测试 1: SSE 端点连接

```bash
curl -v https://mcpapi2.qjyy.com/dataReport/md2doc --max-time 3 --insecure
```

**期望结果**:
- HTTP/1.1 200 OK
- Content-Type: text/event-stream
- Connection: keep-alive（不是 close）
- 返回 SSE 事件：`event:endpoint`

### 测试 2: SSE 连接保持

```bash
curl https://mcpapi2.qjyy.com/dataReport/md2doc --max-time 30 --insecure
```

**期望结果**: 连接保持 30 秒不中断

### 测试 3: REST API

```bash
curl -X POST https://mcpapi2.qjyy.com/api/markdown/convert/text \
  -H "Content-Type: application/json" \
  -d '{"content":"# 测试\n\n这是一个测试"}' \
  --insecure
```

**期望结果**: 返回 JSON 格式的文件下载 URL

### 测试 4: CORS 检查

```bash
curl -v -H "Origin: https://cursor.sh" \
  -H "Access-Control-Request-Method: POST" \
  -X OPTIONS https://mcpapi2.qjyy.com/dataReport/md2doc \
  --insecure
```

**期望结果**:
- HTTP/1.1 200 OK
- Access-Control-Allow-Origin: *
- Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS

---

## 常见问题排查

### 问题 1: SSE 连接立即断开

**症状**: curl 连接后立即返回

**检查**:
1. 反向代理配置中是否有 `proxy_set_header Connection "";`
2. 是否有其他中间件（如 Cloudflare）修改了连接头
3. 检查防火墙规则

### 问题 2: MCP 客户端连接失败

**症状**: Cursor 报错无法连接

**检查**:
1. 查看 `mcp.json` 配置是否正确
2. 测试 SSE 端点是否返回 `event:endpoint`
3. 检查服务器日志是否有错误

### 问题 3: 文件下载失败

**症状**: 转换成功但下载 URL 失效

**检查**:
1. `MD2DOC_DOWNLOAD_BASE_URL` 是否设置正确
2. `/api/markdown/` 路径是否正确转发
3. 临时文件是否存在

### 问题 4: 协议版本警告

**症状**: 日志显示协议版本不匹配

**说明**: 这是兼容性警告，不影响功能

**解决**: 可以忽略，或等待 Spring AI 更新支持新版协议

---

## 日志监控

### 关键日志

```bash
# 实时查看日志
tail -f /path/to/md2doc-service.log

# 过滤 SSE 相关日志
tail -f /path/to/md2doc-service.log | grep -i sse

# 过滤 MCP 相关日志
tail -f /path/to/md2doc-service.log | grep -i mcp
```

### 正常日志示例

```
[INFO] McpAsyncServer - SSE connection established
[INFO] McpAsyncServer - Sending tools list
[INFO] McpAsyncServer - Heartbeat sent
```

### 异常日志示例

```
[WARN] PageNotFound - No mapping for POST /dataReport/md2doc
[ERROR] McpAsyncServer - Connection lost
```

---

## 性能优化建议

### 1. 负载均衡（如有多台服务器）

```nginx
upstream md2doc_backend {
    server 192.9.253.106:8080 max_fails=3 fail_timeout=30s;
    # 如有多台，可添加更多
    # server 192.9.253.107:8080;
}

location /dataReport/ {
    proxy_pass http://md2doc_backend/dataReport/;
    # ... 其他配置
}
```

### 2. 限流保护

```nginx
# 在 location /dataReport/ 中添加
limit_req zone=mcp_limit burst=10 nodelay;
```

并在 http 块中定义：

```nginx
limit_req_zone $binary_remote_addr zone=mcp_limit:10m rate=10r/m;
```

### 3. 监控指标

- SSE 连接数
- 平均连接时长
- 转换请求数
- 错误率

---

## 总结

### 核心配置要点

1. **反向代理**: 必须设置 `proxy_set_header Connection ""`
2. **超时设置**: 所有 SSE 相关超时设置为 24 小时
3. **缓冲禁用**: SSE 必须禁用缓冲和缓存
4. **应用配置**: Tomcat 连接超时设置为 -1

### 验证检查清单

- [ ] Nginx 配置已更新
- [ ] `nginx -t` 测试通过
- [ ] 配置已重载
- [ ] md2doc 服务已重启
- [ ] SSE 端点返回 `Connection: keep-alive`
- [ ] SSE 连接可保持 30 秒以上
- [ ] REST API 正常工作
- [ ] CORS 配置正确
- [ ] 环境变量 `MD2DOC_DOWNLOAD_BASE_URL` 已设置

完成以上检查后，MCP 服务应该可以通过公网正常访问。
