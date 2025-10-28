# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个基于 Python 的 Markdown 转 Word 文档的 MCP (Model Context Protocol) 服务器，完整复刻了 Java 版本的所有功能。项目将 Markdown 文档转换为格式化的 Word 文档，支持标题、表格和 ECharts 图表。

## 开发环境设置

### 安装依赖
```bash
# 安装项目（可编辑模式）
pip install -e .

# 安装开发依赖
pip install -e ".[dev]"
```

### 运行测试
```bash
# 运行所有测试
pytest tests/

# 运行特定测试文件
pytest tests/test_converter.py

# 运行特定测试方法
pytest tests/test_converter.py::TestMarkdownParser::test_extract_headers

# 运行测试并显示详细输出
pytest -v tests/
```

### 启动 MCP 服务器
```bash
# 直接启动
python -m md2doc_mcp.server

# 或使用 asyncio 运行
python src/md2doc_mcp/server.py
```

## 核心架构

### 分层架构设计

项目采用清晰的分层架构，各层职责单一：

1. **MCP 服务层** (`server.py`)
   - 实现 Anthropic MCP 协议
   - 提供三个工具：`convert_markdown_text`、`convert_markdown_file`、`get_supported_features`
   - 处理输入验证和错误处理

2. **核心转换层** (`core/`)
   - `converter.py`: 主转换器，协调整个转换流程
   - `template_creator.py`: 动态创建 Word 模板，处理标题自动编号
   - `word_generator.py`: Word 文档生成，替换占位符并插入内容
   - `echarts_converter.py`: 将 ECharts JSON 配置转换为图表数据

3. **解析器层** (`parser/`)
   - `markdown_parser.py`: 解析 Markdown，提取标题、ECharts 代码块、表格
   - `table_parser.py`: 解析 Markdown 表格为二维列表

4. **数据模型层** (`models/`)
   - `word_params.py`: 管理文本、表格、图表、图像参数
   - `chart_table.py`: 图表数据表模型
   - `chart_column.py`: 图表列数据模型

### 关键转换流程

完整的转换流程（参考 `converter.py`）：

1. 解析 Markdown 内容，识别标题、段落、表格、ECharts 代码块
2. 使用 `DynamicWordDocumentCreator` 根据 Markdown 结构创建 Word 模板
   - 标题自动编号（格式：1.1、1.2.3）
   - 自定义标题样式（Heading1-6）
   - 段落格式：小四宋体、1.5倍行距、首行缩进2字符
3. 处理 ECharts 图表：解析 JSON 配置，转换为 `ChartTable` 数据结构
4. 处理表格：解析 Markdown 表格为二维列表
5. 使用 `PoiWordGenerator` 填充内容到模板
   - 替换文本占位符
   - 插入表格（带表头背景色、居中对齐）
   - 插入图表（转换为数据表格）
6. 生成最终的 Word 文档

### Java 到 Python 的对照

| Java 组件 | Python 组件 | 说明 |
|----------|------------|------|
| Apache POI | python-docx | Word 文档操作 |
| Jackson | json (内置) | JSON 解析 |
| Lombok | dataclasses | 代码简化 |

## 支持的功能

### Markdown 语法
- **标题** (H1-H6): 自动编号，多级标题
- **段落文本**: 小四号宋体，1.5倍行距，首行缩进2字符
- **表格**: 自动解析，表头背景色，居中对齐

### ECharts 图表
使用 ```echarts 代码块，支持：
- 柱状图 (bar)
- 折线图 (line)
- 饼图 (pie)

ECharts 配置格式示例：
```markdown
```echarts
{
  title: { text: '销售数据' },
  xAxis: { data: ['1月', '2月', '3月'] },
  series: [{
    name: '销售额',
    type: 'bar',
    data: [120, 200, 150]
  }]
}
```
```

## 开发注意事项

### 占位符系统
- 文本占位符格式：`{{key}}`
- 表格占位符格式：`{{#table:key}}`
- 图表占位符格式：`{{#chart:key}}`
- 图像占位符格式：`{{#image:key}}`

### 标题自动编号规则
在 `template_creator.py` 中实现：
- H1: 1, 2, 3...
- H2: 1.1, 1.2, 2.1...
- H3: 1.1.1, 1.1.2, 1.2.1...
- 以此类推到 H6

### ECharts 转换逻辑
在 `echarts_converter.py` 中：
1. 预处理 JSON（处理单引号、尾部逗号）
2. 解析 JSON 配置
3. 提取标题、X轴、Y轴数据
4. 支持多个 series（多条数据线）
5. 转换为 `ChartTable` 数据结构

### 错误处理
- 所有 MCP 工具调用都应返回用户友好的错误信息
- 使用 try-except 捕获异常并返回 `TextContent`
- 验证输入：检查空值、文件存在性、文件类型

## 测试相关

### 测试文件位置
- `tests/test_converter.py`: 主要测试文件
- `example.md`: 示例 Markdown 文件用于测试

### 测试覆盖范围
- Markdown 解析（标题、表格、ECharts）
- Word 模板生成
- 占位符替换
- 完整转换流程

## Claude Desktop 集成

配置文件位置：
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

配置示例：
```json
{
  "mcpServers": {
    "md2doc": {
      "command": "python",
      "args": ["-m", "md2doc_mcp.server"]
    }
  }
}
```

## 常见问题排查

1. **MCP 服务器无法启动**
   - 检查 Python 版本 (>=3.8)
   - 确认依赖已安装：`pip list | grep -E 'mcp|python-docx|Pillow'`
   - 查看 Claude Desktop 日志

2. **转换失败**
   - 检查 Markdown 内容格式
   - 验证输出路径可写
   - 检查临时目录权限

3. **图表不显示**
   - 验证 ECharts JSON 格式
   - 确保 series 包含 data 字段
   - 检查数据类型（x轴为字符串，y轴为数字）
