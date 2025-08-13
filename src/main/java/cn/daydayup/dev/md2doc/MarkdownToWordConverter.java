package cn.daydayup.dev.md2doc;

import cn.daydayup.dev.md2doc.generate.PoiWordGenerator;
import cn.daydayup.dev.md2doc.model.WordParam;
import cn.daydayup.dev.md2doc.model.WordParams;
import cn.daydayup.dev.md2doc.parse.MarkdownTableParser;
import cn.daydayup.dev.md2doc.template.DynamicWordDocumentCreator;
import cn.daydayup.dev.md2doc.template.EChartsToWordConverter;
import lombok.val;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName FullMarkdownToWordConverter
 * @Description 完整的Markdown到Word转换器，支持文字、表格和ECharts图表的转换
 * @Author ZhaoYanNing
 * @Date 2025/8/13 9:39
 * @Version 1.0
 */
public class MarkdownToWordConverter {

    // 用于匹配ECharts代码块的正则表达式
    private static final Pattern ECHARTS_PATTERN = Pattern.compile(
            "```echarts\\s*\\n(.*?)\\n```",
            Pattern.DOTALL
    );
    // 用于匹配表格的正则表达式
    private static final Pattern TABLE_PATTERN = Pattern.compile("(\\|[^\\n]*\\|\\s*\\n\\s*\\|[-|:\\s]*\\|\\s*\\n(?:\\s*\\|[^\\n]*\\|\\s*\\n?)*)", Pattern.MULTILINE);
    // 用于匹配标题的正则表达式
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$", Pattern.MULTILINE);
    // 用于匹配段落的正则表达式
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("^(?!#|\\|).*?$", Pattern.MULTILINE);

    /**
     * 将Markdown文件转换为Word文档
     * @param markdownFile Markdown文件路径
     * @param outputFile 输出Word文件路径
     * @throws Exception 转换过程中可能抛出的异常
     */
    public static void convertMarkdownFileToWord(String markdownFile, String outputFile) throws Exception {
        String markdownContent = new String(Files.readAllBytes(Paths.get(markdownFile)));
        convertMarkdownToWord(markdownContent, outputFile);
    }

    /**
     * 将Markdown内容转换为Word文档
     * @param markdownContent Markdown内容
     * @param outputFile 输出Word文件路径
     * @throws Exception 转换过程中可能抛出的异常
     */
    public static void convertMarkdownToWord(String markdownContent, String outputFile) throws Exception {
        // 创建临时模板文件
        String templateFile = outputFile.replace(".docx", "_template.docx");

        // 使用新的方法创建完整模板，更好地保持Markdown结构
        DynamicWordDocumentCreator.createCompleteTemplateFromMarkdown(templateFile, markdownContent);

        val params = WordParams.create();

        // 处理ECharts图表
        processECharts(params, markdownContent);

        // 处理表格
        processTables(params, markdownContent);

        // 处理文本内容
        processTextContent(params, markdownContent);

        // 生成Word文档
        PoiWordGenerator.buildDoc(
                params,
                new File(templateFile),
                new File(outputFile)
        );

        // 删除临时模板文件
        new File(templateFile).delete();
    }


    /**
     * 处理ECharts图表
     * @param params Word参数对象
     * @param markdownContent Markdown内容
     */
    private static void processECharts(WordParams params, String markdownContent) throws Exception {
        Matcher matcher = ECHARTS_PATTERN.matcher(markdownContent);
        int chartIndex = 1;

        while (matcher.find()) {
            String echartsConfig = matcher.group(1);
            String chartKey = "chart" + chartIndex;

            // 使用现有的ECharts转换功能
            EChartsToWordConverter.convertEChartsToWordChart(params, chartKey, echartsConfig);
            chartIndex++;
        }
    }

    /**
     * 处理表格
     * @param params Word参数对象
     * @param markdownContent Markdown内容
     */
    private static void processTables(WordParams params, String markdownContent) {
        Matcher matcher = TABLE_PATTERN.matcher(markdownContent);
        int tableIndex = 1;

        while (matcher.find()) {
            String tableMarkdown = matcher.group(1);
            List<List<String>> tableData = MarkdownTableParser.parseTable(tableMarkdown);
            String tableKey = "table" + tableIndex;

            params.setParam(tableKey, WordParam.table(tableData));
            tableIndex++;
        }
    }

    /**
     * 处理文本内容
     * @param params Word参数对象
     * @param markdownContent Markdown内容
     */
    private static void processTextContent(WordParams params, String markdownContent) {
        // 提取标题作为文档标题
        Matcher headerMatcher = HEADER_PATTERN.matcher(markdownContent);
        if (headerMatcher.find()) {
            params.setText("title", headerMatcher.group(2));
        } else {
            params.setText("title", "默认标题");
        }

        // 可以添加更多文本处理逻辑
        // 例如提取作者、日期等信息
    }
}