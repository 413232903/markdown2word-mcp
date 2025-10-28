# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

md2doc-service 是一个基于 Spring Boot 3.2.3 的服务，同时提供：
1. **RESTful API** - 传统的 HTTP API 接口
2. **MCP Server** - Model Context Protocol 服务器，支持 AI 模型直接调用

该服务是 md2doc-plus 多模块项目的一部分，依赖于 md2doc-core 核心转换模块。

## 技术栈

- Java 17
- Spring Boot 3.2.3
- Spring AI MCP Server (1.0.0-M5)
- Maven 多模块项目
- md2doc-core (核心转换逻辑，基于 Apache POI 5.2.2)

## 常用命令

### 启动服务
```bash
mvn spring-boot:run
```
服务默认运行在 `http://localhost:8080`

### 构建项目
```bash
mvn clean package
```

### 运行测试
```bash
mvn test
```

### 编译项目
```bash
mvn compile
```

## 项目架构

### 模块结构

这是一个 Maven 多模块项目：
- `md2doc-core`: 核心转换逻辑模块（位于 `../md2doc-core`）
  - 包含 Markdown 解析、Word 文档生成、模板引擎等核心功能
  - 主要类：`MarkdownToWordConverter`（位于 `cn.daydayup.dev.md2doc.core` 包）
- `md2doc-service`: 当前服务模块
  - 提供 RESTful API 接口封装

### 服务层架构

采用标准的 Spring Boot 三层架构：

1. **Controller 层** (`MarkdownController`)
   - 位置: `src/main/java/cn/daydayup/dev/md2doc/service/controller/`
   - 提供两个转换端点：
     - `POST /api/markdown/convert/file`: 上传 Markdown 文件转换
     - `POST /api/markdown/convert/text`: 提交 Markdown 文本转换
   - 提供文件下载端点：`GET /api/markdown/files/{fileName}`
   - 启用 CORS，允许跨域访问

2. **Service 层** (`MarkdownConversionService`)
   - 位置: `src/main/java/cn/daydayup/dev/md2doc/service/service/`
   - 封装对 md2doc-core 模块的调用
   - 提供两个转换方法：
     - `convertMarkdownFileToWord()`: 文件路径转换
     - `convertMarkdownToWord()`: 文本内容转换

3. **Configuration** (`WebConfig`)
   - 位置: `src/main/java/cn/daydayup/dev/md2doc/service/config/`
   - Web 配置相关设置

### 临时文件处理

- 临时文件目录: `System.getProperty("java.io.tmpdir") + "/md2doc/"`
- 使用 UUID 生成唯一文件名
- 转换后自动清理临时 Markdown 文件
- 生成的 Word 文档保留在临时目录供下载

### 核心转换流程（来自 md2doc-core）

1. Markdown 解析 (`MarkdownTableParser`)
2. 动态模板生成 (`DynamicWordDocumentCreator`)
3. 图片下载和处理 (`ImageDownloader`)
4. 内容填充 (`PoiWordGenerator`)
5. 图表转换 (`EChartsToWordConverter`)

### 图片处理机制

1. **图片识别**: 使用正则表达式匹配 `![alt](url)` 语法
2. **图片下载**:
   - HTTP/HTTPS URL: 通过 HttpURLConnection 下载（10秒超时）
   - 本地路径: 直接读取文件（支持绝对和相对路径）
3. **尺寸自适应**:
   - 最大宽度限制为 600px（约15cm，适合A4页面）
   - 保持原始宽高比进行缩放
4. **错误处理**:
   - 下载失败时在 Word 中插入占位符文本
   - 记录详细的错误日志

## API 使用示例

### 上传文件转换
```bash
curl -X POST "http://localhost:8080/api/markdown/convert/file" \
     -F "file=@/path/to/markdown.md" \
     -o output.docx
```

### 文本内容转换
```bash
curl -X POST "http://localhost:8080/api/markdown/convert/text" \
     -H "Content-Type: application/json" \
     -d '{"content": "# 标题\n\n段落内容"}' \
     -o output.docx
```

## 支持的 Markdown 特性

- 六级标题 (H1-H6) 及自动编号
- 段落文本
- Markdown 表格
- ECharts 图表 (使用 ```echarts 代码块)
- 图片 (支持 HTTP/HTTPS URL 和本地文件路径)
  - 自动下载网络图片
  - 支持 JPG、PNG、GIF、BMP、WEBP 格式
  - 自适应页面宽度（最大 600px）
  - 下载失败时显示占位符
- 标题格式化和目录生成支持

## 开发注意事项

### 包扫描配置
主应用类使用 `@ComponentScan(basePackages = "cn.daydayup.dev.md2doc")` 扫描所有相关包，包括核心模块的组件。

### 依赖关系
服务模块依赖于 md2doc-core 模块，所有核心转换逻辑在 core 模块中实现。修改转换逻辑时需要到 `../md2doc-core` 目录。

### 多模块构建
由于这是 Maven 多模块项目，在根目录（`../`）执行 `mvn clean install` 会构建所有模块。

## MCP (Model Context Protocol) 支持

### MCP 架构

服务同时支持传统 REST API 和 MCP 协议，互不干扰：

1. **MCP Tools** (`Md2docMcpTools`)
   - 位置: `src/main/java/cn/daydayup/dev/md2doc/service/mcp/`
   - 提供两个可执行工具：
     - `convertMarkdownText`: 转换 Markdown 文本内容
     - `convertMarkdownFile`: 转换 Markdown 文件
   - 返回 Base64 编码的 Word 文档内容

2. **MCP Resources** (`Md2docMcpResources`)
   - 位置: `src/main/java/cn/daydayup/dev/md2doc/service/mcp/`
   - 提供三个数据资源：
     - `md2doc://supported-features`: 支持的 Markdown 特性列表
     - `md2doc://conversion-guide`: 转换指南和最佳实践
     - `md2doc://examples`: 使用示例和代码样例

3. **MCP Prompts** (`Md2docMcpPrompts`)
   - 位置: `src/main/java/cn/daydayup/dev/md2doc/service/mcp/`
   - 提供四个提示模板：
     - `quick-start`: 快速开始指南
     - `chart-conversion`: 图表转换指南
     - `table-formatting`: 表格格式化指南
     - `troubleshooting`: 故障排除指南

### MCP 配置

MCP 服务器配置位于 `application.yml`:
- **传输方式**: HTTP SSE (Server-Sent Events)
- **端点路径**: `/mcp/sse`
- **超时设置**: 30分钟
- **心跳间隔**: 30秒

### 使用 MCP 服务

#### 1. 启动 MCP 服务器
```bash
mvn spring-boot:run
```
MCP 端点: `http://localhost:8080/mcp/sse`

#### 2. 配置 MCP 客户端

在 Claude Desktop 的配置文件中添加：
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

#### 3. 测试 MCP 连接

使用任何 MCP 客户端连接到 `http://localhost:8080/mcp/sse`。

服务器信息：
- Name: `md2doc-mcp-server`
- Version: `1.0`
- Description: `Markdown to Word Document Conversion MCP Server`

#### 4. 调用 MCP Tools

AI 模型可以直接调用工具：

**convertMarkdownText 示例:**
```json
{
  "markdownContent": "# 标题\n\n这是段落。\n\n## 子标题\n\n- 列表项1\n- 列表项2"
}
```

**convertMarkdownFile 示例:**
```json
{
  "markdownFilePath": "/path/to/document.md"
}
```

**返回结果:**
```json
{
  "success": true,
  "message": "转换成功",
  "fileName": "uuid.docx",
  "base64Content": "UEsDBBQABgAIAAAA...",
  "fileSize": 12345
}
```

#### 5. 访问 MCP Resources

AI 模型可以读取资源获取信息：
- 查询支持的特性: 读取 `md2doc://supported-features`
- 查看最佳实践: 读取 `md2doc://conversion-guide`
- 查看使用示例: 读取 `md2doc://examples`

#### 6. 使用 MCP Prompts

AI 模型可以使用预定义提示：
- `quick-start`: 获取快速开始指导
- `chart-conversion`: 学习如何使用图表
- `table-formatting`: 学习表格格式化
- `troubleshooting`: 解决常见问题

### MCP vs REST API

两种接口方式的区别：

| 特性 | REST API | MCP |
|------|----------|-----|
| 调用方式 | HTTP POST | AI 工具调用 |
| 返回格式 | 二进制文件流 | Base64 编码 |
| 适用场景 | Web 应用、脚本 | AI Agent、智能助手 |
| 文档支持 | OpenAPI/Swagger | MCP Resources/Prompts |
| 学习成本 | 需要查阅 API 文档 | AI 自动理解和使用 |

### MCP 调试

启用 MCP 调试日志：
```yaml
logging:
  level:
    org.springframework.ai.mcp: DEBUG
```

查看 MCP 请求和响应详情，帮助排查问题。

