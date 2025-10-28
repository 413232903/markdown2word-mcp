package cn.daydayup.dev.md2doc.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @ClassName ImageDownloader
 * @Description 图片下载和处理工具类
 * @Author ZhaoYanNing
 * @Date 2025/10/26
 * @Version 1.0
 */
public class ImageDownloader {

    private static final Logger logger = LogManager.getLogger(ImageDownloader.class);

    /**
     * 下载超时时间（毫秒）
     */
    private static final int DOWNLOAD_TIMEOUT = 10000; // 10秒

    /**
     * 最大图片宽度（像素）- 约15cm，适合A4页面
     */
    public static final int MAX_WIDTH_PIXELS = 600;

    /**
     * 支持的图片格式
     */
    private static final String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};

    /**
     * 下载或读取图片
     *
     * @param imageSource 图片来源（URL或本地路径）
     * @return BufferedImage 对象，失败返回 null
     */
    public static BufferedImage downloadOrReadImage(String imageSource) {
        if (imageSource == null || imageSource.trim().isEmpty()) {
            logger.warn("图片来源为空");
            return null;
        }

        imageSource = imageSource.trim();

        // 判断是 URL 还是本地路径
        if (isUrl(imageSource)) {
            return downloadImageFromUrl(imageSource);
        } else {
            return readLocalImage(imageSource);
        }
    }

    /**
     * 判断是否为 URL
     */
    private static boolean isUrl(String source) {
        return source.startsWith("http://") || source.startsWith("https://");
    }

    /**
     * 从 URL 下载图片
     */
    private static BufferedImage downloadImageFromUrl(String imageUrl) {
        logger.info("开始下载网络图片: {}", imageUrl);
        HttpURLConnection connection = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (md2doc Image Downloader)");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.error("下载图片失败，HTTP 响应码: {} - {}", responseCode, imageUrl);
                return null;
            }

            try (InputStream inputStream = connection.getInputStream()) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    logger.error("无法解析图片格式: {}", imageUrl);
                    return null;
                }
                logger.info("成功下载图片: {} ({}x{})", imageUrl, image.getWidth(), image.getHeight());
                return image;
            }

        } catch (IOException e) {
            logger.error("下载图片异常: {} - {}", imageUrl, e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 读取本地图片
     */
    private static BufferedImage readLocalImage(String imagePath) {
        logger.info("开始读取本地图片: {}", imagePath);

        try {
            // 处理相对路径和绝对路径
            Path path = Paths.get(imagePath);
            if (!path.isAbsolute()) {
                // 相对路径，尝试相对于当前工作目录
                path = Paths.get(System.getProperty("user.dir"), imagePath);
            }

            File imageFile = path.toFile();
            if (!imageFile.exists()) {
                logger.error("图片文件不存在: {}", path.toAbsolutePath());
                return null;
            }

            if (!imageFile.isFile()) {
                logger.error("不是有效的文件: {}", path.toAbsolutePath());
                return null;
            }

            // 验证文件格式
            String fileName = imageFile.getName().toLowerCase();
            boolean isSupportedFormat = false;
            for (String format : SUPPORTED_FORMATS) {
                if (fileName.endsWith("." + format)) {
                    isSupportedFormat = true;
                    break;
                }
            }

            if (!isSupportedFormat) {
                logger.error("不支持的图片格式: {}", fileName);
                return null;
            }

            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                logger.error("无法解析图片: {}", path.toAbsolutePath());
                return null;
            }

            logger.info("成功读取本地图片: {} ({}x{})", imagePath, image.getWidth(), image.getHeight());
            return image;

        } catch (IOException e) {
            logger.error("读取本地图片异常: {} - {}", imagePath, e.getMessage());
            return null;
        }
    }

    /**
     * 计算自适应尺寸（保持宽高比）
     *
     * @param originalWidth 原始宽度
     * @param originalHeight 原始高度
     * @return [新宽度, 新高度]
     */
    public static int[] calculateAdaptiveSize(int originalWidth, int originalHeight) {
        if (originalWidth <= MAX_WIDTH_PIXELS) {
            // 不需要缩放
            return new int[]{originalWidth, originalHeight};
        }

        // 需要缩放
        double scale = (double) MAX_WIDTH_PIXELS / originalWidth;
        int newWidth = MAX_WIDTH_PIXELS;
        int newHeight = (int) (originalHeight * scale);

        logger.debug("图片尺寸自适应: {}x{} -> {}x{}",
                originalWidth, originalHeight, newWidth, newHeight);

        return new int[]{newWidth, newHeight};
    }

    /**
     * 检查图片来源是否可访问
     *
     * @param imageSource 图片来源
     * @return 是否可访问
     */
    public static boolean isAccessible(String imageSource) {
        if (imageSource == null || imageSource.trim().isEmpty()) {
            return false;
        }

        if (isUrl(imageSource)) {
            return isUrlAccessible(imageSource);
        } else {
            return isLocalFileAccessible(imageSource);
        }
    }

    /**
     * 检查 URL 是否可访问（HEAD 请求）
     */
    private static boolean isUrlAccessible(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 检查本地文件是否可访问
     */
    private static boolean isLocalFileAccessible(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            if (!path.isAbsolute()) {
                path = Paths.get(System.getProperty("user.dir"), imagePath);
            }
            return Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
        } catch (Exception e) {
            return false;
        }
    }
}
