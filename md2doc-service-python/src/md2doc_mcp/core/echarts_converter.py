"""
ECharts 图表转换器
对应 Java 版本的 EChartsToWordConverter.java
"""

import json
import re
from typing import List, Dict, Any, Union
from ..models.word_params import WordParams
from ..models.chart_table import ChartTable


class EChartsToWordConverter:
    """ECharts 图表转换为 Word 图表的工具类
    
    解析 ECharts JSON 配置，提取图表数据（标题、X轴、Y轴、系列）
    转换为 ChartTable 对象，支持柱状图、折线图、饼图
    """
    
    @staticmethod
    def convert_echarts_to_word_chart(params: WordParams, chart_key: str, echarts_config: str) -> None:
        """将 ECharts 配置转换为 Word 图表
        
        Args:
            params: Word 参数对象
            chart_key: 图表键名
            echarts_config: ECharts 配置 JSON 字符串
            
        Raises:
            Exception: JSON 解析异常或其他转换错误
        """
        try:
            # 预处理ECharts配置，将其转换为有效的JSON格式
            json_config = EChartsToWordConverter.convert_echarts_to_json(echarts_config)
            
            # 解析 JSON
            root_node = json.loads(json_config)
            
            # 获取图表标题
            title = root_node.get('title', {}).get('text', '默认标题')
            
            # 创建图表
            chart_table = params.add_chart(chart_key).set_title(title)
            
            # 处理 X 轴数据
            x_axis_node = root_node.get('xAxis')
            if isinstance(x_axis_node, list):
                x_axis_node = x_axis_node[0]  # 多个 x 轴时取第一个
            
            if x_axis_node:
                x_axis_data = x_axis_node.get('data', [])
                if x_axis_data:
                    chart_table.get_x_axis().add_all_data(*x_axis_data)
            
            # 处理 Y 轴数据和系列数据
            series_node = root_node.get('series', [])
            if isinstance(series_node, list):
                for serie in series_node:
                    series_name = serie.get('name', '数据系列')
                    series_data = serie.get('data', [])
                    
                    if series_data:
                        # 将字符串数字转换为数字类型
                        numeric_data = []
                        for data_item in series_data:
                            if isinstance(data_item, (int, float)):
                                numeric_data.append(data_item)
                            elif isinstance(data_item, str):
                                try:
                                    numeric_data.append(float(data_item))
                                except ValueError:
                                    numeric_data.append(0)
                            else:
                                numeric_data.append(0)
                        
                        chart_table.new_y_axis(series_name).add_all_data(*numeric_data)
            
            # 如果有 Y 轴名称设置，更新第一个 Y 轴的标题
            y_axis_node = root_node.get('yAxis')
            if isinstance(y_axis_node, list):
                y_axis_node = y_axis_node[0]  # 多个 y 轴时取第一个
            
            if y_axis_node:
                y_axis_name = y_axis_node.get('name', '')
                if y_axis_name and chart_table.get_y_axis_dict():
                    # 获取第一个 Y 轴并设置标题
                    first_key = list(chart_table.get_y_axis_dict().keys())[0]
                    chart_table.get_y_axis(first_key).set_title(y_axis_name)
                    
        except Exception as e:
            # 如果解析失败，创建一个默认的空图表
            chart_table = params.add_chart(chart_key).set_title("默认图表标题")
            chart_table.get_x_axis().add_all_data("数据1", "数据2", "数据3")
            chart_table.new_y_axis("默认系列").add_all_data(10, 20, 30)
            raise Exception(f"解析ECharts配置时出错: {str(e)}") from e
    
    @staticmethod
    def convert_echarts_to_json(echarts_config: str) -> str:
        """将ECharts配置转换为有效的JSON格式
        
        Args:
            echarts_config: ECharts配置字符串
            
        Returns:
            有效的JSON字符串
        """
        json_str = echarts_config
        
        # 处理键名，给没有引号的键添加引号
        # 匹配键名（以字母、下划线或$开头，后跟字母、数字、下划线或$）
        json_str = re.sub(r'([{,])\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\s*:', r'\1"\2":', json_str)
        
        # 处理单引号为双引号
        json_str = json_str.replace("'", '"')
        
        # 处理末尾的逗号（在}或]之前）
        json_str = re.sub(r',\s*([}\]])', r'\1', json_str)
        
        return json_str
    
    @staticmethod
    def create_bar_chart(params: WordParams, chart_key: str, title: str,
                        x_axis_labels: List[str], series_name: str, series_data: List[Union[int, float]]) -> None:
        """简化版本：直接根据数据创建柱状图
        
        Args:
            params: Word 参数对象
            chart_key: 图表键名
            title: 图表标题
            x_axis_labels: X 轴标签
            series_name: 系列名称
            series_data: 系列数据
        """
        chart_table = params.add_chart(chart_key).set_title(title)
        chart_table.get_x_axis().add_all_data(*x_axis_labels)
        chart_table.new_y_axis(series_name).add_all_data(*series_data)
    
    @staticmethod
    def extract_chart_type(echarts_config: str) -> str:
        """提取图表类型
        
        Args:
            echarts_config: ECharts配置字符串
            
        Returns:
            图表类型（bar/line/pie）
        """
        try:
            json_config = EChartsToWordConverter.convert_echarts_to_json(echarts_config)
            root_node = json.loads(json_config)
            
            series_node = root_node.get('series', [])
            if isinstance(series_node, list) and series_node:
                chart_type = series_node[0].get('type', 'bar')
                return chart_type
            
            return 'bar'  # 默认为柱状图
        except Exception:
            return 'bar'  # 解析失败时默认为柱状图
