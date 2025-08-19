package cn.daydayup.dev.md2doc.service.service;

import cn.daydayup.dev.md2doc.core.MarkdownToWordConverter;
import org.springframework.stereotype.Service;

@Service
public class MarkdownConversionService {

    private final MarkdownToWordConverter converter = new MarkdownToWordConverter();

    /**
     * 将Markdown文件转换为Word文档
     *
     * @param markdownPath Markdown文件路径
     * @param outputPath 输出Word文档路径
     * @throws Exception 转换过程中可能抛出的异常
     */
    public void convertMarkdownFileToWord(String markdownPath, String outputPath) throws Exception {
        converter.convertMarkdownFileToWord(markdownPath, outputPath);
    }

    /**
     * 将Markdown内容转换为Word文档
     *
     * @param markdownContent Markdown内容
     * @param outputPath 输出Word文档路径
     * @throws Exception 转换过程中可能抛出的异常
     */
    public void convertMarkdownToWord(String markdownContent, String outputPath) throws Exception {
        converter.convertMarkdownToWord(markdownContent, outputPath);
    }
}