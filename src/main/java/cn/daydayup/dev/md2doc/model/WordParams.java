package cn.daydayup.dev.md2doc.model;

import lombok.val;

import java.util.HashMap;

/**
 * @ClassName WordParams
 * @Description word参数
 * @Author ZhaoYanNing
 * @Date 2025/8/12 16:49
 * @Version 1.0
 */
public class WordParams {
    private final HashMap<String, WordParam> params = new HashMap<>();
    private final HashMap<String, ChartTable> chartMap = new HashMap<>();

    public void setParam(String key, WordParam value) {
        params.put(key, value);
    }

    public void setText(String key, Object value) {
        setParam(key, WordParam.text(value));
    }

    public void setChart(String key, ChartTable chartTable) {
        chartMap.put(key, chartTable);
    }

    public ChartTable addChart(String key) {
        val table = new ChartTable(key);
        setChart(key, table);
        return table;
    }

    public WordParam getParam(String key) {
        return params.get(key);
    }

    public ChartTable getChart(String key) {
        return chartMap.get(key);
    }

    public static WordParams create() {
        return new WordParams();
    }

}
