"""
动态 Word 文档模板创建器
对应 Java 版本的 DynamicWordDocumentCreator.java
"""

import re
from typing import List, Tuple
from docx import Document
from docx.shared import Inches, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml.shared import OxmlElement, qn
from docx.oxml.ns import nsdecls
from docx.oxml import parse_xml
from ..parser.markdown_parser import MarkdownParser


def format_number_with_thousands_separator(text: str) -> str:
    """格式化文本中的数字，添加千分符号
    
    例如：1000 -> 1,000，1234567 -> 1,234,567
    
    Args:
        text: 输入文本
        
    Returns:
        格式化后的文本
    """
    # 匹配整数和小数的正则表达式
    def replace_number(match):
        number_str = match.group(0)
        # 如果是小数，分别处理整数部分和小数部分
        if '.' in number_str:
            integer_part, decimal_part = number_str.split('.')
            # 格式化整数部分
            formatted_integer = format(int(integer_part), ',')
            return f"{formatted_integer}.{decimal_part}"
        else:
            # 格式化整数
            return format(int(number_str), ',')
    
    # 匹配数字（包括整数和小数）
    pattern = r'\d+(\.\d+)?'
    return re.sub(pattern, replace_number, text)


class HeaderNumbering:
    """标题编号管理器"""
    
    def __init__(self):
        self.number_stack = []
        self.level_counters = {}
    
    def enter_level(self, level: int) -> None:
        """进入指定级别的标题
        
        Args:
            level: 标题级别 (1-6)
        """
        # 重置更深级别的计数器
        for i in range(level + 1, 7):
            self.level_counters[i] = 0
        
        # 增加当前级别的计数器
        self.level_counters[level] = self.level_counters.get(level, 0) + 1
        
        # 更新栈
        while len(self.number_stack) >= level:
            self.number_stack.pop()
        self.number_stack.append(self.level_counters[level])
    
    def get_number(self) -> str:
        """获取当前编号
        
        Returns:
            编号字符串，如 "1.2.3"
        """
        return '.'.join(map(str, self.number_stack))


class DynamicWordDocumentCreator:
    """动态生成Word文档模板
    
    解析 Markdown 结构，创建 Word 文档框架
    插入占位符（${title}、${chart1}、${table1}）
    设置标题样式（自定义 Heading1-6 样式）
    实现标题自动编号
    设置段落格式（四号仿宋、1.5倍行距、首行缩进2字符）
    """
    
    @staticmethod
    def create_complete_template_from_markdown(file_path: str, markdown_content: str) -> None:
        """根据Markdown内容创建更完整的模板
        
        Args:
            file_path: 输出文件路径
            markdown_content: Markdown内容
        """
        document = Document()
        
        # 创建标题样式
        DynamicWordDocumentCreator._create_header_styles(document)
        
        # 创建标题段落
        title_paragraph = document.add_paragraph()
        title_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        DynamicWordDocumentCreator._set_title_paragraph_style(title_paragraph)
        
        title_run = title_paragraph.add_run("${title}")
        title_run.bold = True
        title_run.font.size = Pt(16)  # 三号字体
        title_run.font.name = '仿宋'
        
        # 添加一个空行
        empty_paragraph = document.add_paragraph()
        DynamicWordDocumentCreator._set_default_paragraph_style(empty_paragraph)
        
        # 解析Markdown内容并创建相应的Word结构
        DynamicWordDocumentCreator._parse_and_create_document_structure(document, markdown_content)
        
        # 保存文档
        document.save(file_path)
    
    @staticmethod
    def _create_header_styles(document: Document) -> None:
        """创建自定义标题样式
        
        Args:
            document: Word文档对象
        """
        styles = document.styles
        
        # 创建标题1-6样式
        heading_configs = [
            ("Heading1", 1, 22),
            ("Heading2", 2, 20),
            ("Heading3", 3, 18),
            ("Heading4", 4, 16),
            ("Heading5", 5, 14),
            ("Heading6", 6, 12),
        ]
        
        for style_name, level, font_size in heading_configs:
            DynamicWordDocumentCreator._create_heading_style(styles, style_name, level, font_size)
    
    @staticmethod
    def _create_heading_style(styles, style_name: str, heading_level: int, font_size: int) -> None:
        """创建标题样式
        
        Args:
            styles: 样式集合
            style_name: 样式名称
            heading_level: 标题级别
            font_size: 字体大小
        """
        try:
            # 尝试获取现有样式 - 遍历查找而不是使用已弃用的ID查找
            existing_style = None
            for style in styles:
                if style.name == style_name:
                    existing_style = style
                    break

            if existing_style is None:
                # 创建新样式
                style = styles.add_style(style_name, WD_STYLE_TYPE.PARAGRAPH)
            else:
                style = existing_style
        except (KeyError, ValueError):
            # 如果样式不存在,创建新样式
            style = styles.add_style(style_name, WD_STYLE_TYPE.PARAGRAPH)
        
        # 设置字体
        font = style.font
        font.name = '仿宋'
        font.size = Pt(font_size)
        font.bold = True
        
        # 设置段落格式
        paragraph_format = style.paragraph_format
        paragraph_format.line_spacing = 1.5
        paragraph_format.space_before = Pt(12)  # 段前间距
        paragraph_format.space_after = Pt(6)  # 段后间距

        # 设置大纲级别（用于目录生成）
        paragraph_format.outline_level = heading_level - 1
    
    @staticmethod
    def _set_default_paragraph_style(paragraph) -> None:
        """设置默认段落样式 - 四号仿宋，1.5倍行距，首行缩进2字符
        
        Args:
            paragraph: 段落对象
        """
        paragraph_format = paragraph.paragraph_format
        paragraph_format.line_spacing = 1.5
        paragraph_format.first_line_indent = Inches(0.5)  # 首行缩进2字符
        paragraph_format.space_before = Pt(0)  # 段前0磅
        paragraph_format.space_after = Pt(0)  # 段后0磅
    
    @staticmethod
    def _set_title_paragraph_style(paragraph) -> None:
        """设置标题段落样式
        
        Args:
            paragraph: 标题段落
        """
        paragraph_format = paragraph.paragraph_format
        paragraph_format.line_spacing = 1.5
        paragraph_format.space_before = Pt(12)  # 标题前留一些空间
        paragraph_format.space_after = Pt(6)  # 标题后留一些空间
    
    @staticmethod
    def _parse_and_create_document_structure(document: Document, markdown_content: str) -> None:
        """解析Markdown内容并创建Word文档结构
        
        Args:
            document: Word文档对象
            markdown_content: Markdown内容
        """
        lines = markdown_content.split('\n')
        chart_index = 1
        table_index = 1
        image_index = 1

        # 初始化标题编号器
        header_numbering = HeaderNumbering()
        
        i = 0
        while i < len(lines):
            line = lines[i]
            
            # 检查是否为标题
            header_info = MarkdownParser.is_header_line(line)
            if header_info:
                level, title = header_info
                
                # 更新标题编号
                header_numbering.enter_level(level)
                header_number = header_numbering.get_number()
                
                header_paragraph = document.add_paragraph()
                DynamicWordDocumentCreator._set_header_style(header_paragraph, level)
                
                header_run = header_paragraph.add_run(f"{header_number} {title}")
                header_run.bold = True
                header_run.font.name = '仿宋'

                # 根据标题级别设置字体大小（标准公文字号）
                # H1: 二号(22pt), H2: 三号(16pt), H3: 四号(14pt), H4: 小四(12pt), H5-H6: 五号(10.5pt)
                font_sizes = {1: 22, 2: 16, 3: 14, 4: 12, 5: 10.5, 6: 10.5}
                header_run.font.size = Pt(font_sizes.get(level, 12))
                
                i += 1
                continue
            
            # 检查是否为ECharts图表
            if MarkdownParser.is_echarts_start_line(line):
                # 查找图表代码块的结束位置
                chart_code_lines = []
                i += 1  # 移动到下一行
                while i < len(lines) and not MarkdownParser.is_echarts_end_line(lines[i]):
                    chart_code_lines.append(lines[i])
                    i += 1
                
                chart_code = '\n'.join(chart_code_lines)
                
                # 创建图表占位符
                chart_title_paragraph = document.add_paragraph()
                chart_title_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                DynamicWordDocumentCreator._set_default_paragraph_style(chart_title_paragraph)
                
                chart_title_run = chart_title_paragraph.add_run(f"图表 {chart_index}：")
                chart_title_run.bold = True
                chart_title_run.font.name = '仿宋'
                chart_title_run.font.size = Pt(14)  # 四号字体
                
                # 创建图表占位符段落
                chart_paragraph = document.add_paragraph()
                chart_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                DynamicWordDocumentCreator._set_default_paragraph_style(chart_paragraph)
                
                chart_run = chart_paragraph.add_run(f"${{chart{chart_index}}}")
                
                chart_index += 1
                i += 1
                continue

            # 检查是否为图片
            image_info = MarkdownParser.is_image_line(line)
            if image_info:
                alt_text, image_url, title = image_info

                # 创建图片标题段落
                image_title_paragraph = document.add_paragraph()
                image_title_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                DynamicWordDocumentCreator._set_default_paragraph_style(image_title_paragraph)

                image_title_run = image_title_paragraph.add_run(f"图片 {image_index}：{title}")
                image_title_run.bold = True
                image_title_run.font.name = '仿宋'
                image_title_run.font.size = Pt(14)  # 四号字体

                # 创建图片占位符段落
                image_paragraph = document.add_paragraph()
                image_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                DynamicWordDocumentCreator._set_default_paragraph_style(image_paragraph)

                image_run = image_paragraph.add_run(f"${{image{image_index}}}")

                image_index += 1
                i += 1
                continue

            # 检查是否为表格开始
            if MarkdownParser.is_table_start_line(line):
                # 收集表格的所有行
                table_lines = [line]
                i += 1  # 移动到下一行
                while i < len(lines) and (lines[i].startswith('|') or 
                                        re.match(r'^\|?\s*[-|:\s]+\|?\s*$', lines[i])):
                    table_lines.append(lines[i])
                    i += 1
                i -= 1  # 回退一行，因为循环会自动增加i
                
                # 创建表格占位符
                table_title_paragraph = document.add_paragraph()
                table_title_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                DynamicWordDocumentCreator._set_default_paragraph_style(table_title_paragraph)
                
                table_title_run = table_title_paragraph.add_run(f"表格 {table_index}：")
                table_title_run.bold = True
                table_title_run.font.name = '仿宋'
                table_title_run.font.size = Pt(14)  # 四号字体
                
                table_paragraph = document.add_paragraph()
                table_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                DynamicWordDocumentCreator._set_default_paragraph_style(table_paragraph)
                
                table_run = table_paragraph.add_run(f"${{table{table_index}}}")
                
                table_index += 1
                i += 1
                continue
            
            # 普通段落
            if line.strip():
                paragraph = document.add_paragraph()
                DynamicWordDocumentCreator._set_default_paragraph_style(paragraph)
                
                # 格式化数字，添加千分符号
                formatted_line = format_number_with_thousands_separator(line)
                run = paragraph.add_run(formatted_line)
                run.font.name = '仿宋'
                run.font.size = Pt(14)  # 四号字体
            
            i += 1
    
    @staticmethod
    def _set_header_style(paragraph, level: int) -> None:
        """设置标题样式
        
        Args:
            paragraph: 标题段落
            level: 标题级别
        """
        style_names = {
            1: "Heading1",
            2: "Heading2", 
            3: "Heading3",
            4: "Heading4",
            5: "Heading5",
            6: "Heading6"
        }
        
        style_name = style_names.get(level, "Heading1")
        try:
            paragraph.style = style_name
        except KeyError:
            # 如果样式不存在，使用默认样式
            pass
