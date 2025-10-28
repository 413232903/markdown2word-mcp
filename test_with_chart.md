# 测试文档 - 包含一个图表

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

## 第二部分

### 数据表格

| 姓名 | 年龄 | 职位 |
|-----|------|------|
| 张三 | 28 | 工程师 |
| 李四 | 32 | 经理 |

## 结束

测试完成。
