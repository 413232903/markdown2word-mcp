# md2doc-service

基于 Spring Boot 的 Markdown 转 Word 文档服务模块。

## 功能介绍

提供 RESTful API 接口，支持将 Markdown 文本或文件转换为 Word 文档(.docx)。

## API 接口说明

### 1. 上传 Markdown 文件并转换为 Word 文档

```
POST /api/markdown/convert/file
Content-Type: multipart/form-data

参数:
- file: Markdown 文件

响应:
- 成功: Word 文档文件下载
- 失败: HTTP 错误码
```

### 2. 提交 Markdown 文本内容并转换为 Word 文档

```
POST /api/markdown/convert/text
Content-Type: application/json

参数:
{
  "content": "Markdown 文本内容"
}

响应:
- 成功: Word 文档文件下载
- 失败: HTTP 错误码
```

## 使用示例

### 使用 curl 命令上传文件并转换

```bash
curl -X POST "http://localhost:8080/api/markdown/convert/file" \
     -F "file=@/path/to/your/markdown.md" \
     -o output.docx
```

### 使用 curl 命令提交文本内容并转换

```bash
curl -X POST "http://localhost:8080/api/markdown/convert/text" \
     -H "Content-Type: application/json" \
     -d '{"content": "# 标题\n\n这是段落内容。"}' \
     -o output.docx
```

## 启动服务

```bash
cd md2doc-service
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`。

## 支持的 Markdown 语法

- 标题 (H1-H6)
- 段落文本
- 表格
- ECharts 图表代码块 (使用 ```echarts 代码块)
- 图片 (支持网络 URL 和本地路径)