#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""直接调用转换器生成Word文档"""

import sys
import os

# 添加项目路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'md2doc-service-python', 'src'))

from md2doc_mcp.core.converter import MarkdownToWordConverter

# 读取Markdown文件
markdown_file = 'test_network_image.md'
output_file = 'test_network_image_output.docx'

# 创建转换器
converter = MarkdownToWordConverter()

# 执行转换
print(f"正在转换 {markdown_file} 到 {output_file}...")
try:
    converter.convert_markdown_file_to_word(markdown_file, output_file)
    print(f"✓ 转换成功！文件已保存为: {output_file}")
    
    # 检查文件是否存在
    if os.path.exists(output_file):
        file_size = os.path.getsize(output_file)
        print(f"✓ 文件大小: {file_size} 字节")
    else:
        print("✗ 错误：文件未生成")
except Exception as e:
    print(f"✗ 转换失败: {e}")
    import traceback
    traceback.print_exc()



