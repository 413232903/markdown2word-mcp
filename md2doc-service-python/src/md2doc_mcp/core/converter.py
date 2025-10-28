"""
主转换器
对应 Java 版本的 MarkdownToWordConverter.java
"""

import os
import tempfile
import time
from typing import List
from ..models.word_params import WordParams
from ..parser.markdown_parser import MarkdownParser
from ..parser.table_parser import MarkdownTableParser
from .template_creator import DynamicWordDocumentCreator
from .word_generator import PoiWordGenerator
from .echarts_converter import EChartsToWordConverter
from .echarts_renderer import EChartsRenderer
from ..utils.image_downloader import ImageDownloader


class MarkdownToWordConverter:
    """完整的Markdown到Word转换器
    
    支持文字、表格和ECharts图表的转换
    协调所有组件完成完整的转换流程
    """
    
    def __init__(self):
        """初始化转换器"""
        pass
    
    def convert_markdown_file_to_word(self, markdown_file: str, output_file: str) -> None:
        """将Markdown文件转换为Word文档
        
        Args:
            markdown_file: Markdown文件路径
            output_file: 输出Word文件路径
            
        Raises:
            Exception: 转换过程中可能抛出的异常
        """
        try:
            # 读取Markdown文件内容
            with open(markdown_file, 'r', encoding='utf-8') as f:
                markdown_content = f.read()
            
            # 调用文本转换方法
            self.convert_markdown_to_word(markdown_content, output_file)
            
        except Exception as e:
            raise Exception(f"转换Markdown文件时出错: {str(e)}") from e
    
    def convert_markdown_to_word(self, markdown_content: str, output_file: str) -> None:
        """将Markdown内容转换为Word文档
        
        Args:
            markdown_content: Markdown内容
            output_file: 输出Word文件路径
            
        Raises:
            Exception: 转换过程中可能抛出的异常
        """
        start_time = time.time()
        
        try:
            # 创建临时模板文件
            template_file = output_file.replace('.docx', '_template.docx')
            
            # 使用新的方法创建完整模板，更好地保持Markdown结构
            DynamicWordDocumentCreator.create_complete_template_from_markdown(template_file, markdown_content)
            
            # 创建参数对象
            params = WordParams.create()
            
            # 处理ECharts图表
            self._process_echarts(params, markdown_content)

            # 处理图片
            self._process_images(params, markdown_content)

            # 处理表格
            self._process_tables(params, markdown_content)
            
            # 处理文本内容
            self._process_text_content(params, markdown_content)
            
            # 生成Word文档
            success = PoiWordGenerator.build_doc(params, template_file, output_file)
            
            if not success:
                raise Exception("生成Word文档失败")
            
            # 删除临时模板文件
            if os.path.exists(template_file):
                os.remove(template_file)
            
            end_time = time.time()
            print(f"Markdown文档已成功转换为Word文档: {output_file}，耗时: {(end_time - start_time)*1000:.0f}ms")
            
        except Exception as e:
            # 清理临时文件
            template_file = output_file.replace('.docx', '_template.docx')
            if os.path.exists(template_file):
                try:
                    os.remove(template_file)
                except:
                    pass
            
            raise Exception(f"转换Markdown内容时出错: {str(e)}") from e
    
    def _process_echarts(self, params: WordParams, markdown_content: str) -> None:
        """处理ECharts图表 - 渲染为图片

        Args:
            params: Word参数对象
            markdown_content: Markdown内容
        """
        try:
            echarts_blocks = MarkdownParser.extract_echarts_blocks(markdown_content)
            chart_index = 1

            for echarts_config in echarts_blocks:
                chart_key = f"chart{chart_index}"

                print(f"正在渲染图表 {chart_index}...")

                # 使用新的渲染器将 ECharts 渲染为图片
                result = EChartsRenderer.render_chart(echarts_config)

                if result:
                    image_data, width, height = result
                    # 将图片作为 ImageParam 添加
                    params.set_param(chart_key, WordParams.image(image_data, width, height))
                    print(f"图表 {chart_index} 渲染成功 ({width}x{height})")
                else:
                    print(f"图表 {chart_index} 渲染失败，使用数据表格代替")
                    # 如果渲染失败，使用原来的数据表格方式
                    EChartsToWordConverter.convert_echarts_to_word_chart(params, chart_key, echarts_config)

                chart_index += 1

        except Exception as e:
            print(f"处理ECharts图表时出错: {e}")
            import traceback
            traceback.print_exc()

    def _process_images(self, params: WordParams, markdown_content: str) -> None:
        """处理 Markdown 图片

        Args:
            params: Word参数对象
            markdown_content: Markdown内容
        """
        try:
            images = MarkdownParser.extract_images(markdown_content)
            image_index = 1

            for alt_text, image_url, title in images:
                image_key = f"image{image_index}"

                print(f"正在处理图片 {image_index}: {image_url}...")

                # 下载/读取并处理图片
                result = ImageDownloader.process_image(image_url)

                if result:
                    image_data, width, height = result
                    # 将图片作为 ImageParam 添加
                    params.set_param(image_key, WordParams.image(image_data, width, height))
                    print(f"图片 {image_index} 处理成功 ({width}x{height})")
                else:
                    print(f"图片 {image_index} 处理失败: {image_url}")
                    # 如果失败，添加一个占位文本
                    params.set_text(image_key, f"[图片加载失败: {alt_text}]")

                image_index += 1

        except Exception as e:
            print(f"处理图片时出错: {e}")
            import traceback
            traceback.print_exc()

    def _process_tables(self, params: WordParams, markdown_content: str) -> None:
        """处理表格
        
        Args:
            params: Word参数对象
            markdown_content: Markdown内容
        """
        try:
            table_blocks = MarkdownParser.extract_tables(markdown_content)
            table_index = 1
            
            for table_markdown in table_blocks:
                table_data = MarkdownTableParser.parse_table(table_markdown)
                table_key = f"table{table_index}"
                
                params.set_param(table_key, WordParams.table(table_data))
                table_index += 1
                
        except Exception as e:
            print(f"处理表格时出错: {e}")
            # 继续处理其他内容，不中断整个转换过程
    
    def _process_text_content(self, params: WordParams, markdown_content: str) -> None:
        """处理文本内容
        
        Args:
            params: Word参数对象
            markdown_content: Markdown内容
        """
        try:
            # 提取标题作为文档标题
            first_header = MarkdownParser.extract_first_header(markdown_content)
            if first_header:
                params.set_text("title", first_header)
            else:
                params.set_text("title", "默认标题")
            
            # 可以添加更多文本处理逻辑
            # 例如提取作者、日期等信息
            
        except Exception as e:
            print(f"处理文本内容时出错: {e}")
            # 设置默认标题
            params.set_text("title", "默认标题")
    
    def get_supported_features(self) -> List[str]:
        """获取支持的功能列表
        
        Returns:
            支持的功能列表
        """
        return [
            "Markdown 标题 (H1-H6)",
            "Markdown 段落文本",
            "Markdown 表格",
            "ECharts 图表代码块",
            "标题自动编号",
            "格式化样式"
        ]
    
    def validate_markdown_content(self, markdown_content: str) -> bool:
        """验证Markdown内容是否有效
        
        Args:
            markdown_content: Markdown内容
            
        Returns:
            内容是否有效
        """
        try:
            # 基本验证：检查内容不为空
            if not markdown_content or not markdown_content.strip():
                return False
            
            # 可以添加更多验证逻辑
            # 例如检查是否有有效的Markdown语法
            
            return True
            
        except Exception:
            return False
