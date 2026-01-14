package cn.daydayup.dev.md2doc.core;

import cn.daydayup.dev.md2doc.core.generate.PoiWordGenerator;
import cn.daydayup.dev.md2doc.core.model.WordParam;
import cn.daydayup.dev.md2doc.core.model.WordParams;
import cn.daydayup.dev.md2doc.core.parse.MarkdownTableParser;
import cn.daydayup.dev.md2doc.core.template.DynamicWordDocumentCreator;
import cn.daydayup.dev.md2doc.core.template.EChartsToWordConverter;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger logger = LogManager.getLogger(MarkdownToWordConverter.class);

    // 用于匹配ECharts代码块的正则表达式
    private static final Pattern ECHARTS_PATTERN = Pattern.compile(
            "```echarts\\s*\\n(.*?)\\n```",
            Pattern.DOTALL
    );
    // 用于匹配Mermaid代码块的正则表达式
    private static final Pattern MERMAID_PATTERN = Pattern.compile(
            "```mermaid\\s*\\n(.*?)\\n```",
            Pattern.DOTALL
    );
    // 用于匹配表格的正则表达式
    private static final Pattern TABLE_PATTERN = Pattern.compile("(\\|[^\\n]*\\|\\s*\\n\\s*\\|[-|:\\s]*\\|\\s*\\n(?:\\s*\\|[^\\n]*\\|\\s*\\n?)*)", Pattern.MULTILINE);
    // 用于匹配标题的正则表达式
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$", Pattern.MULTILINE);
    // 用于匹配图片的正则表达式: ![alt](url)
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)", Pattern.MULTILINE);

    /**
     * 将Markdown文件转换为Word文档
     * @param markdownFile Markdown文件路径
     * @param outputFile 输出Word文件路径
     * @throws Exception 转换过程中可能抛出的异常
     */
    public void convertMarkdownFileToWord(String markdownFile, String outputFile) throws Exception {
        String markdownContent = new String(Files.readAllBytes(Paths.get(markdownFile)));
        convertMarkdownToWord(markdownContent, outputFile);
    }

    /**
     * 将Markdown内容转换为Word文档
     * @param markdownContent Markdown内容
     * @param outputFile 输出Word文件路径
     * @throws Exception 转换过程中可能抛出的异常
     */
    public void convertMarkdownToWord(String markdownContent, String outputFile) throws Exception {
        long startTime = System.currentTimeMillis();
        // 创建临时模板文件
        String templateFile = outputFile.replace(".docx", "_template.docx");

        // 使用新的方法创建完整模板，更好地保持Markdown结构
        DynamicWordDocumentCreator.createCompleteTemplateFromMarkdown(templateFile, markdownContent);

        val params = WordParams.create();

        // 处理图片（在 ECharts、Mermaid 和表格之前处理）
        processImages(params, markdownContent);

        // 处理 Mermaid 图表
        processMermaid(params, markdownContent);

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
        long endTime = System.currentTimeMillis();
        logger.info("Markdown文档已成功转换为Word文档: {}，耗时: {}ms", outputFile, (endTime - startTime));
    }


    /**
     * 处理 Mermaid 图表
     * Mermaid 图表将被转换为文本说明（占位符）
     * @param params Word参数对象
     * @param markdownContent Markdown内容
     */
    private void processMermaid(WordParams params, String markdownContent) {
        Matcher matcher = MERMAID_PATTERN.matcher(markdownContent);
        int mermaidIndex = 1;

        while (matcher.find()) {
            String mermaidContent = matcher.group(1).trim();
            String mermaidKey = "mermaid" + mermaidIndex;

            logger.info("处理 Mermaid 图表 [{}]", mermaidIndex);

            // 创建 Mermaid 图表的文本表示
            String mermaidText = "【Mermaid 图表】\n\n" + mermaidContent + "\n\n" +
                    "注意: Mermaid 图表已保留原始代码。如需可视化效果,请访问 https://mermaid.live/ 查看。";

            params.setText(mermaidKey, mermaidText);
            mermaidIndex++;
        }

        if (mermaidIndex > 1) {
            logger.info("共处理 {} 个 Mermaid 图表", mermaidIndex - 1);
        }
    }

    /**
     * 处理图片
     * @param params Word参数对象
     * @param markdownContent Markdown内容
     */
    private void processImages(WordParams params, String markdownContent) {
        Matcher matcher = IMAGE_PATTERN.matcher(markdownContent);
        int imageIndex = 1;

        while (matcher.find()) {
            String altText = matcher.group(1);  // alt 文本
            String imageSource = matcher.group(2);  // 图片 URL 或路径
            String imageKey = "image" + imageIndex;

            logger.info("处理图片 [{}]: {} (alt: {})", imageIndex, imageSource, altText);

            // 使用 WordParam.image(String) 方法，自动处理下载和失败情况
            WordParam imageParam = WordParam.image(imageSource);
            params.setParam(imageKey, imageParam);

            imageIndex++;
        }

        if (imageIndex > 1) {
            logger.info("共处理 {} 张图片", imageIndex - 1);
        }
    }

    /**
     * 处理ECharts图表
     * @param params Word参数对象
     * @param markdownContent Markdown内容
     */
    private void processECharts(WordParams params, String markdownContent) throws Exception {
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
    private void processTables(WordParams params, String markdownContent) {
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
    private void processTextContent(WordParams params, String markdownContent) {
        // 生成文档标题：上个月的年月 + "分析报告"
        java.time.LocalDate now = java.time.LocalDate.now();
        // 计算上个月
        java.time.LocalDate lastMonth = now.minusMonths(1);
        String title = lastMonth.getYear() + "年" + lastMonth.getMonthValue() + "月分析报告";
        params.setText("title", title);

        // 可以添加更多文本处理逻辑
        // 例如提取作者、日期等信息
    }
}