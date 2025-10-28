"""
图表数据模型，管理图表的完整数据结构
对应 Java 版本的 ChartTable.java
"""

from typing import Dict, Union
from .chart_column import ChartColumn


class ChartTable:
    """图表数据表模型
    
    管理图表的完整数据结构，包含标题、X轴数据、多个Y轴数据
    """
    
    def __init__(self, title: str = ""):
        """初始化图表表
        
        Args:
            title: 图表标题
        """
        self.title = title
        self.x_axis = ChartColumn[str]("x轴")
        self.y_axis: Dict[str, ChartColumn[Union[int, float]]] = {}
    
    def set_title(self, title: str) -> 'ChartTable':
        """设置图表标题
        
        Args:
            title: 新的标题
            
        Returns:
            返回自身以支持链式调用
        """
        self.title = title
        return self
    
    def new_y_axis(self, title: str) -> ChartColumn[Union[int, float]]:
        """创建新的Y轴
        
        Args:
            title: Y轴标题
            
        Returns:
            新创建的Y轴列对象
        """
        column = ChartColumn[Union[int, float]]()
        column.set_title(title)
        self.y_axis[title] = column
        return column
    
    def get_y_axis(self, title: str) -> ChartColumn[Union[int, float]]:
        """获取指定标题的Y轴
        
        Args:
            title: Y轴标题
            
        Returns:
            Y轴列对象，如果不存在则返回None
        """
        return self.y_axis.get(title)
    
    def get_x_axis(self) -> ChartColumn[str]:
        """获取X轴数据
        
        Returns:
            X轴列对象
        """
        return self.x_axis
    
    def get_y_axis_dict(self) -> Dict[str, ChartColumn[Union[int, float]]]:
        """获取所有Y轴数据
        
        Returns:
            Y轴字典
        """
        return self.y_axis

