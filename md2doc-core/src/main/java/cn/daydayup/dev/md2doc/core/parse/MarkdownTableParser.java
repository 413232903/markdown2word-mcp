package cn.daydayup.dev.md2doc.core.parse;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName MarkdownTableParser
 * @Description 解析Markdown表格字符串为二维列表
 * @Author ZhaoYanNing
 * @Date 2025/8/13 9:23
 * @Version 1.0
 */
public class MarkdownTableParser {

    /**
     * 解析Markdown表格字符串为二维列表
     * @param markdownTable Markdown表格字符串
     * @return 表格数据的二维列表
     */
    public static List<List<String>> parseTable(String markdownTable) {
        List<List<String>> tableData = new ArrayList<>();

        String[] lines = markdownTable.split("\n");
        for (String line : lines) {
            line = line.trim();
            // 跳过分隔行（只包含|和-的行）
            if (line.matches("^\\|?\\s*[-|:\\s]+\\|?\\s*$") && line.contains("-")) {
                continue;
            }

            if (line.startsWith("|")) {
                line = line.substring(1);
            }
            if (line.endsWith("|")) {
                line = line.substring(0, line.length() - 1);
            }

            String[] cells = line.split("\\|");
            List<String> row = new ArrayList<>();
            for (String cell : cells) {
                row.add(cell.trim());
            }
            // 只有当行不为空时才添加到表格数据中
            if (!row.isEmpty() && !(row.size() == 1 && row.get(0).isEmpty())) {
                tableData.add(row);
            }
        }

        return tableData;
    }
}