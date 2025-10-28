"""
Markdown 解析器
用于解析 Markdown 内容，识别标题、段落、ECharts 代码块等元素
"""

import re
from typing import List, Tuple, Optional


class MarkdownParser:
    """Markdown 解析器
    
    解析 Markdown 内容，识别各种元素：
    - 标题解析（H1-H6）
    - 段落文本提取
    - ECharts 代码块识别
    """
    
    # 用于匹配标题的正则表达式
    HEADER_PATTERN = re.compile(r'^(#{1,6})\s+(.*)$', re.MULTILINE)
    
    # 用于匹配ECharts代码块的正则表达式
    ECHARTS_PATTERN = re.compile(
        r'```echarts\s*\n(.*?)\n```',
        re.DOTALL
    )
    
    # 用于匹配表格的正则表达式
    TABLE_PATTERN = re.compile(
        r'(\|[^\n]*\|\s*\n\s*\|[-|:\s]*\|\s*\n(?:\s*\|[^\n]*\|\s*\n?)*)',
        re.MULTILINE
    )

    # 用于匹配 Markdown 图片的正则表达式
    # 格式: ![alt text](image_url "optional title")
    IMAGE_PATTERN = re.compile(
        r'!\[([^\]]*)\]\(([^\s\)]+)(?:\s+"([^"]*)")?\)',
        re.MULTILINE
    )
    
    @staticmethod
    def extract_headers(markdown_content: str) -> List[Tuple[int, str]]:
        """提取标题
        
        Args:
            markdown_content: Markdown 内容
            
        Returns:
            标题列表，每个元素为 (级别, 标题文本) 的元组
        """
        headers = []
        matches = MarkdownParser.HEADER_PATTERN.findall(markdown_content)
        
        for match in matches:
            level = len(match[0])  # # 的数量
            text = match[1].strip()
            headers.append((level, text))
        
        return headers
    
    @staticmethod
    def extract_echarts_blocks(markdown_content: str) -> List[str]:
        """提取 ECharts 代码块
        
        Args:
            markdown_content: Markdown 内容
            
        Returns:
            ECharts 配置字符串列表
        """
        echarts_blocks = []
        matches = MarkdownParser.ECHARTS_PATTERN.findall(markdown_content)
        
        for match in matches:
            echarts_blocks.append(match.strip())
        
        return echarts_blocks
    
    @staticmethod
    def extract_tables(markdown_content: str) -> List[str]:
        """提取表格
        
        Args:
            markdown_content: Markdown 内容
            
        Returns:
            表格 Markdown 字符串列表
        """
        tables = []
        matches = MarkdownParser.TABLE_PATTERN.findall(markdown_content)
        
        for match in matches:
            tables.append(match.strip())
        
        return tables
    
    @staticmethod
    def extract_first_header(markdown_content: str) -> Optional[str]:
        """提取第一个标题作为文档标题
        
        Args:
            markdown_content: Markdown 内容
            
        Returns:
            第一个标题的文本，如果没有标题则返回 None
        """
        headers = MarkdownParser.extract_headers(markdown_content)
        if headers:
            return headers[0][1]  # 返回第一个标题的文本
        return None
    
    @staticmethod
    def split_into_lines(markdown_content: str) -> List[str]:
        """将 Markdown 内容分割为行
        
        Args:
            markdown_content: Markdown 内容
            
        Returns:
            行列表
        """
        return markdown_content.split('\n')
    
    @staticmethod
    def is_header_line(line: str) -> Optional[Tuple[int, str]]:
        """判断是否为标题行
        
        Args:
            line: 要检查的行
            
        Returns:
            如果是标题则返回 (级别, 标题文本)，否则返回 None
        """
        match = MarkdownParser.HEADER_PATTERN.match(line.strip())
        if match:
            level = len(match.group(1))
            text = match.group(2).strip()
            return (level, text)
        return None
    
    @staticmethod
    def is_echarts_start_line(line: str) -> bool:
        """判断是否为 ECharts 代码块开始行
        
        Args:
            line: 要检查的行
            
        Returns:
            如果是 ECharts 开始行则返回 True
        """
        return line.strip() == '```echarts'
    
    @staticmethod
    def is_echarts_end_line(line: str) -> bool:
        """判断是否为 ECharts 代码块结束行
        
        Args:
            line: 要检查的行
            
        Returns:
            如果是 ECharts 结束行则返回 True
        """
        return line.strip() == '```'
    
    @staticmethod
    def is_table_start_line(line: str) -> bool:
        """判断是否为表格开始行
        
        Args:
            line: 要检查的行
            
        Returns:
            如果是表格开始行则返回 True
        """
        return line.strip().startswith('|')

    @staticmethod
    def extract_images(markdown_content: str) -> List[Tuple[str, str, str]]:
        """提取 Markdown 图片

        Args:
            markdown_content: Markdown 内容

        Returns:
            图片列表，每个元素为 (alt_text, image_url, title) 的元组
        """
        images = []
        matches = MarkdownParser.IMAGE_PATTERN.findall(markdown_content)

        for match in matches:
            alt_text = match[0].strip()
            image_url = match[1].strip()
            title = match[2].strip() if len(match) > 2 else alt_text
            images.append((alt_text, image_url, title))

        return images

    @staticmethod
    def is_image_line(line: str) -> Optional[Tuple[str, str, str]]:
        """判断是否为图片行

        Args:
            line: 要检查的行

        Returns:
            如果是图片则返回 (alt_text, image_url, title)，否则返回 None
        """
        match = MarkdownParser.IMAGE_PATTERN.search(line.strip())
        if match:
            alt_text = match.group(1).strip()
            image_url = match.group(2).strip()
            title = match.group(3).strip() if match.group(3) else alt_text
            return (alt_text, image_url, title)
        return None

