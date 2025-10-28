# 图文并茂测试文档

## 项目概述

本项目是一个 Markdown 转 Word 的转换工具，支持图片和图表的展示。

## 功能特性

### 图片支持

下面是一张网络图片示例：

![Python Logo](https://www.python.org/static/community_logos/python-logo-generic.svg "Python官方Logo")

### ECharts 图表支持

#### 柱状图示例

```echarts
{
  title: { text: '月度销售数据' },
  xAxis: { data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
  yAxis: { name: '销售额(万元)' },
  series: [{
    name: '销售额',
    type: 'bar',
    data: [120, 200, 150, 180, 220, 260]
  }]
}
```

#### 折线图示例

```echarts
{
  title: { text: '用户增长趋势' },
  xAxis: { data: ['周一', '周二', '周三', '周四', '周五'] },
  yAxis: { name: '用户数' },
  series: [{
    name: '新增用户',
    type: 'line',
    data: [150, 230, 224, 218, 350]
  }]
}
```

## 数据表格

| 功能 | 状态 | 进度 |
|------|------|------|
| Markdown 解析 | ✅ 完成 | 100% |
| 图片支持 | ✅ 完成 | 100% |
| 图表渲染 | ✅ 完成 | 100% |
| 表格转换 | ✅ 完成 | 100% |

## 总结

通过本工具，可以将 Markdown 文档转换为包含图片和图表的精美 Word 文档！
