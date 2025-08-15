package cn.daydayup.dev.md2doc.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xwpf.usermodel.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName DynamicWordDocumentCreator
 * @Description 动态生成Word文档模板
 * @Author ZhaoYanNing
 * @Date 2025/8/13 10:44
 * @Version 1.0
 */
public class DynamicWordDocumentCreator {

    /**
     * 创建一个完整的Word文档模板，包含动态生成的图表
     * @param filePath 输出文件路径
     * @param chartCount 图表数量
     * @param tableCount 表格数量
     * @throws IOException IO异常
     * @throws InvalidFormatException 格式异常
     */
    public static void createCompleteTemplate(String filePath, int chartCount, int tableCount)
            throws IOException, InvalidFormatException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 创建标题段落
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("${title}");
            titleRun.setBold(true);
            titleRun.setFontSize(20);

            // 添加一个空行
            document.createParagraph();

            // 为每个图表添加占位段落和实际图表
            for (int i = 1; i <= chartCount; i++) {
                // 添加图表标题
                XWPFParagraph chartTitleParagraph = document.createParagraph();
                XWPFRun chartTitleRun = chartTitleParagraph.createRun();
                chartTitleRun.setText("图表 " + i + "：");
                chartTitleRun.setBold(true);

                // 创建实际的图表对象
                createChartInDocument(document, "chart" + i);

                // 添加空行
                document.createParagraph();
            }

            // 为每个表格添加占位段落
            for (int i = 1; i <= tableCount; i++) {
                // 添加表格标题
                XWPFParagraph tableTitleParagraph = document.createParagraph();
                XWPFRun tableTitleRun = tableTitleParagraph.createRun();
                tableTitleRun.setText("表格 " + i + "：");
                tableTitleRun.setBold(true);

                // 添加表格占位符
                XWPFParagraph tableParagraph = document.createParagraph();
                XWPFRun tableRun = tableParagraph.createRun();
                tableRun.setText("${table" + i + "}");

                // 添加空行
                document.createParagraph();
            }

            // 保存文档
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
        }
    }
    
    /**
     * 根据Markdown内容创建更完整的模板
     * @param filePath 输出文件路径
     * @param markdownContent Markdown内容
     * @throws IOException IO异常
     * @throws InvalidFormatException 格式异常
     */
    public static void createCompleteTemplateFromMarkdown(String filePath, String markdownContent)
            throws IOException, InvalidFormatException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 创建标题段落
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            // 设置标题段落样式
            setTitleParagraphStyle(titleParagraph);
            
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("${title}");
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setFontFamily("宋体");

            // 添加一个空行
            XWPFParagraph emptyParagraph = document.createParagraph();
            setDefaultParagraphStyle(emptyParagraph);

            // 解析Markdown内容并创建相应的Word结构
            parseAndCreateDocumentStructure(document, markdownContent);

            // 保存文档
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
        }
    }
    
    /**
     * 设置默认段落样式 - 小四号宋体，1.5倍行距，首行缩进2字符
     * @param paragraph 段落对象
     */
    private static void setDefaultParagraphStyle(XWPFParagraph paragraph) {
        // 设置小四字号 (12pt = 24 half-points)
        if (paragraph.getCTP().getPPr() == null) {
            paragraph.getCTP().addNewPPr();
        }
        
        // 设置1.5倍行距
        paragraph.getCTP().getPPr().addNewSpacing().setLineRule(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AUTO);
        paragraph.getCTP().getPPr().getSpacing().setLine(BigInteger.valueOf(360)); // 1.5倍行距
        
        // 设置首行缩进2字符 (约24磅的20分之一 = 24 * 20 = 480)
        paragraph.getCTP().getPPr().addNewInd().setFirstLineChars(BigInteger.valueOf(200)); // 2字符
        paragraph.getCTP().getPPr().getInd().setFirstLine(BigInteger.valueOf(480));
    }
    
    /**
     * 设置标题段落样式
     * @param paragraph 标题段落
     */
    private static void setTitleParagraphStyle(XWPFParagraph paragraph) {
        // 标题不需要首行缩进
        if (paragraph.getCTP().getPPr() == null) {
            paragraph.getCTP().addNewPPr();
        }
        paragraph.getCTP().getPPr().addNewInd();
        
        // 设置行距
        paragraph.getCTP().getPPr().addNewSpacing().setLineRule(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AUTO);
        paragraph.getCTP().getPPr().getSpacing().setLine(BigInteger.valueOf(360));
    }
    
    /**
     * 设置各级标题样式
     * @param paragraph 标题段落
     * @param level 标题级别
     */
    private static void setHeaderParagraphStyle(XWPFParagraph paragraph, int level) {
        // 标题不需要首行缩进
        if (paragraph.getCTP().getPPr() == null) {
            paragraph.getCTP().addNewPPr();
        }
        paragraph.getCTP().getPPr().addNewInd();
        
        // 设置行距
        paragraph.getCTP().getPPr().addNewSpacing().setLineRule(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AUTO);
        paragraph.getCTP().getPPr().getSpacing().setLine(BigInteger.valueOf(360));
        
        // 设置字体大小（根据标题级别）
        int fontSize = 16; // 默认H3
        switch (level) {
            case 1: fontSize = 22; break; // H1
            case 2: fontSize = 20; break; // H2
            case 3: fontSize = 18; break; // H3
            case 4: fontSize = 16; break; // H4
            case 5: fontSize = 14; break; // H5
            case 6: fontSize = 12; break; // H6
        }
    }
    
    /**
     * 解析Markdown内容并创建Word文档结构
     * @param document Word文档对象
     * @param markdownContent Markdown内容
     */
    private static void parseAndCreateDocumentStructure(XWPFDocument document, String markdownContent) {
        // 用于匹配ECharts代码块的正则表达式
        Pattern echartsPattern = Pattern.compile("```echarts\\s*\\n(.*?)\\n```", Pattern.DOTALL);
        // 用于匹配表格的正则表达式
        Pattern tablePattern = Pattern.compile("(\\|[^\\n]*\\|\\s*\\n\\s*\\|[-|\\s]*\\|\\s*\\n(?:\\s*\\|[^\\n]*\\|\\s*\\n?)*)", Pattern.MULTILINE);
        // 用于匹配标题的正则表达式
        Pattern headerPattern = Pattern.compile("^(#{1,6})\\s+(.*)$", Pattern.MULTILINE);
        
        String[] lines = markdownContent.split("\n");
        int chartIndex = 1;
        int tableIndex = 1;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // 检查是否为标题
            Matcher headerMatcher = headerPattern.matcher(line);
            if (headerMatcher.find()) {
                int level = headerMatcher.group(1).length();
                String title = headerMatcher.group(2);
                
                XWPFParagraph headerParagraph = document.createParagraph();
                setHeaderStyle(headerParagraph, level);
                setHeaderParagraphStyle(headerParagraph, level);
                XWPFRun headerRun = headerParagraph.createRun();
                headerRun.setText(title);
                headerRun.setBold(true);
                headerRun.setFontFamily("宋体");
                
                // 根据标题级别设置字体大小
                int fontSize = 16; // 默认H3
                switch (level) {
                    case 1: fontSize = 22; break; // H1
                    case 2: fontSize = 20; break; // H2
                    case 3: fontSize = 18; break; // H3
                    case 4: fontSize = 16; break; // H4
                    case 5: fontSize = 14; break; // H5
                    case 6: fontSize = 12; break; // H6
                }
                headerRun.setFontSize(fontSize);
                
                continue;
            }
            
            // 检查是否为ECharts图表
            if (line.trim().equals("```echarts")) {
                // 查找图表代码块的结束位置
                StringBuilder chartCode = new StringBuilder();
                i++; // 移动到下一行
                while (i < lines.length && !lines[i].trim().equals("```")) {
                    chartCode.append(lines[i]).append("\n");
                    i++;
                }
                
                // 创建图表占位符
                XWPFParagraph chartTitleParagraph = document.createParagraph();
                chartTitleParagraph.setAlignment(ParagraphAlignment.CENTER); // 设置居中对齐
                setDefaultParagraphStyle(chartTitleParagraph); // 图表标题使用默认段落样式
                XWPFRun chartTitleRun = chartTitleParagraph.createRun();
                chartTitleRun.setText("图表 " + chartIndex + "：");
                chartTitleRun.setBold(true);
                chartTitleRun.setFontFamily("宋体");

                // 创建实际的图表对象
                try {
                    createChartInDocument(document, "chart" + chartIndex, chartCode.toString());
                } catch (Exception e) {
                    // 如果创建图表失败，至少添加占位符
                    XWPFParagraph chartParagraph = document.createParagraph();
                    chartParagraph.setAlignment(ParagraphAlignment.CENTER);
                    setDefaultParagraphStyle(chartParagraph);
                    XWPFRun chartRun = chartParagraph.createRun();
                    chartRun.setText("${chart" + chartIndex + "}");
                }
                
                chartIndex++;
                continue;
            }
            
            // 检查是否为表格开始
            if (line.startsWith("|")) {
                // 收集表格的所有行
                StringBuilder tableMarkdown = new StringBuilder(line).append("\n");
                i++; // 移动到下一行
                while (i < lines.length && (lines[i].startsWith("|") || lines[i].trim().matches("^\\|?\\s*[-|:\\s]+\\|?\\s*$"))) {
                    tableMarkdown.append(lines[i]).append("\n");
                    i++;
                }
                i--; // 回退一行，因为循环会自动增加i
                
                // 创建表格占位符
                XWPFParagraph tableTitleParagraph = document.createParagraph();
                tableTitleParagraph.setAlignment(ParagraphAlignment.CENTER); // 设置居中对齐
                setDefaultParagraphStyle(tableTitleParagraph); // 表格标题使用默认段落样式
                XWPFRun tableTitleRun = tableTitleParagraph.createRun();
                tableTitleRun.setText("表格 " + tableIndex + "：");
                tableTitleRun.setBold(true);
                tableTitleRun.setFontFamily("宋体");

                XWPFParagraph tableParagraph = document.createParagraph();
                tableParagraph.setAlignment(ParagraphAlignment.CENTER); // 设置居中对齐
                setDefaultParagraphStyle(tableParagraph);
                XWPFRun tableRun = tableParagraph.createRun();
                tableRun.setText("${table" + tableIndex + "}");
                
                tableIndex++;
                continue;
            }
            
            // 普通段落
            if (!line.trim().isEmpty()) {
                XWPFParagraph paragraph = document.createParagraph();
                setDefaultParagraphStyle(paragraph); // 内容段落使用默认样式
                XWPFRun run = paragraph.createRun();
                run.setText(line);
                run.setFontFamily("宋体");
                run.setFontSize(12); // 小四号字体
            }
        }
    }
    
    /**
     * 设置标题样式
     * @param paragraph 标题段落
     * @param level 标题级别
     */
    private static void setHeaderStyle(XWPFParagraph paragraph, int level) {
        switch (level) {
            case 1:
                paragraph.setStyle("Heading1");
                break;
            case 2:
                paragraph.setStyle("Heading2");
                break;
            case 3:
                paragraph.setStyle("Heading3");
                break;
            case 4:
                paragraph.setStyle("Heading4");
                break;
            case 5:
                paragraph.setStyle("Heading5");
                break;
            case 6:
                paragraph.setStyle("Heading6");
                break;
            default:
                paragraph.setStyle("Heading1");
                break;
        }
    }

    /**
     * 在文档中创建一个图表
     * @param document Word文档对象
     * @param chartTitle 图表标题
     */
    private static void createChartInDocument(XWPFDocument document, String chartTitle) throws IOException, InvalidFormatException {
        // 创建段落来放置图表
        XWPFParagraph chartParagraph = document.createParagraph();
        chartParagraph.setAlignment(ParagraphAlignment.CENTER);

        // 创建一个简单的柱状图
        XWPFChart chart = createSampleChart(document, chartTitle);
    }
    
    /**
     * 在文档中创建一个图表，根据ECharts配置创建相应类型的图表
     * @param document Word文档对象
     * @param chartTitle 图表标题
     * @param echartsConfig ECharts配置
     */
    private static void createChartInDocument(XWPFDocument document, String chartTitle, String echartsConfig) throws IOException, InvalidFormatException {
        // 创建段落来放置图表
        XWPFParagraph chartParagraph = document.createParagraph();
        chartParagraph.setAlignment(ParagraphAlignment.CENTER);

        // 根据ECharts配置创建相应类型的图表
        XWPFChart chart = createChartBasedOnEChartsConfig(document, chartTitle, echartsConfig);
        
        // 设置图表显示尺寸
        chart.getCTChartSpace().getChart();
    }

    /**
     * 创建一个示例图表
     * @param document Word文档对象
     * @param chartTitle 图表标题
     * @return 创建的图表对象
     */
    private static XWPFChart createSampleChart(XWPFDocument document, String chartTitle) throws IOException, InvalidFormatException {
        // 创建图表对象
        XWPFChart chart = document.createChart();
        chart.setTitleText(chartTitle);
        
        // 创建数据
        String[] series = {"系列1"};
        String[] categories = {"类别1", "类别2", "类别3"};
        Double[][] values = {{20.0, 40.0, 30.0}};

        // 创建图表数据
        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        categoryAxis.setTitle("X轴");

        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        valueAxis.setTitle("Y轴");

        XDDFChartData data = chart.createData(ChartTypes.BAR, categoryAxis, valueAxis);
        data.setVaryColors(true);

        // 添加数据系列
        XDDFCategoryDataSource categoryDataSource = XDDFDataSourcesFactory.fromArray(categories);
        XDDFNumericalDataSource<Double> valueDataSource = XDDFDataSourcesFactory.fromArray(values[0]);

        XDDFChartData.Series series1 = data.addSeries(categoryDataSource, valueDataSource);
        series1.setTitle(series[0], null);

        chart.plot(data);

        return chart;
    }
    
    /**
     * 根据ECharts配置创建相应类型的图表
     * @param document Word文档对象
     * @param chartTitle 图表标题
     * @param echartsConfig ECharts配置
     * @return 创建的图表对象
     */
    private static XWPFChart createChartBasedOnEChartsConfig(XWPFDocument document, String chartTitle, String echartsConfig) throws IOException, InvalidFormatException {
        try {
            // 预处理ECharts配置，将其转换为有效的JSON格式
            String jsonConfig = EChartsToWordConverter.convertEChartsToJson(echartsConfig);
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonConfig);
            
            // 获取图表类型，默认为柱状图
            String chartType = "bar";
            JsonNode seriesNode = rootNode.path("series");
            if (seriesNode.isArray() && seriesNode.size() > 0) {
                JsonNode firstSeries = seriesNode.get(0);
                chartType = firstSeries.path("type").asText("bar");
            }
            
            // 创建图表对象
            XWPFChart chart = document.createChart();
            chart.setTitleText(chartTitle);

            // 根据图表类型创建相应的图表
            switch (chartType) {
                case "line":
                    createLineChart(chart, rootNode);
                    break;
                case "pie":
                    createPieChart(chart, rootNode);
                    break;
                default: // 默认创建柱状图
                    createBarChart(chart, rootNode);
                    break;
            }
            
            return chart;
        } catch (Exception e) {
            // 如果解析失败，创建一个默认的柱状图
            return createSampleChart(document, chartTitle);
        }
    }
    
    /**
     * 创建柱状图
     * @param chart 图表对象
     * @param rootNode ECharts配置的根节点
     */
    private static void createBarChart(XWPFChart chart, JsonNode rootNode) {
        // 提取X轴数据
        String[] categories = {};
        JsonNode xAxisNode = rootNode.path("xAxis");
        if (xAxisNode.isArray()) {
            xAxisNode = xAxisNode.get(0);
        }
        
        if (!xAxisNode.isMissingNode()) {
            JsonNode xAxisData = xAxisNode.path("data");
            if (!xAxisData.isMissingNode() && xAxisData.isArray()) {
                categories = new String[xAxisData.size()];
                for (int i = 0; i < xAxisData.size(); i++) {
                    categories[i] = xAxisData.get(i).asText();
                }
            }
        }
        
        // 创建轴
        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        JsonNode xAxisName = xAxisNode.path("name");
        if (!xAxisName.isMissingNode()) {
            categoryAxis.setTitle(xAxisName.asText());
        }
        
        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        JsonNode yAxisNode = rootNode.path("yAxis");
        if (yAxisNode.isArray()) {
            yAxisNode = yAxisNode.get(0);
        }
        
        if (!yAxisNode.isMissingNode()) {
            JsonNode yAxisName = yAxisNode.path("name");
            if (!yAxisName.isMissingNode()) {
                valueAxis.setTitle(yAxisName.asText());
            }
        }
        
        // 创建图表数据
        XDDFChartData data = chart.createData(ChartTypes.BAR, categoryAxis, valueAxis);
        data.setVaryColors(true);
        
        // 添加数据系列
        JsonNode seriesNode = rootNode.path("series");
        if (seriesNode.isArray()) {
            for (int i = 0; i < seriesNode.size(); i++) {
                JsonNode serie = seriesNode.get(i);
                String seriesName = serie.path("name").asText("系列" + (i+1));
                
                JsonNode seriesData = serie.path("data");
                if (!seriesData.isMissingNode() && seriesData.isArray()) {
                    Double[] values = new Double[seriesData.size()];
                    for (int j = 0; j < seriesData.size(); j++) {
                        values[j] = seriesData.get(j).asDouble();
                    }
                    
                    XDDFCategoryDataSource categoryDataSource = XDDFDataSourcesFactory.fromArray(categories);
                    XDDFNumericalDataSource<Double> valueDataSource = XDDFDataSourcesFactory.fromArray(values);
                    
                    XDDFChartData.Series series = data.addSeries(categoryDataSource, valueDataSource);
                    series.setTitle(seriesName, null);
                }
            }
        }
        
        chart.plot(data);
    }
    
    /**
     * 创建折线图
     * @param chart 图表对象
     * @param rootNode ECharts配置的根节点
     */
    private static void createLineChart(XWPFChart chart, JsonNode rootNode) {
        // 提取X轴数据
        String[] categories = {};
        JsonNode xAxisNode = rootNode.path("xAxis");
        if (xAxisNode.isArray()) {
            xAxisNode = xAxisNode.get(0);
        }
        
        if (!xAxisNode.isMissingNode()) {
            JsonNode xAxisData = xAxisNode.path("data");
            if (!xAxisData.isMissingNode() && xAxisData.isArray()) {
                categories = new String[xAxisData.size()];
                for (int i = 0; i < xAxisData.size(); i++) {
                    categories[i] = xAxisData.get(i).asText();
                }
            }
        }
        
        // 创建轴
        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        JsonNode xAxisName = xAxisNode.path("name");
        if (!xAxisName.isMissingNode()) {
            categoryAxis.setTitle(xAxisName.asText());
        }
        
        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        JsonNode yAxisNode = rootNode.path("yAxis");
        if (yAxisNode.isArray()) {
            yAxisNode = yAxisNode.get(0);
        }
        
        if (!yAxisNode.isMissingNode()) {
            JsonNode yAxisName = yAxisNode.path("name");
            if (!yAxisName.isMissingNode()) {
                valueAxis.setTitle(yAxisName.asText());
            }
        }
        
        // 创建图表数据
        XDDFChartData data = chart.createData(ChartTypes.LINE, categoryAxis, valueAxis);
        
        // 添加数据系列
        JsonNode seriesNode = rootNode.path("series");
        if (seriesNode.isArray()) {
            for (int i = 0; i < seriesNode.size(); i++) {
                JsonNode serie = seriesNode.get(i);
                String seriesName = serie.path("name").asText("系列" + (i+1));
                
                JsonNode seriesData = serie.path("data");
                if (!seriesData.isMissingNode() && seriesData.isArray()) {
                    Double[] values = new Double[seriesData.size()];
                    for (int j = 0; j < seriesData.size(); j++) {
                        values[j] = seriesData.get(j).asDouble();
                    }
                    
                    XDDFCategoryDataSource categoryDataSource = XDDFDataSourcesFactory.fromArray(categories);
                    XDDFNumericalDataSource<Double> valueDataSource = XDDFDataSourcesFactory.fromArray(values);
                    
                    XDDFChartData.Series series = data.addSeries(categoryDataSource, valueDataSource);
                    series.setTitle(seriesName, null);
                }
            }
        }
        
        chart.plot(data);
    }
    
    /**
     * 创建饼图
     * @param chart 图表对象
     * @param rootNode ECharts配置的根节点
     */
    private static void createPieChart(XWPFChart chart, JsonNode rootNode) {
        // 创建轴
        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        categoryAxis.setVisible(false);
        
        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        valueAxis.setVisible(false);
        
        // 创建图表数据
        XDDFChartData data = chart.createData(ChartTypes.PIE, categoryAxis, valueAxis);
        data.setVaryColors(true);
        
        // 添加数据系列
        JsonNode seriesNode = rootNode.path("series");
        if (seriesNode.isArray() && seriesNode.size() > 0) {
            JsonNode serie = seriesNode.get(0); // 饼图通常只有一个系列
            String seriesName = serie.path("name").asText("系列1");
            
            JsonNode seriesData = serie.path("data");
            if (!seriesData.isMissingNode() && seriesData.isArray()) {
                String[] categories = new String[seriesData.size()];
                Double[] values = new Double[seriesData.size()];
                
                for (int i = 0; i < seriesData.size(); i++) {
                    JsonNode dataItem = seriesData.get(i);
                    if (dataItem.isObject()) {
                        categories[i] = dataItem.path("name").asText("类别" + (i+1));
                        values[i] = dataItem.path("value").asDouble();
                    } else {
                        categories[i] = "类别" + (i+1);
                        values[i] = dataItem.asDouble();
                    }
                }
                
                XDDFCategoryDataSource categoryDataSource = XDDFDataSourcesFactory.fromArray(categories);
                XDDFNumericalDataSource<Double> valueDataSource = XDDFDataSourcesFactory.fromArray(values);
                
                XDDFChartData.Series series = data.addSeries(categoryDataSource, valueDataSource);
                series.setTitle(seriesName, null);
            }
        }
        
        chart.plot(data);
    }
}