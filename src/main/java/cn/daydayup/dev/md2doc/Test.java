package cn.daydayup.dev.md2doc;

/**
 * @ClassName Test
 * @Description 测试
 * @Author ZhaoYanNing
 * @Date 2025/8/13 15:12
 * @Version 1.0
 */
public class Test {

    public static void main(String[] args) throws Exception {
        MarkdownToWordConverter.convertMarkdownFileToWord("./markdown/未命名.md",
                "./word/未命名_output.docx");
    }
}
