# md2doc MCP 服务使用指南

## ✅ 服务状态

MCP 服务已成功启动并运行!

### 服务信息
- **进程 ID**: 77684
- **端口**: 8080
- **Java 版本**: 18
- **MCP 功能**: 已启用 ✓

## 🌐 访问地址

### 局域网访问 (从其他电脑)
```
http://192.9.243.78:8080
```

### MCP SSE 端点
```
http://192.9.243.78:8080/dataReport/md2doc
```

### REST API 端点
```
POST http://192.9.243.78:8080/api/markdown/convert/text
POST http://192.9.243.78:8080/api/markdown/convert/file
```

## 🔧 可用的 MCP 工具

服务提供以下 3 个 MCP 工具:

1. **convertMarkdownText** - 将 Markdown 文本转换为 Word 文档
2. **convertMarkdownFile** - 将 Markdown 文件转换为 Word 文档
3. **getSupportedFeatures** - 获取支持的 Markdown 特性列表

## 📱 配置 MCP 客户端

### Claude Desktop / Cursor

编辑配置文件 `~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "md2doc": {
      "command": "curl",
      "args": [
        "-N",
        "-H",
        "Accept: text/event-stream",
        "http://192.9.243.78:8080/dataReport/md2doc"
      ]
    }
  }
}
```

### 注意事项

1. **端点路径**: MCP SSE 端点是 `/dataReport/md2doc`
2. **握手流程**:
   - 客户端首先连接 `/dataReport/md2doc` 端点
   - 服务器返回 sessionId
   - 后续消息使用 `/dataReport/mcp/message?sessionId=xxx` 端点
3. **网络**: 确保客户端和服务器在同一局域网,或使用公网 IP

## 🧪 测试方法

### 1. 测试 SSE 端点连接
```bash
curl -N -H "Accept: text/event-stream" http://192.9.243.78:8080/dataReport/md2doc
```

应该返回类似:
```
id:xxxx-xxxx-xxxx
event:endpoint
data:/dataReport/mcp/message?sessionId=xxxx-xxxx-xxxx
```

### 2. 测试 REST API
```bash
curl -X POST http://192.9.243.78:8080/api/markdown/convert/text \
  -H "Content-Type: application/json" \
  -d '{"content":"# 测试标题\n\n这是测试内容"}' \
  -o test.docx
```

### 3. 浏览器测试
在其他电脑的浏览器中访问:
```
http://192.9.243.78:8080/api/markdown/convert/text
```

## 🔄 重启服务

如果需要重启服务:

```bash
# 1. 查找进程 ID
ps aux | grep "md2doc-service" | grep -v grep

# 2. 停止服务
kill <PID>

# 3. 启动服务
export JAVA_HOME=$(/usr/libexec/java_home -v 18)
java -jar md2doc-service/target/md2doc-service-1.0.jar
```

## 📝 使用示例

### 在 AI 对话中使用

配置完成后,在 Claude Desktop 或 Cursor 中,你可以直接让 AI 调用工具:

```
请帮我将以下 Markdown 转换为 Word:

# 项目报告
## 1. 概述
这是一个测试报告。

## 2. 数据表格
| 指标 | 数值 |
|------|------|
| 用户数 | 1000 |
| 收入 | 50000 |
```

AI 会自动调用 `convertMarkdownText` 工具,并返回 Base64 编码的 Word 文档。

## 🎯 MCP 协议流程

```
客户端                    MCP 服务器
   |                          |
   |--- GET /dataReport/md2doc --→ |
   |                          | (建立 SSE 连接)
   |←-- sessionId -----------|
   |                          |
   |--- POST /dataReport/mcp/message --→ |
   |    (带 sessionId)        |
   |                          | (处理请求)
   |←-- 响应 ----------------|
```

## ✅ 验证 MCP 工作状态

服务启动日志中应包含:
```
Enable tools capabilities, notification: true
Enable resources capabilities, notification: true
Enable prompts capabilities, notification: true
Enable completions capabilities
```

如果看到这些日志,说明 MCP 功能已正确启用!

## 🔧 故障排除

### 问题 1: 连接被拒绝
- 检查防火墙设置
- 确认服务正在运行: `ps aux | grep md2doc-service`
- 确认端口监听: `lsof -i :8080`

### 问题 2: MCP 工具不可见
- 确认配置文件路径正确
- 重启 Claude Desktop / Cursor
- 检查端点地址是否正确 (必须是 `/dataReport/md2doc`)

### 问题 3: 404 错误
- 确认使用 `/dataReport/md2doc` 端点而不是其他路径
- 检查服务日志是否有错误

## 📚 更多信息

- Spring AI MCP 文档: https://docs.spring.io/spring-ai/reference/api/mcp/
- Model Context Protocol 规范: https://spec.modelcontextprotocol.io/
- 项目 GitHub: (你的仓库地址)

---

**服务已就绪,可以开始使用! 🚀**
