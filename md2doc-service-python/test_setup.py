#!/usr/bin/env python3
"""
md2doc-mcp 安装和测试脚本
"""

import os
import sys
import subprocess
import tempfile
from pathlib import Path


def run_command(command, description):
    """运行命令并显示结果"""
    print(f"\n🔄 {description}...")
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True)
        if result.returncode == 0:
            print(f"✅ {description} 成功")
            if result.stdout:
                print(f"输出: {result.stdout.strip()}")
        else:
            print(f"❌ {description} 失败")
            print(f"错误: {result.stderr.strip()}")
            return False
    except Exception as e:
        print(f"❌ {description} 异常: {e}")
        return False
    return True


def install_dependencies():
    """安装依赖"""
    commands = [
        ("pip install mcp", "安装 MCP SDK"),
        ("pip install python-docx", "安装 python-docx"),
        ("pip install Pillow", "安装 Pillow"),
        ("pip install pytest", "安装 pytest"),
    ]
    
    for command, description in commands:
        if not run_command(command, description):
            return False
    return True


def test_basic_functionality():
    """测试基本功能"""
    print("\n🧪 测试基本功能...")
    
    try:
        # 测试导入
        from md2doc_mcp.core.converter import MarkdownToWordConverter
        from md2doc_mcp.parser.markdown_parser import MarkdownParser
        from md2doc_mcp.parser.table_parser import MarkdownTableParser
        
        print("✅ 模块导入成功")
        
        # 测试 Markdown 解析
        markdown_content = """# 测试标题
## 子标题
这是测试内容。

| 列1 | 列2 |
|-----|-----|
| 值1 | 值2 |"""
        
        parser = MarkdownParser()
        headers = parser.extract_headers(markdown_content)
        tables = parser.extract_tables(markdown_content)
        
        print(f"✅ Markdown 解析成功 - 找到 {len(headers)} 个标题, {len(tables)} 个表格")
        
        # 测试表格解析
        if tables:
            table_data = MarkdownTableParser.parse_table(tables[0])
            print(f"✅ 表格解析成功 - 解析出 {len(table_data)} 行数据")
        
        # 测试转换器
        converter = MarkdownToWordConverter()
        features = converter.get_supported_features()
        print(f"✅ 转换器初始化成功 - 支持 {len(features)} 种功能")
        
        return True
        
    except Exception as e:
        print(f"❌ 基本功能测试失败: {e}")
        return False


def test_conversion():
    """测试转换功能"""
    print("\n🔄 测试 Markdown 转 Word 转换...")
    
    try:
        from md2doc_mcp.core.converter import MarkdownToWordConverter
        
        # 创建测试 Markdown 内容
        test_markdown = """# 测试文档

## 概述
这是一个测试文档，用于验证 md2doc-mcp 的功能。

## 数据表格
| 项目 | 进度 | 状态 |
|------|------|------|
| 任务1 | 80% | 进行中 |
| 任务2 | 100% | 完成 |

## 图表分析
```echarts
{
  title: { text: '项目进度' },
  xAxis: { data: ['任务1', '任务2'] },
  series: [{ 
    name: '进度', 
    data: [80, 100] 
  }]
}
```

## 总结
测试完成！
"""
        
        # 创建临时输出文件
        with tempfile.NamedTemporaryFile(suffix='.docx', delete=False) as tmp_file:
            output_path = tmp_file.name
        
        # 执行转换
        converter = MarkdownToWordConverter()
        converter.convert_markdown_to_word(test_markdown, output_path)
        
        # 检查结果
        if os.path.exists(output_path):
            file_size = os.path.getsize(output_path)
            print(f"✅ 转换成功！输出文件: {output_path}")
            print(f"✅ 文件大小: {file_size} 字节")
            
            # 清理临时文件
            os.remove(output_path)
            return True
        else:
            print("❌ 转换失败 - 未生成输出文件")
            return False
            
    except Exception as e:
        print(f"❌ 转换测试失败: {e}")
        return False


def main():
    """主函数"""
    print("🚀 md2doc-mcp 安装和测试脚本")
    print("=" * 50)
    
    # 检查 Python 版本
    if sys.version_info < (3, 8):
        print("❌ 需要 Python 3.8 或更高版本")
        return False
    
    print(f"✅ Python 版本: {sys.version}")
    
    # 安装依赖
    if not install_dependencies():
        print("❌ 依赖安装失败")
        return False
    
    # 测试基本功能
    if not test_basic_functionality():
        print("❌ 基本功能测试失败")
        return False
    
    # 测试转换功能
    if not test_conversion():
        print("❌ 转换功能测试失败")
        return False
    
    print("\n🎉 所有测试通过！md2doc-mcp 已准备就绪")
    print("\n📖 使用说明:")
    print("1. 在 Claude Desktop 中配置 MCP 服务器")
    print("2. 使用 convert_markdown_text 或 convert_markdown_file 工具")
    print("3. 查看 README.md 获取详细文档")
    
    return True


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)

