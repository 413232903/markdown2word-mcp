# 测试文档 - 包含三个图表

## 第一部分

这是测试文档的第一部分。

## 数据可视化

### 用户增长趋势

```echarts
{
  title: { text: '月度用户增长', left: 'center' },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: ['1月', '2月', '3月', '4月', '5月', '6月']
  },
  yAxis: {
    type: 'value',
    name: '用户数(万)'
  },
  series: [{
    name: '新增用户',
    type: 'line',
    data: [120, 132, 145, 158, 178, 195],
    smooth: true,
    itemStyle: { color: '#5470c6' }
  }]
}
```

### 产品销售对比

```echarts
{
  title: { text: '产品销售额对比', left: 'center' },
  tooltip: { trigger: 'axis' },
  legend: { data: ['产品A', '产品B'], top: '10%' },
  xAxis: {
    type: 'category',
    data: ['Q1', 'Q2', 'Q3', 'Q4']
  },
  yAxis: {
    type: 'value',
    name: '销售额(万元)'
  },
  series: [
    {
      name: '产品A',
      type: 'bar',
      data: [320, 332, 301, 364],
      itemStyle: { color: '#5470c6' }
    },
    {
      name: '产品B',
      type: 'bar',
      data: [220, 182, 191, 234],
      itemStyle: { color: '#91cc75' }
    }
  ]
}
```

### 市场份额分布

```echarts
{
  title: { text: '市场份额分布', left: 'center' },
  tooltip: { trigger: 'item' },
  series: [{
    name: '市场份额',
    type: 'pie',
    radius: '50%',
    data: [
      { value: 335, name: '华东地区' },
      { value: 310, name: '华北地区' },
      { value: 234, name: '华南地区' }
    ]
  }]
}
```

## 数据表格

| 姓名 | 年龄 | 职位 |
|-----|------|------|
| 张三 | 28 | 工程师 |
| 李四 | 32 | 经理 |

## 结束

测试完成。
