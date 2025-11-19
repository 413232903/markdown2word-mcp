package cn.daydayup.dev.md2doc.service.mcp;

import cn.daydayup.dev.md2doc.service.service.MarkdownConversionService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * MCP Tools 配置 - 提供 Markdown 转 Word 的工具供 AI 模型调用
 */
@Configuration
public class Md2docMcpTools {

    @Autowired
    private MarkdownConversionService markdownConversionService;

    @Value("${md2doc.download-base-url:http://localhost:8080}")
    private String downloadBaseUrl;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/md2doc/";

    /**
     * 将 Markdown 文本内容转换为 Word 文档的工具
     */
    @Bean
    public ToolCallback convertMarkdownText() {
        BiFunction<ConvertTextRequest, ToolContext, String> function = (request, toolContext) -> {
            try {
                // 创建临时目录
                Path tempDir = Paths.get(TEMP_DIR);
                if (!Files.exists(tempDir)) {
                    Files.createDirectories(tempDir);
                }

                // 生成唯一文件名
                String fileName = UUID.randomUUID().toString();
                Path outputPath = tempDir.resolve(fileName + ".docx");

                // 执行转换
                markdownConversionService.convertMarkdownToWord(request.markdownContent, outputPath.toString());

                // 检查文件是否生成成功
                if (!Files.exists(outputPath)) {
                    return "错误：转换失败，未生成输出文件";
                }

                // 构造文件访问URL并直接返回
                String fileUrl = buildDownloadUrl(fileName + ".docx");
                return fileUrl;

            } catch (Exception e) {
                return "错误：转换失败 - " + e.getMessage();
            }
        };

        return FunctionToolCallback.builder("convertMarkdownText", function)
            .description("将 Markdown 文本内容转换为 Word 文档。支持标题、段落、表格、ECharts 图表、图片等元素。返回可下载的 Word 文档链接")
            .inputType(ConvertTextRequest.class)
            .build();
    }

    /**
     * 将 Markdown 文件转换为 Word 文档的工具
     */
    @Bean
    public ToolCallback convertMarkdownFile() {
        BiFunction<ConvertFileRequest, ToolContext, String> function = (request, toolContext) -> {
            try {
                // 验证文件是否存在
                Path inputPath = Paths.get(request.markdownFilePath);
                if (!Files.exists(inputPath)) {
                    return "错误：文件不存在 - " + request.markdownFilePath;
                }

                // 创建临时目录
                Path tempDir = Paths.get(TEMP_DIR);
                if (!Files.exists(tempDir)) {
                    Files.createDirectories(tempDir);
                }

                // 生成唯一文件名
                String fileName = UUID.randomUUID().toString();
                Path outputPath = tempDir.resolve(fileName + ".docx");

                // 执行转换
                markdownConversionService.convertMarkdownFileToWord(request.markdownFilePath, outputPath.toString());

                // 检查文件是否生成成功
                if (!Files.exists(outputPath)) {
                    return "错误：转换失败，未生成输出文件";
                }

                // 构造文件访问URL并直接返回
                String fileUrl = buildDownloadUrl(fileName + ".docx");
                return fileUrl;

            } catch (Exception e) {
                return "错误：转换失败 - " + e.getMessage();
            }
        };

        return FunctionToolCallback.builder("convertMarkdownFile", function)
            .description("将指定路径的 Markdown 文件转换为 Word 文档。文件路径必须是绝对路径。返回可下载的 Word 文档链接")
            .inputType(ConvertFileRequest.class)
            .build();
    }

    /**
     * 获取支持的 Markdown 特性列表的工具
     */
    @Bean
    public ToolCallback getSupportedFeatures() {
        BiFunction<GetFeaturesRequest, ToolContext, FeaturesResponse> function = (input, toolContext) -> {
            FeaturesResponse result = new FeaturesResponse();
            result.features = new String[]{
                "1. 六级标题 (H1-H6) - 自动添加序号和格式化",
                "2. 段落文本 - 普通文本段落",
                "3. Markdown 表格 - 标准表格语法",
                "4. ECharts 图表 - 使用 ```echarts 代码块,支持柱状图、折线图、饼图等",
                "5. 图片 - 支持 HTTP/HTTPS URL 和本地文件路径,自适应页面宽度",
                "6. 标题自动编号 - 如 1.1、1.2、2.1 等",
                "7. 目录生成支持 - 可在 Word 中自动生成目录"
            };
            result.chartExample =
                "```echarts\n" +
                "{\n" +
                "  \"title\": {\"text\": \"示例图表\"},\n" +
                "  \"xAxis\": {\"data\": [\"A\", \"B\", \"C\"]},\n" +
                "  \"yAxis\": {},\n" +
                "  \"series\": [{\"type\": \"bar\", \"data\": [10, 20, 30]}]\n" +
                "}\n" +
                "```";
            result.tableExample = "| 列1 | 列2 |\n|-----|-----|\n| 值1 | 值2 |";
            result.imageExample = "![图片描述](https://example.com/image.png)";
            return result;
        };

        return FunctionToolCallback.builder("getSupportedFeatures", function)
            .description("获取 md2doc 支持的 Markdown 特性列表、使用说明和示例")
            .inputType(GetFeaturesRequest.class)
            .build();
    }

    /**
     * Markdown 文本转换请求
     */
    @JsonClassDescription("Markdown 文本转换请求")
    public static class ConvertTextRequest {
        @JsonProperty(required = true)
        @JsonPropertyDescription("要转换的 Markdown 文本内容")
        public String markdownContent;
    }

    /**
     * Markdown 文件转换请求
     */
    @JsonClassDescription("Markdown 文件转换请求")
    public static class ConvertFileRequest {
        @JsonProperty(required = true)
        @JsonPropertyDescription("Markdown 文件的完整路径(绝对路径)")
        public String markdownFilePath;
    }

    /**
     * 获取特性请求
     */
    @JsonClassDescription("获取支持特性的请求")
    public static class GetFeaturesRequest {
        // 空请求对象
    }

    /**
     * 特性响应
     */
    public static class FeaturesResponse {
        public String[] features;
        public String chartExample;
        public String tableExample;
        public String imageExample;
    }

    /**
     * 构建文件下载URL
     */
    private String buildDownloadUrl(String fileName) {
        String normalizedBaseUrl = downloadBaseUrl.endsWith("/")
            ? downloadBaseUrl.substring(0, downloadBaseUrl.length() - 1)
            : downloadBaseUrl;
        return normalizedBaseUrl + "/api/markdown/files/" + fileName;
    }
}
