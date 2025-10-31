package cn.daydayup.dev.md2doc.core.model;

import cn.daydayup.dev.md2doc.core.util.ImageDownloader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.util.Units;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

/**
 * @ClassName WordParam
 * @Description word参数
 * @Author ZhaoYanNing
 * @Date 2025/8/12 16:48
 * @Version 1.0
 */
public sealed interface WordParam {

    static WordParam text(Object msg) {
        return new Text(String.valueOf(msg));
    }

    static WordParam image(InputStream inputStream, int width, int height) {
        return new Image(inputStream, width, height);
    }

    static WordParam image(BufferedImage image) throws IOException {
        return new Image(image);
    }

    static WordParam image(File file) throws IOException {
        return new Image(ImageIO.read(file));
    }

    /**
     * 从 URL 或本地路径创建图片参数
     * 支持自动下载网络图片和读取本地图片
     *
     * @param imageSource 图片来源（URL 或本地路径）
     * @return WordParam.Image 成功时，WordParam.Text 失败时（占位符）
     */
    static WordParam image(String imageSource) {
        try {
            BufferedImage bufferedImage = ImageDownloader.downloadOrReadImage(imageSource);
            if (bufferedImage != null) {
                return new Image(bufferedImage);
            } else {
                // 下载/读取失败，返回占位符文本
                return imagePlaceholder(imageSource, "图片加载失败");
            }
        } catch (Exception e) {
            return imagePlaceholder(imageSource, "图片处理异常: " + e.getMessage());
        }
    }

    /**
     * 创建图片加载失败的占位符
     *
     * @param imageSource 图片来源
     * @param errorMessage 错误信息
     * @return WordParam.Text 占位符
     */
    static WordParam imagePlaceholder(String imageSource, String errorMessage) {
        String placeholder = String.format("[图片加载失败: %s]\n原因: %s", imageSource, errorMessage);
        return new Text(placeholder);
    }

    // 添加表格支持
    static WordParam table(List<List<String>> data) {
        return new Table(data);
    }

    @Getter
    @AllArgsConstructor
    final class Text implements WordParam {
        private final String msg;
    }

    @Getter
    @AllArgsConstructor
    final class Image implements WordParam {
        private final InputStream inputStream;
        private final int width;
        private final int height;

        public Image(BufferedImage bufferedImage) throws IOException {
            this.inputStream = toInputStream(bufferedImage);

            // 使用自适应尺寸计算
            int[] adaptiveSize = ImageDownloader.calculateAdaptiveSize(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight()
            );

            // 修复: 使用 pixelToEMU 而不是 toEMU (toEMU 是用于 points 而不是 pixels)
            this.width = Units.pixelToEMU(adaptiveSize[0]);
            this.height = Units.pixelToEMU(adaptiveSize[1]);
        }

        private static InputStream toInputStream(BufferedImage image) throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", os);
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    // 添加表格实现类
    @Getter
    @AllArgsConstructor
    final class Table implements WordParam {
        private final List<List<String>> data;
    }
}
