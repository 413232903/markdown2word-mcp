package cn.daydayup.dev.md2doc.service.mcp;

import cn.daydayup.dev.md2doc.service.service.MarkdownConversionService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * MCP Tools 配置 - 提供 Markdown 转 Word 的工具供 AI 模型调用
 */
@Configuration
public class Md2docMcpTools {

    @Autowired
    private MarkdownConversionService markdownConversionService;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/md2doc/";

    /**
     * 将 Markdown 文本内容转换为 Word 文档的工具
     */
    @Bean
    @Description("将 Markdown 文本内容转换为 Word 文档。支持标题、段落、表格、ECharts 图表、图片等元素。返回 Base64 编码的 Word 文档内容")
    public Function<ConvertTextRequest, ConvertResponse> convertMarkdownText() {
        return request -> {
            ConvertResponse result = new ConvertResponse();

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

                // 读取生成的文件并转为 Base64
                byte[] fileContent = Files.readAllBytes(outputPath);
                String base64Content = Base64.getEncoder().encodeToString(fileContent);

                // 构造响应
                result.success = true;
                result.message = "转换成功";
                result.fileName = fileName + ".docx";
                result.base64Content = base64Content;
                result.fileSize = fileContent.length;

                return result;

            } catch (Exception e) {
                result.success = false;
                result.message = "转换失败: " + e.getMessage();
                result.error = e.getClass().getName();
                return result;
            }
        };
    }

    /**
     * 将 Markdown 文件转换为 Word 文档的工具
     */
    @Bean
    @Description("将指定路径的 Markdown 文件转换为 Word 文档。文件路径必须是绝对路径。返回 Base64 编码的 Word 文档内容")
    public Function<ConvertFileRequest, ConvertResponse> convertMarkdownFile() {
        return request -> {
            ConvertResponse result = new ConvertResponse();

            try {
                // 验证文件是否存在
                Path inputPath = Paths.get(request.markdownFilePath);
                if (!Files.exists(inputPath)) {
                    result.success = false;
                    result.message = "文件不存在: " + request.markdownFilePath;
                    return result;
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

                // 读取生成的文件并转为 Base64
                byte[] fileContent = Files.readAllBytes(outputPath);
                String base64Content = Base64.getEncoder().encodeToString(fileContent);

                // 构造响应
                result.success = true;
                result.message = "转换成功";
                result.fileName = fileName + ".docx";
                result.base64Content = base64Content;
                result.fileSize = fileContent.length;

                return result;

            } catch (Exception e) {
                result.success = false;
                result.message = "转换失败: " + e.getMessage();
                result.error = e.getClass().getName();
                return result;
            }
        };
    }

    /**
     * 获取支持的 Markdown 特性列表的工具
     */
    @Bean
    @Description("获取 md2doc 支持的 Markdown 特性列表、使用说明和示例")
    public Function<GetFeaturesRequest, FeaturesResponse> getSupportedFeatures() {
        return input -> {
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
     * 转换响应
     */
    public static class ConvertResponse {
        public boolean success;
        public String message;
        public String fileName;
        public String base64Content;
        public long fileSize;
        public String error;
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
}
