package cn.daydayup.dev.md2doc.service.mcp;

import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 md2doc MCP 的转换结果格式化为包含 files 字段的标准响应，
 * 便于 Dify 等平台直接识别并渲染下载链接。
 */
public class Md2docToolResultConverter implements ToolCallResultConverter {

    private static final String WORD_MIME_TYPE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Override
    public String convert(Object result, Type toolReturnType) {
        Map<String, Object> payload = new LinkedHashMap<>();

        if (result instanceof Md2docMcpTools.ConvertResponse convertResponse) {
            payload.put("success", convertResponse.success);
            payload.put("text", buildTextMessage(convertResponse));
            payload.put("files", buildFiles(convertResponse));
            payload.put("error", convertResponse.error);
        } else {
            // 回退到默认字段，确保不会丢失原始信息
            payload.put("success", true);
            payload.put("text", result != null ? result.toString() : "");
            payload.put("files", List.of());
        }

        return JsonParser.toJson(payload);
    }

    private String buildTextMessage(Md2docMcpTools.ConvertResponse response) {
        if (StringUtils.hasText(response.message)) {
            return response.message;
        }
        return response.success ? "转换成功" : "转换失败";
    }

    private List<Map<String, Object>> buildFiles(Md2docMcpTools.ConvertResponse response) {
        if (!response.success || !StringUtils.hasText(response.downloadUrl)) {
            return List.of();
        }

        Map<String, Object> fileEntry = new LinkedHashMap<>();
        fileEntry.put("name", response.fileName);
        fileEntry.put("url", response.downloadUrl);
        fileEntry.put("size", response.fileSize);
        fileEntry.put("mimeType", WORD_MIME_TYPE);

        List<Map<String, Object>> files = new ArrayList<>();
        files.add(fileEntry);
        return files;
    }
}

