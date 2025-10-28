"""
Word 参数模型，管理所有占位符参数（文本、表格、图表）
对应 Java 版本的 WordParams.java
"""

from typing import Dict, List, Union, Any
from .chart_table import ChartTable


class WordParam:
    """Word 参数基类"""
    pass


class TextParam(WordParam):
    """文本参数"""
    def __init__(self, msg: str):
        self.msg = msg


class TableParam(WordParam):
    """表格参数"""
    def __init__(self, data: List[List[str]]):
        self.data = data


class ImageParam(WordParam):
    """图像参数"""
    def __init__(self, image_data: bytes, width: int, height: int):
        self.image_data = image_data
        self.width = width
        self.height = height


class WordParams:
    """Word 参数管理器
    
    管理所有占位符参数（文本、表格、图表）
    提供添加和获取参数的方法
    """
    
    def __init__(self):
        """初始化参数管理器"""
        self.params: Dict[str, WordParam] = {}
        self.chart_map: Dict[str, ChartTable] = {}
    
    def set_param(self, key: str, value: WordParam) -> None:
        """设置参数
        
        Args:
            key: 参数键
            value: 参数值
        """
        self.params[key] = value
    
    def set_text(self, key: str, value: Any) -> None:
        """设置文本参数
        
        Args:
            key: 参数键
            value: 文本值
        """
        self.set_param(key, TextParam(str(value)))
    
    def set_chart(self, key: str, chart_table: ChartTable) -> None:
        """设置图表参数
        
        Args:
            key: 参数键
            chart_table: 图表表对象
        """
        self.chart_map[key] = chart_table
    
    def add_chart(self, key: str) -> ChartTable:
        """添加图表
        
        Args:
            key: 图表键
            
        Returns:
            新创建的图表表对象
        """
        table = ChartTable(key)
        self.set_chart(key, table)
        return table
    
    def get_param(self, key: str) -> WordParam:
        """获取参数
        
        Args:
            key: 参数键
            
        Returns:
            参数对象，如果不存在则返回None
        """
        return self.params.get(key)
    
    def get_chart(self, key: str) -> ChartTable:
        """获取图表
        
        Args:
            key: 图表键
            
        Returns:
            图表表对象，如果不存在则返回None
        """
        return self.chart_map.get(key)
    
    @staticmethod
    def create() -> 'WordParams':
        """创建新的参数管理器
        
        Returns:
            新的 WordParams 实例
        """
        return WordParams()
    
    # 静态方法用于创建不同类型的参数
    @staticmethod
    def text(msg: Any) -> TextParam:
        """创建文本参数
        
        Args:
            msg: 文本内容
            
        Returns:
            文本参数对象
        """
        return TextParam(str(msg))
    
    @staticmethod
    def table(data: List[List[str]]) -> TableParam:
        """创建表格参数
        
        Args:
            data: 表格数据
            
        Returns:
            表格参数对象
        """
        return TableParam(data)
    
    @staticmethod
    def image(image_data: bytes, width: int, height: int) -> ImageParam:
        """创建图像参数
        
        Args:
            image_data: 图像数据
            width: 图像宽度
            height: 图像高度
            
        Returns:
            图像参数对象
        """
        return ImageParam(image_data, width, height)

