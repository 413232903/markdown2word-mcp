package cn.daydayup.dev.md2doc.core.model;

import cn.daydayup.dev.md2doc.core.util.ImageDownloader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

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

    /**
     * 从 URL 或本地路径创建图片参数
     * 支持自动下载网络图片和读取本地图片
     *
     * @param imageSource 图片来源（URL 或本地路径）
     * @return WordParam.Image 成功时，WordParam.Text 失败时（占位符）
     */
    static WordParam image(String imageSource) {
        try {
            ImageDownloader.DownloadedImage downloadedImage = ImageDownloader.downloadOrReadImage(imageSource);
            if (downloadedImage != null) {
                return Image.fromDownloaded(downloadedImage, imageSource);
            } else {
                // 下载/读取失败，返回占位符文本
                return imagePlaceholder(imageSource, "图片加载失败");
            }
        } catch (Exception e) {
            return imagePlaceholder(imageSource, "图片处理异常: " + e.getMessage());
        }
    }

    static WordParam image(BufferedImage image) throws IOException {
        return Image.fromBufferedImage(image, "png");
    }

    static WordParam image(File file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file);
        if (bufferedImage == null) {
            throw new IOException("无法读取图片文件: " + file.getAbsolutePath());
        }
        return Image.fromBufferedImage(bufferedImage, detectFormatByName(file.getName()));
    }

    private static String detectFormatByName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "png";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
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
    final class Image implements WordParam {
        private final byte[] data;
        private final int width;
        private final int height;
        private final int pictureType;
        private final String fileExtension;

        private Image(byte[] data, int originalWidth, int originalHeight, String format, int pictureType) {
            this.data = data;
            int[] adaptiveSize = ImageDownloader.calculateAdaptiveSize(originalWidth, originalHeight);
            this.width = Units.pixelToEMU(adaptiveSize[0]);
            this.height = Units.pixelToEMU(adaptiveSize[1]);
            this.pictureType = pictureType;
            this.fileExtension = format;
        }

        public static Image fromDownloaded(ImageDownloader.DownloadedImage downloadedImage, String imageSource) throws IOException {
            String format = normalizeFormat(downloadedImage.format());
            int pictureType = resolvePictureType(format);
            byte[] data = downloadedImage.data();
            int originalWidth = downloadedImage.width();
            int originalHeight = downloadedImage.height();

            if (pictureType == -1) {
                // 不支持的格式，转换为 PNG
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(downloadedImage.data()));
                if (bufferedImage == null) {
                    throw new IOException("无法解析图片用于转换");
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                data = baos.toByteArray();
                format = "png";
                pictureType = Document.PICTURE_TYPE_PNG;
                originalWidth = bufferedImage.getWidth();
                originalHeight = bufferedImage.getHeight();
            }

            return new Image(data, originalWidth, originalHeight, format, pictureType);
        }

        public static Image fromBufferedImage(BufferedImage bufferedImage, String preferredFormat) throws IOException {
            if (bufferedImage == null) {
                throw new IOException("BufferedImage 为空");
            }
            String format = preferredFormat != null ? preferredFormat : "png";
            int pictureType = resolvePictureType(format);
            if (pictureType == -1) {
                format = "png";
                pictureType = Document.PICTURE_TYPE_PNG;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, format, baos);
            return new Image(baos.toByteArray(), bufferedImage.getWidth(), bufferedImage.getHeight(), format, pictureType);
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        public String getFileExtension() {
            return fileExtension;
        }

        private static String normalizeFormat(String format) {
            return format == null ? "png" : format.toLowerCase(Locale.ROOT);
        }

        private static int resolvePictureType(String format) {
            return switch (format.toLowerCase(Locale.ROOT)) {
                case "jpg", "jpeg" -> Document.PICTURE_TYPE_JPEG;
                case "png" -> Document.PICTURE_TYPE_PNG;
                case "gif" -> Document.PICTURE_TYPE_GIF;
                case "bmp" -> Document.PICTURE_TYPE_BMP;
                case "dib" -> Document.PICTURE_TYPE_DIB;
                default -> -1;
            };
        }
    }

    // 添加表格实现类
    @Getter
    @AllArgsConstructor
    final class Table implements WordParam {
        private final List<List<String>> data;
    }
}
