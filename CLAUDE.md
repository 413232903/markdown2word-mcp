# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

md2doc-plus is a Markdown to Word document converter with dual implementations:
- **Java service** (md2doc-core + md2doc-service): Spring Boot REST API and MCP server
- **Python MCP server** (md2doc-service-python): Python rewrite with MCP protocol support

The project converts Markdown documents to formatted Word (.docx) files with support for:
- Multi-level headings (H1-H6) with automatic numbering
- Tables with formatting
- ECharts chart conversion (using ```echarts code blocks)
- Images (HTTP/HTTPS URLs and local paths)
- Lists and inline formatting

## Repository Structure

```
markdown2word-mcp/
├── md2doc-core/          # Java core conversion library (Apache POI)
├── md2doc-service/       # Spring Boot REST API + MCP server (Java)
├── md2doc-service-python/  # Python MCP server implementation
├── md2doc-ui/            # UI components (if present)
└── pom.xml              # Maven parent POM
```

## Java Development Commands

### Build the entire project
```bash
# From root directory
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

### Build individual modules
```bash
# Build core module only
cd md2doc-core
mvn clean install

# Build service module only
cd md2doc-service
mvn clean package
```

### Run the service
```bash
cd md2doc-service
mvn spring-boot:run

# Or run the JAR directly
java -jar target/md2doc-service-1.0.jar
```

### Run tests
```bash
# All tests
mvn test

# Specific module
cd md2doc-core
mvn test
```

## Python Development Commands

### Install dependencies
```bash
cd md2doc-service-python
pip install -e .

# With dev dependencies
pip install -e ".[dev]"
```

### Run tests
```bash
pytest tests/

# Specific test file
pytest tests/test_converter.py

# With verbose output
pytest -v tests/
```

### Start Python MCP server
```bash
python -m md2doc_mcp.server
```

## Java Architecture

### Module Dependencies
- **md2doc-core**: Standalone library with conversion logic
  - Depends on: Apache POI 5.2.2, Jackson, Lombok
  - Contains: `MarkdownToWordConverter` (main entry point)
- **md2doc-service**: Spring Boot application
  - Depends on: md2doc-core, Spring Boot 3.2.3, Spring AI MCP
  - Exposes: REST API + MCP SSE endpoint

### Core Conversion Flow (Java)

Located in `md2doc-core/src/main/java/cn/daydayup/dev/md2doc/core/`:

1. **MarkdownToWordConverter** - Main orchestrator
2. **MarkdownTableParser** - Parses Markdown tables
3. **DynamicWordDocumentCreator** - Creates Word template with:
   - Auto-numbering for headings (H1: 一、二、三, H2: 1、2、3, H3: 1）2）3）)
   - Custom heading styles (Heading1-6)
   - Paragraph formatting (宋体 小四, 1.5x line spacing, 2-char indent)
4. **EChartsToWordConverter** - Converts ECharts JSON to chart data tables
5. **PoiWordGenerator** - Fills template with content
6. **ImageDownloader** - Downloads/reads images and adapts to max 600px width

### Key Java Classes

| Package | Class | Purpose |
|---------|-------|---------|
| `core.model` | `WordParams`, `WordParam` | Parameter management |
| `core.model` | `ChartTable`, `ChartColumn` | Chart data structures |
| `core.parse` | `MarkdownTableParser` | Table parsing |
| `core.template` | `DynamicWordDocumentCreator` | Word template creation |
| `core.template` | `EChartsToWordConverter` | Chart conversion |
| `core.generate` | `PoiWordGenerator` | Document generation |
| `core.util` | `ImageDownloader` | Image handling |
| `service.controller` | `MarkdownController` | REST API endpoints |
| `service.mcp` | `Md2docMcpTools` | MCP tool definitions |

## Python Architecture

### Layered Design
Located in `md2doc-service-python/src/md2doc_mcp/`:

1. **server.py** - MCP protocol implementation
2. **core/converter.py** - Main conversion orchestrator
3. **core/template_creator.py** - Dynamic Word template with heading numbering
4. **core/word_generator.py** - Content filling and document generation
5. **core/echarts_converter.py** - ECharts JSON parsing
6. **parser/markdown_parser.py** - Markdown element extraction
7. **parser/table_parser.py** - Table parsing
8. **models/** - Data models (WordParams, ChartTable, ChartColumn)

### Technology Mapping (Java → Python)

| Java | Python |
|------|--------|
| Apache POI | python-docx |
| Jackson | json (built-in) |
| Lombok | dataclasses |
| Spring AI MCP | mcp SDK |

## Heading Auto-Numbering Logic

Critical logic maintained across both implementations:

- **H1**: 一、二、三、... (Chinese numerals, never resets)
- **H2**: 1、2、3、... (resets under each H1)
- **H3**: 1）、2）、3）、... (resets under each H2)
- **H4**: 1.1.1.1, 1.1.1.2 (resets under each H3)
- **H5/H6**: 1）、2）、... (resets under parent)

**Key Rule**: When moving from child to parent level (e.g., H3 → H2), if still under the same grandparent, the parent continues incrementing (does NOT reset).

Files:
- Java: `md2doc-core/.../DynamicWordDocumentCreator.java`
- Python: `md2doc-service-python/.../template_creator.py`

## MCP Configuration

### Java Service MCP Endpoint
- **SSE Endpoint**: `http://localhost:8080/dataReport/md2doc`
- **Config**: `md2doc-service/src/main/resources/application.yml`
- **Tools**: `convertMarkdownText`, `convertMarkdownFile`

### Python MCP Server
- **Module**: `md2doc_mcp.server`
- **Tools**: Same as Java implementation

### Claude Desktop Config
```json
{
  "mcpServers": {
    "md2doc": {
      "command": "java",
      "args": ["-jar", "/path/to/md2doc-service-1.0.jar"],
      "env": {"SPRING_PROFILES_ACTIVE": "mcp"}
    }
  }
}
```

## Placeholder System

Used in template creation and content filling:
- Text: `{{key}}`
- Tables: `{{#table:key}}`
- Charts: `{{#chart:key}}`
- Images: `{{#image:key}}`

## ECharts Integration

ECharts charts are embedded in Markdown using fenced code blocks:

```markdown
\`\`\`echarts
{
  "title": {"text": "Sales Data"},
  "xAxis": {"data": ["Jan", "Feb", "Mar"]},
  "series": [{
    "name": "Sales",
    "type": "bar",
    "data": [120, 200, 150]
  }]
}
\`\`\`
```

Supported chart types: bar, line, pie

## Image Handling

1. **Network images**: Downloaded via HTTP (10s timeout)
2. **Local images**: Read from filesystem (absolute/relative paths)
3. **Size adaptation**: Max 600px width, maintains aspect ratio
4. **Format support**: JPG, PNG, GIF, BMP, WEBP
5. **Error handling**: Placeholder text inserted on failure

## Recent Bug Fixes

### Heading Numbering Issue (2025-01)
- **Problem**: H2 headings would reset to 1 after H3 instead of continuing (e.g., 1 → 2)
- **Root cause**: Missing parent level tracking when moving from child to parent
- **Solution**: Added `parentLevels` Map/Dict to track parent hierarchy and only reset when actually entering a new parent section
- **Files modified**: `DynamicWordDocumentCreator.java`, `template_creator.py`

## Environment Requirements

- **Java**: 17 required
- **Maven**: 3.8.1+
- **Python**: 3.8+ (for Python MCP server)
- **Build tool**: Maven for Java, pip for Python

## API Endpoints (Java Service)

- `POST /api/markdown/convert/file` - Convert uploaded Markdown file
- `POST /api/markdown/convert/text` - Convert Markdown text content
- `GET /api/markdown/files/{fileName}` - Download converted Word file
- `SSE /dataReport/md2doc` - MCP server endpoint

## Testing Files

- Java test: `md2doc-service/src/test/java/.../Md2docServiceApplicationTests.java`
- Python test: `md2doc-service-python/tests/test_converter.py`
- Sample Markdown: Various `demo_report*.md`, `test_*.md` files in root
