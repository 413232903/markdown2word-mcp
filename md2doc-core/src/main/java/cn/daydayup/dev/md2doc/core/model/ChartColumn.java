package cn.daydayup.dev.md2doc.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.*;

/**
 * @ClassName ChartColumn
 * @Description word图表-字段
 * @Author ZhaoYanNing
 * @Date 2025/8/12 16:46
 * @Version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class ChartColumn<T> implements Iterable<T> {
    private String title;
    private List<T> dateList = new ArrayList<>();

    public ChartColumn(String title) {
        this.title = title;
    }

    public ChartColumn<T> setTitle(String title) {
        this.title = title;
        return this;
    }

    public int size() {
        return dateList.size();
    }

    @SafeVarargs
    public final void addAllData(T... date) {
        dateList.addAll(Arrays.asList(date));
    }

    public final void addAllData(Collection<? extends T> date) {
        dateList.addAll(date);
    }

    @Override
    public Iterator<T> iterator() {
        return dateList.iterator();
    }

    public T[] toArray(T[] arr) {
        return dateList.toArray(arr);
    }
}