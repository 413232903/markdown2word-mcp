package cn.daydayup.dev.md2doc.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;

import java.util.HashMap;

/**
 * @ClassName ChartTable
 * @Description word图表-表
 * @Author ZhaoYanNing
 * @Date 2025/8/12 16:47
 * @Version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class ChartTable {
    private String title;
    private ChartColumn<String> xAxis = new ChartColumn<>("x轴");
    private HashMap<String, ChartColumn<Number>> yAxis = new HashMap<>();

    public ChartTable(String title) {
        this.title = title;
    }

    public ChartTable setTitle(String title) {
        this.title = title;
        return this;
    }

    public ChartColumn<Number> newYAxis(String title) {
        val column = new ChartColumn<Number>();
        yAxis.put(title, column);
        return column;
    }

    public ChartColumn<Number> getYAxis(String title) {
        return yAxis.get(title);
    }

}
