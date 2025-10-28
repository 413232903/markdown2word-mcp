"""
MCP 服务器主入口
实现符合 Anthropic MCP 协议规范的服务器
"""

import os
import tempfile
import uuid
from typing import Dict, Any
from mcp.server import Server
from mcp.server.models import InitializationOptions, ServerCapabilities
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent
from .core.converter import MarkdownToWordConverter


# 创建 MCP 服务器实例
server = Server("md2doc-mcp")

# 创建转换器实例
converter = MarkdownToWordConverter()


@server.list_tools()
async def handle_list_tools() -> list[Tool]:
    """列出可用的工具"""
    return [
        Tool(
            name="convert_markdown_text",
            description="将 Markdown 文本转换为 Word 文档",
            inputSchema={
                "type": "object",
                "properties": {
                    "content": {
                        "type": "string",
                        "description": "Markdown 文本内容"
                    }
                },
                "required": ["content"]
            }
        ),
        Tool(
            name="convert_markdown_file",
            description="将 Markdown 文件转换为 Word 文档",
            inputSchema={
                "type": "object",
                "properties": {
                    "file_path": {
                        "type": "string",
                        "description": "Markdown 文件路径"
                    }
                },
                "required": ["file_path"]
            }
        ),
        Tool(
            name="get_supported_features",
            description="获取支持的功能列表",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        )
    ]


@server.call_tool()
async def handle_call_tool(name: str, arguments: Dict[str, Any]) -> list[TextContent]:
    """处理工具调用"""
    
    if name == "convert_markdown_text":
        return await convert_markdown_text(arguments.get("content", ""))
    
    elif name == "convert_markdown_file":
        return await convert_markdown_file(arguments.get("file_path", ""))
    
    elif name == "get_supported_features":
        return await get_supported_features()
    
    else:
        raise ValueError(f"未知的工具: {name}")


async def convert_markdown_text(content: str) -> list[TextContent]:
    """将 Markdown 文本转换为 Word 文档
    
    Args:
        content: Markdown 文本内容
        
    Returns:
        包含转换结果的文本内容列表
    """
    try:
        # 验证输入
        if not content or not content.strip():
            return [TextContent(
                type="text",
                text="错误：Markdown 内容不能为空"
            )]
        
        # 验证Markdown内容
        if not converter.validate_markdown_content(content):
            return [TextContent(
                type="text", 
                text="错误：无效的 Markdown 内容"
            )]
        
        # 生成输出文件路径
        output_filename = f"markdown_output_{uuid.uuid4().hex[:8]}.docx"
        output_path = os.path.join(tempfile.gettempdir(), output_filename)
        
        # 执行转换
        converter.convert_markdown_to_word(content, output_path)
        
        # 检查文件是否生成成功
        if os.path.exists(output_path):
            file_size = os.path.getsize(output_path)
            return [TextContent(
                type="text",
                text=f"转换成功！\n\n输出文件：{output_path}\n文件大小：{file_size} 字节\n\n您可以在临时目录中找到生成的 Word 文档。"
            )]
        else:
            return [TextContent(
                type="text",
                text="错误：转换失败，未生成输出文件"
            )]
            
    except Exception as e:
        return [TextContent(
            type="text",
            text=f"转换过程中发生错误：{str(e)}"
        )]


async def convert_markdown_file(file_path: str) -> list[TextContent]:
    """将 Markdown 文件转换为 Word 文档
    
    Args:
        file_path: Markdown 文件路径
        
    Returns:
        包含转换结果的文本内容列表
    """
    try:
        # 验证输入
        if not file_path or not file_path.strip():
            return [TextContent(
                type="text",
                text="错误：文件路径不能为空"
            )]
        
        # 检查文件是否存在
        if not os.path.exists(file_path):
            return [TextContent(
                type="text",
                text=f"错误：文件不存在 - {file_path}"
            )]
        
        # 检查文件扩展名
        if not file_path.lower().endswith(('.md', '.markdown')):
            return [TextContent(
                type="text",
                text="错误：文件必须是 Markdown 格式（.md 或 .markdown）"
            )]
        
        # 生成输出文件路径
        base_name = os.path.splitext(os.path.basename(file_path))[0]
        output_filename = f"{base_name}_converted_{uuid.uuid4().hex[:8]}.docx"
        output_path = os.path.join(tempfile.gettempdir(), output_filename)
        
        # 执行转换
        converter.convert_markdown_file_to_word(file_path, output_path)
        
        # 检查文件是否生成成功
        if os.path.exists(output_path):
            file_size = os.path.getsize(output_path)
            return [TextContent(
                type="text",
                text=f"转换成功！\n\n输入文件：{file_path}\n输出文件：{output_path}\n文件大小：{file_size} 字节\n\n您可以在临时目录中找到生成的 Word 文档。"
            )]
        else:
            return [TextContent(
                type="text",
                text="错误：转换失败，未生成输出文件"
            )]
            
    except Exception as e:
        return [TextContent(
            type="text",
            text=f"转换过程中发生错误：{str(e)}"
        )]


async def get_supported_features() -> list[TextContent]:
    """获取支持的功能列表
    
    Returns:
        包含功能列表的文本内容
    """
    try:
        features = converter.get_supported_features()
        features_text = "md2doc-mcp 支持的功能：\n\n" + "\n".join(f"• {feature}" for feature in features)
        
        return [TextContent(
            type="text",
            text=features_text
        )]
        
    except Exception as e:
        return [TextContent(
            type="text",
            text=f"获取功能列表时发生错误：{str(e)}"
        )]


async def main():
    """主函数"""
    # 使用 stdio 传输运行服务器
    async with stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="md2doc-mcp",
                server_version="1.0.0",
                capabilities=ServerCapabilities(
                    tools={}
                )
            )
        )


if __name__ == "__main__":
    import asyncio
    asyncio.run(main())

