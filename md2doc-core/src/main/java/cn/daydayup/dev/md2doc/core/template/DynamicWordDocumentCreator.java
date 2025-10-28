package cn.daydayup.dev.md2doc.core.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName DynamicWordDocumentCreator
 * @Description 动态生成Word文档模板
 * @Author ZhaoYanNing
 * @Date 2025/8/13 10:44
 * @Version 1.0
 */
public class DynamicWordDocumentCreator {
    
    // 添加标题编号栈和映射
    private static class HeaderNumbering {
        private final Stack<Integer> numberStack = new Stack<>();
        private final Map<Integer, Integer> levelCounters = new HashMap<>();
        
        public void enterLevel(int level) {
            // 重置更深级别的计数器
            for (int i = level + 1; i <= 6; i++) {
                levelCounters.put(i, 0);
            }
            
            // 增加当前级别的计数器
            levelCounters.put(level, levelCounters.getOrDefault(level, 0) + 1);
            
            // 更新栈
            while (numberStack.size() >= level) {
                numberStack.pop();
            }
            numberStack.push(levelCounters.get(level));
        }
        
        public String getNumber() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numberStack.size(); i++) {
                if (i > 0) sb.append(".");
                sb.append(numberStack.get(i));
            }
            return sb.toString();
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
            // 创建标题样式
            createHeaderStyles(document);
            
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
     * 创建自定义标题样式
     * @param document Word文档对象
     */
    private static void createHeaderStyles(XWPFDocument document) {
        XWPFStyles styles = document.createStyles();
        
        // 创建标题1样式
        createHeadingStyle(styles, "Heading1", 1, 22, "000000", "宋体");
        
        // 创建标题2样式
        createHeadingStyle(styles, "Heading2", 2, 20, "000000", "宋体");
        
        // 创建标题3样式
        createHeadingStyle(styles, "Heading3", 3, 18, "000000", "宋体");
        
        // 创建标题4样式
        createHeadingStyle(styles, "Heading4", 4, 16, "000000", "宋体");
        
        // 创建标题5样式
        createHeadingStyle(styles, "Heading5", 5, 14, "000000", "宋体");
        
        // 创建标题6样式
        createHeadingStyle(styles, "Heading6", 6, 12, "000000", "宋体");
    }

    /**
     * 创建标题样式
     * @param styles 样式集合
     * @param styleId 样式ID
     * @param headingLevel 标题级别
     * @param fontSize 字体大小
     * @param color 颜色
     * @param fontName 字体名称
     */
    private static void createHeadingStyle(XWPFStyles styles, String styleId, int headingLevel,
                                           int fontSize, String color, String fontName) {
        // 创建样式
        CTStyle ctStyle = CTStyle.Factory.newInstance();
        ctStyle.setStyleId(styleId);

        CTString styleName = CTString.Factory.newInstance();
        styleName.setVal(styleId);
        ctStyle.setName(styleName);

        // 设置样式类型为段落样式
        ctStyle.setType(STStyleType.PARAGRAPH);

        // 设置样式优先级（数字越小优先级越高）
        CTDecimalNumber priority = CTDecimalNumber.Factory.newInstance();
        priority.setVal(BigInteger.valueOf(headingLevel));
        ctStyle.setUiPriority(priority);

        // 设置样式在格式栏中显示
        CTOnOff quickFormat = CTOnOff.Factory.newInstance();
        ctStyle.setQFormat(quickFormat);

        // 设置样式在使用时不会被隐藏
        CTOnOff unhide = CTOnOff.Factory.newInstance();
        ctStyle.setUnhideWhenUsed(unhide);

        // 样式定义给定级别的标题（这是关键，确保标题出现在导航窗格中）
        CTPPrGeneral ppr = CTPPrGeneral.Factory.newInstance();
        CTDecimalNumber outlineLevel = CTDecimalNumber.Factory.newInstance();
        outlineLevel.setVal(BigInteger.valueOf(headingLevel - 1)); // Word中0级是最高级
        ppr.setOutlineLvl(outlineLevel);
        ctStyle.setPPr(ppr);

        // 设置字体样式
        CTRPr rpr = CTRPr.Factory.newInstance();

        CTHpsMeasure size = CTHpsMeasure.Factory.newInstance();
        size.setVal(new BigInteger(String.valueOf(fontSize * 2))); // 半点为单位

        CTHpsMeasure size2 = CTHpsMeasure.Factory.newInstance();
        size2.setVal(new BigInteger(String.valueOf(fontSize * 2)));

        CTFonts fonts = CTFonts.Factory.newInstance();
        fonts.setAscii(fontName);
        fonts.setEastAsia(fontName);
        fonts.setHAnsi(fontName);

        // 使用POI 5.2.2兼容的方式设置字体
        CTFonts[] fontsArray = new CTFonts[1];
        fontsArray[0] = fonts;
        rpr.setRFontsArray(fontsArray);

        CTHpsMeasure[] szArray = new CTHpsMeasure[1];
        szArray[0] = size;
        rpr.setSzArray(szArray);

        CTHpsMeasure[] szCsArray = new CTHpsMeasure[1];
        szCsArray[0] = size2;
        rpr.setSzCsArray(szCsArray);

        if (color != null && !color.isEmpty()) {
            CTColor ctColor = CTColor.Factory.newInstance();
            ctColor.setVal(color);
            CTColor[] colorArray = new CTColor[1];
            colorArray[0] = ctColor;
            rpr.setColorArray(colorArray);
        }

        ctStyle.setRPr(rpr);

        XWPFStyle style = new XWPFStyle(ctStyle);
        style.setType(STStyleType.PARAGRAPH);
        styles.addStyle(style);
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
     * 解析Markdown内容并创建Word文档结构
     * @param document Word文档对象
     * @param markdownContent Markdown内容
     */
    private static void parseAndCreateDocumentStructure(XWPFDocument document, String markdownContent) {
        // 用于匹配标题的正则表达式
        Pattern headerPattern = Pattern.compile("^(#{1,6})\\s+(.*)$", Pattern.MULTILINE);
        // 用于匹配图片的正则表达式: ![alt](url)
        Pattern imagePattern = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");

        String[] lines = markdownContent.split("\n");
        int chartIndex = 1;
        int mermaidIndex = 1;
        int tableIndex = 1;
        int imageIndex = 1;

        // 初始化标题编号器
        HeaderNumbering headerNumbering = new HeaderNumbering();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // 检查是否为标题
            Matcher headerMatcher = headerPattern.matcher(line);
            if (headerMatcher.find()) {
                int level = headerMatcher.group(1).length();
                String title = headerMatcher.group(2);
                
                // 更新标题编号
                headerNumbering.enterLevel(level);
                String headerNumber = headerNumbering.getNumber();
                
                XWPFParagraph headerParagraph = document.createParagraph();
                setHeaderStyle(headerParagraph, level);
                XWPFRun headerRun = headerParagraph.createRun();
                headerRun.setText(headerNumber + " " + title);
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

            // 检查是否为 Mermaid 图表
            if (line.trim().equals("```mermaid")) {
                // 查找 Mermaid 代码块的结束位置
                StringBuilder mermaidCode = new StringBuilder();
                i++; // 移动到下一行
                while (i < lines.length && !lines[i].trim().equals("```")) {
                    mermaidCode.append(lines[i]).append("\n");
                    i++;
                }

                // 创建 Mermaid 占位符
                XWPFParagraph mermaidParagraph = document.createParagraph();
                setDefaultParagraphStyle(mermaidParagraph);
                XWPFRun mermaidRun = mermaidParagraph.createRun();
                mermaidRun.setText("${mermaid" + mermaidIndex + "}");

                mermaidIndex++;
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
                // 检查行中是否包含图片
                Matcher imageMatcher = imagePattern.matcher(line);
                if (imageMatcher.find()) {
                    // 处理包含图片的行
                    String beforeImage = line.substring(0, imageMatcher.start());
                    String afterImage = line.substring(imageMatcher.end());

                    // 如果图片前有文本，创建段落
                    if (!beforeImage.trim().isEmpty()) {
                        XWPFParagraph paragraph = document.createParagraph();
                        setDefaultParagraphStyle(paragraph);
                        XWPFRun run = paragraph.createRun();
                        run.setText(beforeImage);
                        run.setFontFamily("宋体");
                        run.setFontSize(12);
                    }

                    // 创建图片占位符段落
                    XWPFParagraph imageParagraph = document.createParagraph();
                    imageParagraph.setAlignment(ParagraphAlignment.CENTER); // 图片居中
                    setDefaultParagraphStyle(imageParagraph);
                    XWPFRun imageRun = imageParagraph.createRun();
                    imageRun.setText("${image" + imageIndex + "}");

                    imageIndex++;

                    // 如果图片后有文本，创建段落
                    if (!afterImage.trim().isEmpty()) {
                        XWPFParagraph paragraph = document.createParagraph();
                        setDefaultParagraphStyle(paragraph);
                        XWPFRun run = paragraph.createRun();
                        run.setText(afterImage);
                        run.setFontFamily("宋体");
                        run.setFontSize(12);
                    }
                } else {
                    // 普通文本行
                    XWPFParagraph paragraph = document.createParagraph();
                    setDefaultParagraphStyle(paragraph); // 内容段落使用默认样式
                    XWPFRun run = paragraph.createRun();
                    run.setText(line);
                    run.setFontFamily("宋体");
                    run.setFontSize(12); // 小四号字体
                }
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
     * 在文档中创建一个图表，根据ECharts配置创建相应类型的图表
     * @param document Word文档对象
     * @param chartTitle 图表标题
     * @param echartsConfig ECharts配置
     */
    private static void createChartInDocument(XWPFDocument document, String chartTitle, String echartsConfig) throws IOException, InvalidFormatException {
        // 根据ECharts配置创建相应类型的图表
        XWPFChart chart = createChartBasedOnEChartsConfig(document, chartTitle, echartsConfig);

        // 注意：document.createChart() 已经自动将图表添加到文档中
        // 不需要手动创建段落或run来关联图表
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
            
            // 创建图表对象,并设置图表大小
            XWPFChart chart = document.createChart(15 * Units.EMU_PER_CENTIMETER, 8 * Units.EMU_PER_CENTIMETER);
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

        // 设置显示数据标签
        if (!chart.getCTChart().getPlotArea().getBarChartList().isEmpty()) {
            // 获取柱状图的第一个系列并设置数据标签
            org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart ctBarChart =
                    chart.getCTChart().getPlotArea().getBarChartList().get(0);

            if (!ctBarChart.getSerList().isEmpty()) {
                for (int i = 0; i < ctBarChart.getSerList().size(); i++) {
                    org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer series =
                            ctBarChart.getSerList().get(i);

                    // 添加数据标签设置
                    org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls dLbls = series.addNewDLbls();
                    dLbls.addNewShowVal().setVal(true);     // 显示数值
                    dLbls.addNewShowCatName().setVal(false); // 不显示类别名称
                    dLbls.addNewShowSerName().setVal(false); // 不显示系列名称
                    dLbls.addNewShowPercent().setVal(false); // 不显示百分比
                    dLbls.addNewShowLegendKey().setVal(false); // 不显示图例
                    dLbls.addNewShowBubbleSize().setVal(false); // 不显示气泡大小
                }
            }
        }
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

        // 设置显示数据标签
        if (!chart.getCTChart().getPlotArea().getLineChartList().isEmpty()) {
            // 获取折线图的第一个系列并设置数据标签
            org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart ctLineChart =
                    chart.getCTChart().getPlotArea().getLineChartList().get(0);

            if (!ctLineChart.getSerList().isEmpty()) {
                for (int i = 0; i < ctLineChart.getSerList().size(); i++) {
                    org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer series =
                            ctLineChart.getSerList().get(i);

                    // 添加数据标签设置
                    org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls dLbls = series.addNewDLbls();
                    dLbls.addNewShowVal().setVal(true);     // 显示数值
                    dLbls.addNewShowCatName().setVal(false); // 不显示类别名称
                    dLbls.addNewShowSerName().setVal(false); // 不显示系列名称
                    dLbls.addNewShowPercent().setVal(false); // 不显示百分比
                    dLbls.addNewShowLegendKey().setVal(false); // 不显示图例
                    dLbls.addNewShowBubbleSize().setVal(false); // 不显示气泡大小
                }
            }
        }
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
        CTDLbls ctdLbls = chart.getCTChart().getPlotArea().getPieChartArray(0).getSerArray(0).addNewDLbls();
        ctdLbls.addNewShowVal().setVal(false);
        ctdLbls.addNewShowLegendKey().setVal(false);
        //类别名称
        ctdLbls.addNewShowCatName().setVal(true);
        //百分比
        ctdLbls.addNewShowSerName().setVal(false);
        ctdLbls.addNewShowPercent().setVal(true);
        //引导线
        ctdLbls.addNewShowLeaderLines().setVal(true);
        //分隔符为分行符
        ctdLbls.setSeparator("\n");
    }
}