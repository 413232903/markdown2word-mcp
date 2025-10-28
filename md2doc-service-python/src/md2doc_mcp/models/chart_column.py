"""
图表列数据模型，用于存储图表的列数据（X轴或Y轴）
对应 Java 版本的 ChartColumn.java
"""

from typing import List, TypeVar, Generic, Iterator, Collection
from dataclasses import dataclass, field

T = TypeVar('T')


@dataclass
class ChartColumn(Generic[T]):
    """图表列数据模型
    
    用于存储图表的列数据，支持 X 轴和 Y 轴数据
    支持添加数据、设置标题、迭代等功能
    """
    title: str = ""
    data_list: List[T] = field(default_factory=list)
    
    def __init__(self, title: str = ""):
        """初始化图表列
        
        Args:
            title: 列标题
        """
        self.title = title
        self.data_list = []
    
    def set_title(self, title: str) -> 'ChartColumn[T]':
        """设置列标题
        
        Args:
            title: 新的标题
            
        Returns:
            返回自身以支持链式调用
        """
        self.title = title
        return self
    
    def size(self) -> int:
        """获取数据数量
        
        Returns:
            数据项的数量
        """
        return len(self.data_list)
    
    def add_all_data(self, *data: T) -> None:
        """添加多个数据项
        
        Args:
            *data: 要添加的数据项
        """
        self.data_list.extend(data)
    
    def add_all_data_from_collection(self, data: Collection[T]) -> None:
        """从集合中添加数据
        
        Args:
            data: 数据集合
        """
        self.data_list.extend(data)
    
    def __iter__(self) -> Iterator[T]:
        """支持迭代"""
        return iter(self.data_list)
    
    def to_array(self, array_type: type) -> List[T]:
        """转换为数组
        
        Args:
            array_type: 数组类型（为了兼容 Java 版本）
            
        Returns:
            数据列表
        """
        return self.data_list.copy()

