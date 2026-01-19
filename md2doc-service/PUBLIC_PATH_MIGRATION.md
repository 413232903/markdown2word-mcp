# md2doc MCP 服务公网路径迁移说明

## 方案概述

由于公网反向代理配置为多个 MCP 服务共用，不便修改，因此将 REST API 端点统一迁移到 `/dataReport/md2doc/api/markdown/` 路径下，确保所有请求都能通过现有的 `/dataReport/` 反向代理规则。

## 路径变更对比

| 功能 | 原路径 | 新路径（公网） |
|------|---------|---------------|
| SSE 主端点 | `/dataReport/md2doc` | `/dataReport/md2doc` ✅ 无变化 |
| 文本转换 | `/api/markdown/convert/text` | `/dataReport/md2doc/api/markdown/convert/text` |
| 文件转换 | `/api/markdown/convert/file` | `/dataReport/md2doc/api/markdown/convert/file` |
| 文件下载 | `/api/markdown/files/{fileName}` | `/dataReport/md2doc/api/markdown/files/{fileName}` |

## 代码修改

### 1. MarkdownController.java

**文件**: `src/main/java/cn/daydayup/dev/md2doc/service/controller/MarkdownController.java`

**修改**: 第 22 行，将 `@RequestMapping` 从 `/api/markdown` 改为 `/dataReport/md2doc/api/markdown`

```java
// 修改前
@RequestMapping("/api/markdown")

// 修改后
@RequestMapping("/dataReport/md2doc/api/markdown")
```

### 2. Md2docMcpTools.java

**文件**: `src/main/java/cn/daydayup/dev/md2doc/service/mcp/Md2docMcpTools.java`

**修改**: 第 203 行，`buildDownloadUrl` 方法返回的路径

```java
// 修改前
return normalizedBaseUrl + "/api/markdown/files/" + fileName;

// 修改后
return normalizedBaseUrl + "/dataReport/md2doc/api/markdown/files/" + fileName;
```

### 3. WebConfig.java

**文件**: `src/main/java/cn/daydayup/dev/md2doc/service/config/WebConfig.java`

**修改**: 移除 `/api/**` 路径的 CORS 配置，只保留 `/dataReport/**`

```java
// 修改前
registry.addMapping("/api/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false);

registry.addMapping("/dataReport/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false);

// 修改后
registry.addMapping("/dataReport/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false);
```

## 部署步骤

### 1. 重新编译项目

```bash
cd md2doc-service
mvn clean package -DskipTests
```

### 2. 重启服务

```bash
# 停止旧服务
# (使用 kill 或 systemctl)

# 启动新服务
java -jar target/md2doc-service-1.0.jar \
  --md2doc.download-base-url=https://mcpapi2.qjyy.com
```

### 3. 验证路径

```bash
# 1. SSE 端点（应该不变）
curl -s -o /dev/null -w "%{http_code}" https://mcpapi2.qjyy.com/dataReport/md2doc --insecure
# 期望: 200

# 2. 文本转换端点（新路径）
curl -s -o /dev/null -w "%{http_code}" https://mcpapi2.qjyy.com/dataReport/md2doc/api/markdown/convert/text --insecure
# 期望: 404（需要 POST）

# 3. 文件下载端点（新路径）
curl -s -o /dev/null -w "%{http_code}" https://mcpapi2.qjyy.com/dataReport/md2doc/api/markdown/files/test.docx --insecure
# 期望: 404（文件不存在，但路径已正确转发）
```

## MCP 客户端配置

### Cursor MCP 配置

**配置文件**: `~/.cursor/mcp.json`

```json
{
  "mcpServers": {
    "md2doc_mcp_public": {
      "type": "sse",
      "url": "https://mcpapi2.qjyy.com/dataReport/md2doc",
      "args": ["--insecure"]
    }
  }
}
```

**无需修改** - SSE 端点路径不变，工具返回的下载 URL 会自动使用新路径。

## 方案优缺点

### ✅ 优点

| 优点 | 说明 |
|--------|------|
| 无需修改反向代理 | 所有请求在 `/dataReport/` 下，使用现有配置 |
| 统一路径管理 | 所有 MCP 相关功能在 `/dataReport/md2doc` 下 |
| 多服务兼容 | 不影响其他 MCP 服务 |
| 简化配置 | 不需要为 `/api/` 路径添加额外规则 |

### ⚠️ 缺点

| 缺点 | 影响 |
|--------|------|
| 路径变长 | URL 更长（但用户不直接使用） |
| 内网路径也改变 | 内网访问需要使用新路径 |
| 破坏性变更 | 如有其他客户端依赖旧路径，需要更新 |

## 注意事项

1. **向后兼容性**: 旧的 `/api/markdown/` 路径将不再可用
2. **内网访问**: 内网也需要使用新路径 `/dataReport/md2doc/api/markdown/...`
3. **临时文件**: 文件下载 URL 会自动使用新路径，无需手动修改

## 完整路径示例

### 内网访问

```
SSE:         http://192.9.253.106:8080/dataReport/md2doc
转换文本:    POST http://192.9.253.106:8080/dataReport/md2doc/api/markdown/convert/text
转换文件:    POST http://192.9.253.106:8080/dataReport/md2doc/api/markdown/convert/file
下载文件:    GET  http://192.9.253.106:8080/dataReport/md2doc/api/markdown/files/{uuid}.docx
```

### 公网访问

```
SSE:         https://mcpapi2.qjyy.com/dataReport/md2doc
转换文本:    POST https://mcpapi2.qjyy.com/dataReport/md2doc/api/markdown/convert/text
转换文件:    POST https://mcpapi2.qjyy.com/dataReport/md2doc/api/markdown/convert/file
下载文件:    GET  https://mcpapi2.qjyy.com/dataReport/md2doc/api/markdown/files/{uuid}.docx
```
