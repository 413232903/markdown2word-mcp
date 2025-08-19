package cn.daydayup.dev.md2doc.core.template;

import cn.daydayup.dev.md2doc.core.model.ChartTable;
import cn.daydayup.dev.md2doc.core.model.WordParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName EChartsToWordConverter
 * @Description ECharts图表转换为Word图表的工具类
 * @Author ZhaoYanNing
 * @Date 2025/8/12 17:12
 * @Version 1.0
 */
public class EChartsToWordConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将 ECharts 配置转换为 Word 图表
     *
     * @param params        Word 参数对象
     * @param chartKey      图表键名
     * @param echartsConfig ECharts 配置 JSON 字符串
     * @throws IOException JSON 解析异常
     */
    public static void convertEChartsToWordChart(WordParams params, String chartKey, String echartsConfig) throws IOException {
        try {
            // 预处理ECharts配置，将其转换为有效的JSON格式
            String jsonConfig = convertEChartsToJson(echartsConfig);

            JsonNode rootNode = objectMapper.readTree(jsonConfig);

            // 获取图表标题
            String title = rootNode.path("title").path("text").asText("默认标题");

            // 创建图表
            ChartTable chartTable = params.addChart(chartKey).setTitle(title);

            // 处理 X 轴数据
            JsonNode xAxisNode = rootNode.path("xAxis");
            if (xAxisNode.isArray()) {
                xAxisNode = xAxisNode.get(0); // 多个 x 轴时取第一个
            }

            if (!xAxisNode.isMissingNode()) {
                JsonNode xAxisData = xAxisNode.path("data");
                if (!xAxisData.isMissingNode()) {
                    List<String> xAxisLabels = new ArrayList<>();
                    for (JsonNode dataNode : xAxisData) {
                        xAxisLabels.add(dataNode.asText());
                    }
                    chartTable.getXAxis().addAllData(xAxisLabels);
                }
            }

            // 处理 Y 轴数据和系列数据
            JsonNode seriesNode = rootNode.path("series");
            if (seriesNode.isArray()) {
                for (JsonNode serie : seriesNode) {
                    String seriesName = serie.path("name").asText("数据系列");
                    JsonNode seriesData = serie.path("data");

                    if (!seriesData.isMissingNode() && seriesData.isArray()) {
                        List<Number> dataValues = new ArrayList<>();
                        for (JsonNode dataNode : seriesData) {
                            if (dataNode.isNumber()) {
                                dataValues.add(dataNode.numberValue());
                            } else {
                                dataValues.add(0);
                            }
                        }
                        chartTable.newYAxis(seriesName).addAllData(dataValues);
                    }
                }
            }

            // 如果有 Y 轴名称设置，更新第一个 Y 轴的标题
            JsonNode yAxisNode = rootNode.path("yAxis");
            if (yAxisNode.isArray()) {
                yAxisNode = yAxisNode.get(0); // 多个 y 轴时取第一个
            }

            if (!yAxisNode.isMissingNode()) {
                String yAxisName = yAxisNode.path("name").asText("");
                if (!yAxisName.isEmpty() && !chartTable.getYAxis().isEmpty()) {
                    // 获取第一个 Y 轴并设置标题
                    String firstKey = chartTable.getYAxis().keySet().iterator().next();
                    chartTable.getYAxis(firstKey).setTitle(yAxisName);
                }
            }
        } catch (Exception e) {
            // 如果解析失败，创建一个默认的空图表
            ChartTable chartTable = params.addChart(chartKey).setTitle("默认图表标题");
            chartTable.getXAxis().addAllData("数据1", "数据2", "数据3");
            chartTable.newYAxis("默认系列").addAllData(10, 20, 30);
            throw new IOException("解析ECharts配置时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 将ECharts配置转换为有效的JSON格式
     * @param echartsConfig ECharts配置字符串
     * @return 有效的JSON字符串
     */
    public static String convertEChartsToJson(String echartsConfig) {  // 改为public方法
        String json = echartsConfig;

        // 处理键名，给没有引号的键添加引号
        // 匹配键名（以字母、下划线或$开头，后跟字母、数字、下划线或$）
        json = json.replaceAll("([{,]\\s*)([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*:", "$1\"$2\":");

        // 处理单引号为双引号
        json = json.replaceAll("'", "\"");

        // 处理末尾的逗号（在}或]之前）
        json = json.replaceAll(",\\s*([}\\]])", "$1");

        return json;
    }

    /**
     * 简化版本：直接根据数据创建柱状图
     *
     * @param params       Word 参数对象
     * @param chartKey     图表键名
     * @param title        图表标题
     * @param xAxisLabels  X 轴标签
     * @param seriesName   系列名称
     * @param seriesData   系列数据
     */
    public static void createBarChart(WordParams params, String chartKey, String title,
                                      List<String> xAxisLabels, String seriesName, List<Number> seriesData) {
        ChartTable chartTable = params.addChart(chartKey).setTitle(title);
        chartTable.getXAxis().addAllData(xAxisLabels);
        chartTable.newYAxis(seriesName).addAllData(seriesData);
    }
}