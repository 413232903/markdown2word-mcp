package cn.daydayup.dev.md2doc.service.mcp;

import org.springframework.ai.tool.execution.ToolCallResultConverter;

import java.lang.reflect.Type;

/**
 * MCP 工具结果转换器
 * 
 * 当前版本：工具直接返回 String URL，转换器直接返回原始字符串，不进行任何包装。
 * 这样可以确保 MCP 返回的内容是纯字符串类型的 URL。
 */
public class Md2docToolResultConverter implements ToolCallResultConverter {

    @Override
    public String convert(Object result, Type toolReturnType) {
        // 如果结果是 String 类型（URL），直接返回原始字符串，不进行任何转换
        if (result instanceof String) {
            return (String) result;
        }
        
        // 对于其他类型，转换为字符串
        return result != null ? result.toString() : "";
    }

}

