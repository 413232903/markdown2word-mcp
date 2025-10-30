package cn.daydayup.dev.md2doc.service.mcp;

import cn.daydayup.dev.md2doc.service.service.MarkdownConversionService;
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
 * 使用 Spring Bean + @Description 方式注册 MCP 工具
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
    @Description("将 Markdown 文本内容转换为 Word 文档。支持标题、段落、表格、ECharts 图表、图片等元素。" +
                 "返回 Base64 编码的 Word 文档内容")
    public Function<ConvertTextRequest, Map<String, Object>> convertMarkdownText() {
        return request -> {
            Map<String, Object> result = new HashMap<>();

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
                markdownConversionService.convertMarkdownToWord(request.markdownContent(), outputPath.toString());

                // 读取生成的文件并转为 Base64
                byte[] fileContent = Files.readAllBytes(outputPath);
                String base64Content = Base64.getEncoder().encodeToString(fileContent);

                // 构造响应
                result.put("success", true);
                result.put("message", "转换成功");
                result.put("fileName", fileName + ".docx");
                result.put("base64Content", base64Content);
                result.put("fileSize", fileContent.length);

                return result;

            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "转换失败: " + e.getMessage());
                result.put("error", e.getClass().getName());
                return result;
            }
        };
    }

    /**
     * 将 Markdown 文件转换为 Word 文档的工具
     */
    @Bean
    @Description("将指定路径的 Markdown 文件转换为 Word 文档。文件路径必须是绝对路径。" +
                 "返回 Base64 编码的 Word 文档内容")
    public Function<ConvertFileRequest, Map<String, Object>> convertMarkdownFile() {
        return request -> {
            Map<String, Object> result = new HashMap<>();

            try {
                // 验证文件是否存在
                Path inputPath = Paths.get(request.markdownFilePath());
                if (!Files.exists(inputPath)) {
                    result.put("success", false);
                    result.put("message", "文件不存在: " + request.markdownFilePath());
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
                markdownConversionService.convertMarkdownFileToWord(request.markdownFilePath(), outputPath.toString());

                // 读取生成的文件并转为 Base64
                byte[] fileContent = Files.readAllBytes(outputPath);
                String base64Content = Base64.getEncoder().encodeToString(fileContent);

                // 构造响应
                result.put("success", true);
                result.put("message", "转换成功");
                result.put("fileName", fileName + ".docx");
                result.put("base64Content", base64Content);
                result.put("fileSize", fileContent.length);

                return result;

            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "转换失败: " + e.getMessage());
                result.put("error", e.getClass().getName());
                return result;
            }
        };
    }

    /**
     * 获取支持的 Markdown 特性列表的工具
     */
    @Bean
    @Description("获取 md2doc 支持的 Markdown 特性列表、使用说明和示例")
    public Function<Map<String, Object>, Map<String, Object>> getSupportedFeatures() {
        return input -> {
            Map<String, Object> result = new HashMap<>();
            result.put("features", new String[]{
                "1. 六级标题 (H1-H6) - 自动添加序号和格式化",
                "2. 段落文本 - 普通文本段落",
                "3. Markdown 表格 - 标准表格语法",
                "4. ECharts 图表 - 使用 ```echarts 代码块,支持柱状图、折线图、饼图等",
                "5. 图片 - 支持 HTTP/HTTPS URL 和本地文件路径,自适应页面宽度",
                "6. 标题自动编号 - 如 1.1、1.2、2.1 等",
                "7. 目录生成支持 - 可在 Word 中自动生成目录"
            });
            result.put("chartExample",
                "```echarts\n" +
                "{\n" +
                "  \"title\": {\"text\": \"示例图表\"},\n" +
                "  \"xAxis\": {\"data\": [\"A\", \"B\", \"C\"]},\n" +
                "  \"yAxis\": {},\n" +
                "  \"series\": [{\"type\": \"bar\", \"data\": [10, 20, 30]}]\n" +
                "}\n" +
                "```"
            );
            result.put("tableExample", "| 列1 | 列2 |\n|-----|-----|\n| 值1 | 值2 |");
            result.put("imageExample", "![图片描述](https://example.com/image.png)");
            return result;
        };
    }

    /**
     * Markdown 文本转换请求
     */
    public record ConvertTextRequest(
        @Description("要转换的 Markdown 文本内容")
        String markdownContent
    ) {}

    /**
     * Markdown 文件转换请求
     */
    public record ConvertFileRequest(
        @Description("Markdown 文件的完整路径(绝对路径)")
        String markdownFilePath
    ) {}
}
