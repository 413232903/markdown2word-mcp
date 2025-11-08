package cn.daydayup.dev.md2doc.service.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

/**
 * 定时清理临时目录中超过保留期的 Word 文件，防止磁盘空间被长期占用。
 */
@Component
public class TempFileCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(TempFileCleanupTask.class);

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/md2doc/";

    private final Duration retentionDuration;

    public TempFileCleanupTask(
            @Value("${md2doc.temp-file-retention-days:10}") int retentionDays) {
        this.retentionDuration = Duration.ofDays(Math.max(retentionDays, 0));
    }

    /**
     * 每天凌晨 3 点执行一次清理任务，删除超过保留期的临时文件。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanUpExpiredFiles() {
        Path tempDir = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDir)) {
            return;
        }

        Instant expirationThreshold = Instant.now().minus(retentionDuration);
        int deletedCount = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
            for (Path filePath : stream) {
                if (!Files.isRegularFile(filePath)) {
                    continue;
                }

                FileTime lastModified = Files.getLastModifiedTime(filePath);
                if (lastModified.toInstant().isBefore(expirationThreshold)) {
                    try {
                        Files.deleteIfExists(filePath);
                        deletedCount++;
                    } catch (IOException deleteException) {
                        log.warn("删除临时文件失败: {}", filePath, deleteException);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("清理临时文件目录失败: {}", tempDir, e);
            return;
        }

        if (deletedCount > 0) {
            log.info("临时目录清理完成，共删除 {} 个超过 {} 天的文件", deletedCount,
                    retentionDuration.toDays());
        }
    }
}

