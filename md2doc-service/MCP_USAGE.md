# MCP 服务使用说明

## 什么是 MCP？

Model Context Protocol (MCP) 是一个开放协议，使 AI 模型能够与外部工具和数据源无缝集成。本服务实现了 MCP 服务器，让 AI 助手（如 Claude）可以直接调用 Markdown 转 Word 的功能。

## 快速开始

### 1. 启动服务

```bash
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动，MCP 端点为 `http://localhost:8080/mcp/sse`。

### 2. 配置 MCP 客户端

#### 使用 Claude Desktop

编辑 Claude Desktop 配置文件：

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

添加以下配置：

```json
{
  "mcpServers": {
    "md2doc": {
      "command": "curl",
      "args": [
        "-N",
        "-H", "Accept: text/event-stream",
        "http://localhost:8080/mcp/sse"
      ],
      "type": "sse"
    }
  }
}
```

重启 Claude Desktop，MCP 服务器将自动连接。

### 3. 使用 AI 工具

在 Claude Desktop 中，你可以直接要求 Claude 帮你转换 Markdown：

```
请帮我把这段 Markdown 转换为 Word 文档：

# 项目报告

## 概述
这是一个示例项目。

![项目架构图](https://example.com/architecture.png)

## 数据
| 项目 | 进度 |
|------|------|
| A    | 80%  |
| B    | 60%  |
```

Claude 将自动：
1. 调用 `convertMarkdownText` 工具
2. 下载网络图片并嵌入到 Word 文档
3. 获取 Base64 编码的 Word 文档
4. 为你保存文件

## MCP 功能说明

### 可用工具 (Tools)

1. **convertMarkdownText** - 转换 Markdown 文本
   - 参数: `markdownContent` (字符串)
   - 返回: Base64 编码的 Word 文档

2. **convertMarkdownFile** - 转换 Markdown 文件
   - 参数: `markdownFilePath` (文件路径)
   - 返回: Base64 编码的 Word 文档

### 数据资源 (Resources)

AI 可以读取以下资源获取帮助：

- `md2doc://supported-features` - 支持的 Markdown 特性列表
- `md2doc://conversion-guide` - 转换指南和最佳实践
- `md2doc://examples` - 使用示例

### 提示模板 (Prompts)

预定义的提示模板帮助 AI 更好地使用服务：

- `quick-start` - 快速开始指南
- `chart-conversion` - 图表转换指导
- `table-formatting` - 表格格式化技巧
- `troubleshooting` - 故障排除

## 示例对话

### 示例 1: 基本转换

**用户**: 帮我把这个 Markdown 转成 Word: `# Hello World`

**Claude**:
- 调用 `convertMarkdownText` 工具
- 获取并保存 Word 文档
- 提示用户文档已生成

### 示例 2: 带图表的文档

**用户**: 我想创建一个带柱状图的报告

**Claude**:
- 使用 `chart-conversion` 提示获取指导
- 生成包含 ECharts 图表的 Markdown
- 调用 `convertMarkdownText` 转换
- 返回 Word 文档

### 示例 3: 了解功能

**用户**: 这个转换工具支持什么功能？

**Claude**:
- 读取 `md2doc://supported-features` 资源
- 向用户展示支持的特性列表

## 配置选项

在 `application.yml` 中可以调整 MCP 配置：

```yaml
spring:
  ai:
    mcp:
      server:
        transport:
          sse:
            enabled: true              # 启用/禁用 SSE 传输
            path: /mcp/sse            # SSE 端点路径
            timeout: 1800000          # 连接超时 (毫秒)
            heartbeat-interval: 30000 # 心跳间隔 (毫秒)
        tools:
          enabled: true               # 启用/禁用工具
        resources:
          enabled: true               # 启用/禁用资源
        prompts:
          enabled: true               # 启用/禁用提示
```

## 调试

启用调试日志以查看 MCP 通信细节：

```yaml
logging:
  level:
    org.springframework.ai.mcp: DEBUG
```

## 与 REST API 的区别

| 特性 | REST API | MCP |
|------|----------|-----|
| 使用方式 | 手动编写 HTTP 请求 | AI 自动调用 |
| 返回格式 | 二进制文件流 | Base64 编码 |
| 文档获取 | 需要查阅 API 文档 | AI 自动读取资源 |
| 学习曲线 | 需要了解 API 规范 | 自然语言交互 |
| 适用场景 | Web 应用、脚本集成 | AI Agent、智能助手 |

## 故障排除

### MCP 服务器无法连接

1. 确认服务已启动: `curl http://localhost:8080/mcp/sse`
2. 检查防火墙设置
3. 查看服务日志

### 工具调用失败

1. 检查 Markdown 语法是否正确
2. 确认文件路径存在（使用 `convertMarkdownFile` 时）
3. 查看 MCP 调试日志

### Claude Desktop 未显示工具

1. 确认配置文件格式正确
2. 重启 Claude Desktop
3. 检查服务器连接状态

## 更多信息

详细的开发文档请参考 [CLAUDE.md](CLAUDE.md)。
