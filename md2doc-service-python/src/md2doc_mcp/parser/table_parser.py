"""
Markdown 表格解析器
对应 Java 版本的 MarkdownTableParser.java
"""

from typing import List


class MarkdownTableParser:
    """Markdown 表格解析器
    
    解析 Markdown 表格字符串为二维列表
    使用正则表达式匹配表格，跳过分隔行
    """
    
    @staticmethod
    def parse_table(markdown_table: str) -> List[List[str]]:
        """解析 Markdown 表格字符串为二维列表
        
        Args:
            markdown_table: Markdown 表格字符串
            
        Returns:
            表格数据的二维列表
        """
        table_data: List[List[str]] = []
        
        lines = markdown_table.split('\n')
        for line in lines:
            line = line.strip()
            
            # 跳过分隔行（只包含|和-的行）
            if MarkdownTableParser._is_separator_line(line):
                continue
            
            if line.startswith('|'):
                line = line[1:]  # 移除开头的 |
            
            if line.endswith('|'):
                line = line[:-1]  # 移除结尾的 |
            
            cells = line.split('|')
            row = []
            for cell in cells:
                row.append(cell.strip())
            
            # 只有当行不为空时才添加到表格数据中
            if row and not (len(row) == 1 and row[0] == ''):
                table_data.append(row)
        
        return table_data
    
    @staticmethod
    def _is_separator_line(line: str) -> bool:
        """判断是否为分隔行
        
        Args:
            line: 要检查的行
            
        Returns:
            如果是分隔行则返回 True
        """
        # 匹配只包含 | 和 - 的行，且包含 -
        import re
        pattern = r'^\|?\s*[-|:\s]+\|?\s*$'
        return bool(re.match(pattern, line)) and '-' in line

