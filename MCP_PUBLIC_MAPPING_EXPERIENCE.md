# MCP 服务公网映射开发经验总结

> 项目：md2doc-mcp (Markdown 转 Word 的 MCP 服务)
> 公网地址：https://mcpapi2.qjyy.com/dataReport/md2doc
> 反向代理：Nginx/Tengine（多 MCP 服务共用）

---

## 📋 问题描述

### 初始问题

1. **MCP 工具在公网无法使用**
   - Cursor MCP 客户端连接失败
   - 错误信息：`Error POSTing to endpoint (HTTP 404): Not Found`

2. **内网地址正常工作**
   - `http://192.9.253.106:8080/dataReport/md2doc` 可以正常连接

---

## 🔍 问题排查过程

### 第一阶段：基础连接测试

| 测试项 | 结果 | 结论 |
|--------|------|------|
| 内网 SSE 端点 | ✅ 200 | 端点正常 |
| 公网 SSE 端点 | ✅ 200 | 端点可访问 |
| 公网返回 sessionId | ✅ 成功 | 返回动态 sessionId |

### 第二阶段：发现路径缺失

#### 问题 1：REST API 路径无法访问

```bash
# 测试发现
curl https://mcpapi2.qjyy.com/api/markdown/convert/text
# 结果：404 Not Found
```

**根本原因**：
- 反向代理只配置了 `/dataReport/` 路径的转发
- REST API 端点使用 `/api/markdown/` 路径
- 这些路径没有对应的反向代理配置，请求被默认规则处理

**运维反馈**：
> "你发送了两个动作，第二个里面没有带 dataReport/md2doc 路径，他传不过去的，发默认规则里面去了，你除了第一个对接之外，里面的包里面还发送了其他请求信息"

#### 问题 2：`Connection: close` 影响 SSE 长连接

```bash
curl -v https://mcpapi2.qjyy.com/dataReport/md2doc
# 响应头：
< HTTP/1.1 200
< Connection: close
```

**影响**：
- SSE 连接无法长期保持
- MCP 客户端需要频繁重新连接
- 可能导致消息丢失

#### 问题 3：MCP message 端点配置错误

```bash
# SSE 返回的 message 端点
data:/dataReport/mcp/message?sessionId=xxx

# 尝试 POST 请求
curl -X POST https://mcpapi2.qjyy.com/dataReport/mcp/message?sessionId=xxx
# 结果：404 Not Found
```

**原因**：
- Spring AI MCP 配置的 `sse-message-endpoint: /dataReport/mcp/message`
- 这个路径不在应用中实际存在

---

## 🎯 解决方案

### 方案选择分析

#### 方案 A：修改反向代理（被否决）

**内容**：为 `/api/markdown/` 添加反向代理规则

**优点**：
- 应用代码无需修改
- 路径保持简洁

**缺点**：
- ❌ 反向代理是多个 MCP 服务共用，不方便修改
- ❌ 可能影响其他服务

**决策**：被运维否决

---

#### 方案 B：统一路径到 `/dataReport/md2doc`（采用）✅

**内容**：将所有 REST API 端点迁移到 `/dataReport/md2doc/api/markdown/`

**优点**：
- ✅ 不需要修改反向代理
- ✅ 所有请求都在 `/dataReport/` 下
- ✅ 统一路径管理
- ✅ 多服务兼容，不影响其他 MCP 服务

**缺点**：
- ⚠️ 路径变长
- ⚠️ 内网环境也需要使用新路径
- ⚠️ 向后不兼容

**决策**：✅ 采用此方案

---

## 📝 实施细节

### 代码修改清单

| 文件 | 修改内容 | 关键点 |
|------|---------|---------|
| `MarkdownController.java` | `@RequestMapping("/dataReport/md2doc/api/markdown")` | 统一 API 路径 |
| `MarkdownController.java` | `buildDownloadUrl()` 方法 | 返回新路径的下载 URL |
| `Md2docMcpTools.java` | `buildDownloadUrl()` 方法 | MCP 工具返回新下载 URL |
| `WebConfig.java` | 移除 `/api/**` CORS 规则 | 简化 CORS 配置 |
| `application.yml` | `sse-message-endpoint: /dataReport/md2doc/mcp/message` | 修正 message 端点路径 |

### 新路径结构

```
/dataReport/md2doc                          ← SSE 主端点
/dataReport/md2doc/mcp/message               ← MCP message 端点
/dataReport/md2doc/api/markdown/convert/text  ← 文本转换
/dataReport/md2doc/api/markdown/convert/file  ← 文件转换
/dataReport/md2doc/api/markdown/files/{file}    ← 文件下载
```

---

## ⚠️ 遇到的坑和教训

### 坑 1：多个 `buildDownloadUrl` 方法

**问题**：
- `MarkdownController.java` 有自己的 `buildDownloadUrl` 方法
- `Md2docMcpTools.java` 也有 `buildDownloadUrl` 方法
- 两个方法都需要修改，容易遗漏

**教训**：
- 全局搜索所有相关的 URL 构建逻辑
- 使用 IDE 的查找引用功能，确保所有地方都修改

---

### 坑 2：路径配置不一致

**问题**：
- `sse-message-endpoint` 初始配置为 `/dataReport/mcp/message`
- 但实际需要的路径是 `/dataReport/md2doc/mcp/message`
- 导致 MCP 客户端请求错误的端点

**教训**：
- Spring AI MCP 的 `sse-message-endpoint` 应该在 `sse-endpoint` 路径下
- 需要仔细阅读 Spring AI MCP 的配置文档
- 如果不确定，可以尝试注释掉让框架自动管理

---

### 坑 3：反向代理 `Connection: close`

**问题**：
- 反向代理默认返回 `Connection: close`
- SSE 长连接需要 `Connection: keep-alive`

**教训**：
- SSE 连接必须特殊处理 Connection 头
- 反向代理配置中需要 `proxy_set_header Connection ""`
- 这是 Spring AI MCP SSE 传输的必需配置

---

### 坑 4：CORS 配置遗漏

**问题**：
- 初始 CORS 配置只有 `/api/**`
- MCP SSE 端点 `/dataReport/**` 没有覆盖

**教训**：
- 所有暴露给公网的端点都需要 CORS 配置
- 使用通配符 `/**` 确保覆盖所有子路径

---

### 坑 5：测试不够全面

**问题**：
- 只测试了 GET 请求，没有测试 POST 请求
- 只测试了主要端点，没有测试所有子路径

**教训**：
- 建立完整的测试用例列表
- 测试 GET、POST、OPTIONS、DELETE 等所有方法
- 测试所有路径变体（带/不带斜杠、带查询参数等）

---

## 🔧 反向代理配置（待运维添加）

### 必需配置

```nginx
location /dataReport/ {
    proxy_pass http://192.9.253.106:8080/dataReport/;

    # 🔑 最关键：清空 Connection 头
    proxy_set_header Connection "";
    proxy_http_version 1.1;

    # 基础头
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # SSE 必需配置
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 86400s;
    proxy_send_timeout 86400s;
    gzip off;
}
```

### 配置验证

```bash
# 测试 Connection 头
curl -v https://mcpapi2.qjyy.com/dataReport/md2doc 2>&1 | grep -i "connection"

# 期望结果：
< Connection: keep-alive
# 或者没有 Connection 头（表示使用后端默认）
```

---

## ✅ 最终验证清单

### 应用服务端

| 项目 | 检查方法 | 结果 |
|------|---------|------|
| SSE 端点连接 | `curl https://mcpapi2.qjyy.com/dataReport/md2doc` | ✅ 200 |
| 文本转换 API | `curl -X POST /dataReport/md2doc/api/markdown/convert/text` | ✅ 200 |
| 返回文件 URL | 检查响应 JSON 中的 `fileUrl` | ✅ 新路径 |
| CORS 头 | `curl -H "Origin: *" -X OPTIONS` | ✅ 正确 |
| 文件下载端点 | `curl /dataReport/md2doc/api/markdown/files/test.docx` | ✅ 404（文件不存在但路径转发正确）|

### MCP 客户端

| 项目 | 验证方法 | 状态 |
|------|---------|------|
| SSE 连接 | Cursor MCP 配置文件中的 URL | ✅ 等部署 |
| 工具列表 | 调用 `tools/list` | ⚠️ 等部署验证 |
| 文本转换 | 调用 `convertMarkdownText` | ⚠️ 等部署验证 |
| 文件下载 | 访问返回的下载 URL | ⚠️ 等部署验证 |

---

## 📚 参考资料

### Spring AI MCP 相关

- **SSE 传输协议**：Server-Sent Events，用于服务器向客户端推送消息
- **message 端点**：客户端向服务器发送请求的端点
- **端点关系**：`sse-endpoint` 返回的 `data` 字段包含 `sse-message-endpoint` 的完整路径

### Nginx/Tengine SSE 配置

```nginx
# SSE 必需的配置项
proxy_http_version 1.1;          # HTTP/1.1
proxy_set_header Connection "";     # 清空 Connection 头
proxy_buffering off;               # 禁用缓冲
proxy_cache off;                  # 禁用缓存
proxy_read_timeout 86400s;         # 长读取超时
gzip off;                        # SSE 不支持压缩
```

---

## 🎓 总结与最佳实践

### 最佳实践

#### 1. 路径规划

- ✅ **统一前缀**：所有相关端点使用统一前缀（如 `/dataReport/md2doc/`）
- ✅ **避免跨层级**：不同服务使用不同前缀，避免冲突
- ✅ **RESTful 设计**：API 端点遵循 RESTful 规范

#### 2. 反向代理配置

- ✅ **明确路径**：每个服务使用独立的 `location` 块
- ✅ **SSE 特殊处理**：SSE 端点需要特殊的连接头和超时设置
- ✅ **日志监控**：配置访问日志，便于问题排查

#### 3. 测试策略

- ✅ **内网优先**：先在内网验证功能，再部署公网
- ✅ **逐层测试**：从最底层开始，逐层向上验证
- ✅ **全路径覆盖**：测试所有可能的路径和请求方法

#### 4. 问题排查

- ✅ **查看完整请求**：包括请求方法、路径、头部、Body
- ✅ **检查响应头**：关注 `Connection`、`Content-Type`、`CORS` 等关键头
- ✅ **对比内网/公网**：相同请求在内网和公网的表现差异

#### 5. 配置管理

- ✅ **环境变量化**：公网 URL、超时等配置通过环境变量传递
- ✅ **配置文档化**：记录所有配置项和默认值
- ✅ **版本控制**：配置文件纳入版本管理

---

## 🚨 常见错误和解决

| 错误症状 | 可能原因 | 解决方法 |
|----------|----------|---------|
| 404 Not Found | 路径未配置、反向代理规则缺失 | 检查反向代理配置、添加对应 `location` |
| 405 Method Not Allowed | HTTP 方法不支持 | 检查 Controller 的 `@GetMapping`/`@PostMapping` |
| 403 Forbidden | CORS 配置错误 | 检查 `WebConfig` 的 CORS 规则 |
| Connection: close | 反向代理 SSE 配置不正确 | 添加 `proxy_set_header Connection ""` |
| 400 Bad Request | 请求体格式错误 | 检查 `@RequestBody` 的字段名和类型 |
| Session not found | sessionId 过期或无效 | 重新建立 SSE 连接获取新 sessionId |

---

## 📌 后续优化建议

### 短期优化

1. **修复 SSE 连接保持**
   - 添加反向代理 `proxy_set_header Connection ""` 配置
   - 验证 `Connection: keep-alive` 返回

2. **完善监控**
   - 添加请求/响应日志
   - 监控 SSE 连接数和持续时间
   - 设置错误告警

### 长期优化

1. **性能优化**
   - 考虑使用 WebSocket 替代 SSE（如需要双向通信）
   - 优化文件下载性能（使用 CDN 或对象存储）

2. **安全性增强**
   - 限制 `allowedOrigins` 为具体域名
   - 添加认证机制
   - 实施速率限制

3. **高可用性**
   - 配置多个后端实例的负载均衡
   - 实现健康检查和自动故障转移

---

## 📊 成本分析

| 项目 | 耗时 | 说明 |
|------|------|------|
| 问题排查 | ~2 小时 | 测试各种端点和配置 |
| 方案设计讨论 | ~30 分钟 | 与团队讨论方案可行性 |
| 代码修改 | ~30 分钟 | 修改 4 个文件 |
| 测试验证 | ~1 小时 | 多轮测试和验证 |
| 文档编写 | ~20 分钟 | 编写经验文档 |
| **总计** | ~4 小时 | 完整的问题解决和部署 |

---

## 🎯 关键要点回顾

1. **路径统一是关键**：通过将所有端点统一到 `/dataReport/md2doc` 下，避免了修改反向代理的复杂性
2. **SSE 需要特殊处理**：反向代理必须正确配置 `Connection` 头和超时设置
3. **全面测试必不可少**：覆盖所有端点、方法和路径变体
4. **文档和沟通很重要**：清晰的问题描述和解决方案文档有助于团队协作
5. **配置管理优先**：使用环境变量和版本控制，便于部署和维护

---

**总结**：这次公网映射问题的解决过程展示了系统化排查、多方案对比、 careful 实施、全面验证的重要性。通过积累这些经验，可以更高效地解决类似问题。
