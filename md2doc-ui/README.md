# md2doc-ui

Markdown 转 Word 工具的前端界面，基于 Vue 3 和 Vite 构建。

## 功能特点

- 美观的用户界面设计
- 支持文本转换和文件上传两种方式
- 拖拽上传文件功能
- 实时错误提示
- 响应式设计，适配不同屏幕尺寸

## 技术栈

- Vue 3
- Vite
- Axios

## 项目结构

```
md2doc-ui/
├── src/
│   ├── components/
│   │   ├── Header.vue       # 页面头部
│   │   ├── MainContent.vue  # 主要内容区域
│   │   └── Footer.vue       # 页面底部
│   ├── App.vue              # 根组件
│   └── main.js              # 入口文件
├── public/                  # 静态资源
├── index.html               # 主页面
└── vite.config.js           # Vite 配置文件
```

## 安装和运行

### 安装依赖

```bash
npm install
```

### 开发环境运行

```bash
npm run dev
```

默认访问地址: http://localhost:3000

### 构建生产版本

```bash
npm run build
```

## API 接口

前端通过以下接口与后端服务通信：

1. 文本转换: `POST /api/markdown/convert/text`
2. 文件转换: `POST /api/markdown/convert/file`

## 支持的 Markdown 语法

- 标题 (H1-H6)
- 段落文本
- 表格
- ECharts 图表代码块 (使用 ```echarts 代码块)