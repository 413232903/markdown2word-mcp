# md2doc MCP 服务公网部署指南

## 修复内容总结

本次修复解决了 MCP 服务公网映射失败的问题，主要包括以下改动：

### 1. CORS 配置扩展

**文件**: `src/main/java/cn/daydayup/dev/md2doc/service/config/WebConfig.java`

**问题**: 原 CORS 配置只覆盖 `/api/**` 路径，MCP 的 SSE 端点 `/dataReport/**` 不在允许范围内。

**修复**: 添加了 `/dataReport/**` 路径的 CORS 支持，确保公网访问时不会被浏览器跨域策略阻止。

```java
// MCP SSE 端点路径 - 必须支持 CORS 以便公网访问
registry.addMapping("/dataReport/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false);
```

### 2. REST API URL 生成优化

**文件**: `src/main/java/cn/daydayup/dev/md2doc/service/controller/MarkdownController.java`

**问题**: REST API 返回相对路径，客户端无法直接访问公网地址。

**修复**:
- 添加 `downloadBaseUrl` 配置项
- 新增 `buildDownloadUrl()` 方法，根据配置返回完整 URL 或相对路径
- MCP 工具类已有相同逻辑，保持一致

### 3. 环境变量支持

**文件**: `src/main/resources/application.yml`

服务通过环境变量 `MD2DOC_DOWNLOAD_BASE_URL` 接收公网地址配置。

## 部署步骤

### 方式 1: 使用环境变量（推荐）

在部署服务器上设置环境变量：

```bash
# Linux/Mac
export MD2DOC_DOWNLOAD_BASE_URL=https://mcpapi2.qjyy.com

# Windows (PowerShell)
$env:MD2DOC_DOWNLOAD_BASE_URL="https://mcpapi2.qjyy.com"

# Windows (CMD)
set MD2DOC_DOWNLOAD_BASE_URL=https://mcpapi2.qjyy.com
```

然后启动服务：

```bash
java -jar md2doc-service-1.0.jar
```

### 方式 2: 启动参数

```bash
java -jar md2doc-service-1.0.jar --md2doc.download-base-url=https://mcpapi2.qjyy.com
```

### 方式 3: 修改 application.yml

在 `application.yml` 中直接修改：

```yaml
md2doc:
  download-base-url: https://mcpapi2.qjyy.com
```

## MCP 客户端配置

### Cursor MCP 配置

**配置文件位置**: `~/.cursor/mcp.json` (或 `c:\Users\Administrator\.cursor\mcp.json`)

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

**说明**:
- `url`: 公网 MCP SSE 端点
- `args`: `--insecure` 用于跳过 SSL 证书验证（自签名证书需要）
- 如果使用有效 SSL 证书，可以移除 `--insecure`

## 端点说明

| 端点 | 类型 | 用途 | CORS |
|------|------|------|------|
| `/dataReport/md2doc` | SSE | MCP 服务主端点 | ✅ 已支持 |
| `/dataReport/mcp/message` | SSE | MCP 消息端点 | ✅ 已支持 |
| `/api/markdown/convert/file` | POST | 文件转换 | ✅ 已支持 |
| `/api/markdown/convert/text` | POST | 文本转换 | ✅ 已支持 |
| `/api/markdown/files/{fileName}` | GET | 文件下载 | ✅ 已支持 |

## 返回 URL 格式

### 配置了公网 URL 时
```
https://mcpapi2.qjyy.com/api/markdown/files/{uuid}.docx
```

### 未配置时（内网/本地）
```
/api/markdown/files/{uuid}.docx
```

## 反向代理配置建议

如果使用 Nginx 作为反向代理，建议配置如下：

```nginx
# MCP SSE 端点
location /dataReport/ {
    proxy_pass http://localhost:8080/dataReport/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# REST API
location /api/markdown/ {
    proxy_pass http://localhost:8080/api/markdown/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 故障排查

### 1. SSE 连接失败

**症状**: 客户端无法连接到 MCP 端点

**检查**:
- CORS 配置是否生效
- 防火墙是否开放 8080 端口
- 反向代理是否正确配置 `Upgrade` 头

### 2. 文件下载失败

**症状**: 转换成功但下载失败

**检查**:
- `MD2DOC_DOWNLOAD_BASE_URL` 是否正确设置
- 返回的 URL 是否可访问
- 临时文件是否已生成

### 3. 跨域错误

**症状**: 浏览器控制台显示 CORS 错误

**检查**:
- 确认已修复 WebConfig.java 中的 CORS 配置
- 重启服务使配置生效

## 安全建议

1. **生产环境**: 建议使用 HTTPS，移除 `--insecure` 参数
2. **访问控制**: 将 `allowedOrigins("*")` 改为具体的域名
3. **认证**: 如果需要，添加认证机制保护端点
4. **速率限制**: 对转换接口添加速率限制，防止滥用

## 验证修复

测试 MCP 连接：

```bash
# 测试 SSE 端点
curl -v https://mcpapi2.qjyy.com/dataReport/md2doc

# 测试转换接口
curl -X POST https://mcpapi2.qjyy.com/api/markdown/convert/text \
  -H "Content-Type: application/json" \
  -d '{"content": "# 测试文档\n\n这是一个测试。"}'
```

## 总结

通过本次修复，MCP 服务现在支持：

1. ✅ 完整的 CORS 支持（包括 MCP SSE 端点）
2. ✅ 可配置的公网 URL 生成
3. ✅ 环境变量部署支持
4. ✅ 统一的 URL 构建逻辑

确保在部署时正确设置 `MD2DOC_DOWNLOAD_BASE_URL` 环境变量即可支持公网访问。
