"""
ECharts 图表渲染器
将 ECharts JSON 配置渲染为 PNG 图片
"""

import os
import json
import tempfile
import hashlib
from typing import Optional, Tuple
from pyecharts import options as opts
from pyecharts.charts import Bar, Line, Pie, Scatter
from pyecharts.render import make_snapshot
from snapshot_pyppeteer import snapshot


class EChartsRenderer:
    """ECharts 图表渲染器

    功能：
    - 解析 ECharts JSON 配置
    - 使用 pyecharts 重建图表
    - 渲染为 PNG 图片
    - 缓存已渲染的图表
    """

    # 图表默认尺寸
    DEFAULT_WIDTH = "800px"
    DEFAULT_HEIGHT = "500px"

    # 缓存目录
    _cache_dir = None

    @classmethod
    def get_cache_dir(cls) -> str:
        """获取缓存目录

        Returns:
            缓存目录路径
        """
        if cls._cache_dir is None:
            cls._cache_dir = os.path.join(tempfile.gettempdir(), 'md2doc_echarts_cache')
            os.makedirs(cls._cache_dir, exist_ok=True)
        return cls._cache_dir

    @staticmethod
    def _preprocess_json(echarts_config: str) -> str:
        """预处理 ECharts JSON 配置

        Args:
            echarts_config: ECharts 配置字符串

        Returns:
            标准 JSON 字符串
        """
        import re

        json_str = echarts_config

        # 处理键名，给没有引号的键添加引号
        json_str = re.sub(r'([{,])\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\s*:', r'\1"\2":', json_str)

        # 处理单引号为双引号
        json_str = json_str.replace("'", '"')

        # 处理末尾的逗号
        json_str = re.sub(r',\s*([}\]])', r'\1', json_str)

        return json_str

    @staticmethod
    def render_bar_chart(config: dict, output_path: str) -> bool:
        """渲染柱状图

        Args:
            config: ECharts 配置字典
            output_path: 输出文件路径

        Returns:
            是否成功
        """
        try:
            # 提取数据
            title = config.get('title', {}).get('text', '柱状图')
            x_axis_data = config.get('xAxis', {})
            if isinstance(x_axis_data, list):
                x_axis_data = x_axis_data[0]
            x_data = x_axis_data.get('data', [])

            series_list = config.get('series', [])
            if not isinstance(series_list, list):
                series_list = [series_list]

            # 创建柱状图
            bar = Bar(init_opts=opts.InitOpts(
                width=EChartsRenderer.DEFAULT_WIDTH,
                height=EChartsRenderer.DEFAULT_HEIGHT
            ))
            bar.add_xaxis(x_data)

            # 添加系列数据
            for series in series_list:
                series_name = series.get('name', '数据')
                series_data = series.get('data', [])
                bar.add_yaxis(series_name, series_data)

            # 设置全局配置
            bar.set_global_opts(
                title_opts=opts.TitleOpts(title=title),
                xaxis_opts=opts.AxisOpts(name=x_axis_data.get('name', '')),
                yaxis_opts=opts.AxisOpts(name=config.get('yAxis', {}).get('name', '')),
                toolbox_opts=opts.ToolboxOpts(),
                legend_opts=opts.LegendOpts(pos_top="5%")
            )

            # 渲染为图片
            make_snapshot(snapshot, bar.render(), output_path)
            return True

        except Exception as e:
            print(f"渲染柱状图失败: {e}")
            return False

    @staticmethod
    def render_line_chart(config: dict, output_path: str) -> bool:
        """渲染折线图

        Args:
            config: ECharts 配置字典
            output_path: 输出文件路径

        Returns:
            是否成功
        """
        try:
            # 提取数据
            title = config.get('title', {}).get('text', '折线图')
            x_axis_data = config.get('xAxis', {})
            if isinstance(x_axis_data, list):
                x_axis_data = x_axis_data[0]
            x_data = x_axis_data.get('data', [])

            series_list = config.get('series', [])
            if not isinstance(series_list, list):
                series_list = [series_list]

            # 创建折线图
            line = Line(init_opts=opts.InitOpts(
                width=EChartsRenderer.DEFAULT_WIDTH,
                height=EChartsRenderer.DEFAULT_HEIGHT
            ))
            line.add_xaxis(x_data)

            # 添加系列数据
            for series in series_list:
                series_name = series.get('name', '数据')
                series_data = series.get('data', [])
                line.add_yaxis(series_name, series_data, is_smooth=True)

            # 设置全局配置
            line.set_global_opts(
                title_opts=opts.TitleOpts(title=title),
                xaxis_opts=opts.AxisOpts(name=x_axis_data.get('name', '')),
                yaxis_opts=opts.AxisOpts(name=config.get('yAxis', {}).get('name', '')),
                toolbox_opts=opts.ToolboxOpts(),
                legend_opts=opts.LegendOpts(pos_top="5%")
            )

            # 渲染为图片
            make_snapshot(snapshot, line.render(), output_path)
            return True

        except Exception as e:
            print(f"渲染折线图失败: {e}")
            return False

    @staticmethod
    def render_pie_chart(config: dict, output_path: str) -> bool:
        """渲染饼图

        Args:
            config: ECharts 配置字典
            output_path: 输出文件路径

        Returns:
            是否成功
        """
        try:
            # 提取数据
            title = config.get('title', {}).get('text', '饼图')
            series_list = config.get('series', [])
            if not isinstance(series_list, list):
                series_list = [series_list]

            # 创建饼图
            pie = Pie(init_opts=opts.InitOpts(
                width=EChartsRenderer.DEFAULT_WIDTH,
                height=EChartsRenderer.DEFAULT_HEIGHT
            ))

            # 添加数据
            for series in series_list:
                series_name = series.get('name', '数据')
                series_data = series.get('data', [])

                # 转换数据格式
                pie_data = []
                if isinstance(series_data, list):
                    # 如果是 [{name: 'A', value: 10}, ...] 格式
                    if series_data and isinstance(series_data[0], dict):
                        pie_data = [(item.get('name', ''), item.get('value', 0)) for item in series_data]
                    else:
                        # 如果是简单的数值列表，需要配合 x 轴数据
                        x_data = config.get('xAxis', {}).get('data', [])
                        if isinstance(x_data, dict):
                            x_data = x_data.get('data', [])
                        pie_data = list(zip(x_data, series_data)) if x_data else [(f'项目{i+1}', v) for i, v in enumerate(series_data)]

                pie.add(series_name, pie_data, radius=["30%", "75%"])

            # 设置全局配置
            pie.set_global_opts(
                title_opts=opts.TitleOpts(title=title),
                legend_opts=opts.LegendOpts(pos_left="left", orient="vertical")
            )
            pie.set_series_opts(label_opts=opts.LabelOpts(formatter="{b}: {c} ({d}%)"))

            # 渲染为图片
            make_snapshot(snapshot, pie.render(), output_path)
            return True

        except Exception as e:
            print(f"渲染饼图失败: {e}")
            return False

    @staticmethod
    def render_chart(echarts_config: str) -> Optional[Tuple[bytes, int, int]]:
        """渲染 ECharts 图表为 PNG 图片

        Args:
            echarts_config: ECharts JSON 配置字符串

        Returns:
            (图片数据, 宽度, 高度) 或 None
        """
        try:
            # 检查缓存
            cache_key = hashlib.md5(echarts_config.encode()).hexdigest()
            cache_file = os.path.join(EChartsRenderer.get_cache_dir(), f"{cache_key}.png")

            if os.path.exists(cache_file):
                with open(cache_file, 'rb') as f:
                    image_data = f.read()
                # 假设固定尺寸（可以后续改进读取实际尺寸）
                return image_data, 600, 400

            # 预处理 JSON
            json_str = EChartsRenderer._preprocess_json(echarts_config)
            config = json.loads(json_str)

            # 检测图表类型
            series = config.get('series', [])
            if isinstance(series, list) and series:
                chart_type = series[0].get('type', 'bar')
            else:
                chart_type = 'bar'

            # 临时文件
            temp_output = os.path.join(EChartsRenderer.get_cache_dir(), f"{cache_key}_temp.png")

            # 根据类型渲染
            success = False
            if chart_type == 'bar':
                success = EChartsRenderer.render_bar_chart(config, temp_output)
            elif chart_type == 'line':
                success = EChartsRenderer.render_line_chart(config, temp_output)
            elif chart_type == 'pie':
                success = EChartsRenderer.render_pie_chart(config, temp_output)
            else:
                # 默认使用柱状图
                print(f"不支持的图表类型: {chart_type}，使用柱状图代替")
                success = EChartsRenderer.render_bar_chart(config, temp_output)

            if not success or not os.path.exists(temp_output):
                print(f"图表渲染失败")
                return None

            # 读取生成的图片
            with open(temp_output, 'rb') as f:
                image_data = f.read()

            # 保存到缓存
            with open(cache_file, 'wb') as f:
                f.write(image_data)

            # 删除临时文件
            if os.path.exists(temp_output):
                os.remove(temp_output)

            # 删除 pyecharts 生成的 HTML 文件
            temp_html = temp_output.replace('.png', '.html')
            if os.path.exists(temp_html):
                os.remove(temp_html)

            return image_data, 600, 400

        except Exception as e:
            print(f"渲染 ECharts 图表失败: {e}")
            import traceback
            traceback.print_exc()
            return None
