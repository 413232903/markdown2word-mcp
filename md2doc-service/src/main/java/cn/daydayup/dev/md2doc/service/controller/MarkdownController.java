package cn.daydayup.dev.md2doc.service.controller;

import cn.daydayup.dev.md2doc.service.service.MarkdownConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/markdown")
@CrossOrigin(origins = "*")
public class MarkdownController {

    @Autowired
    private MarkdownConversionService markdownConversionService;
    
    // 用于存储临时文件的目录
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/md2doc/";

    /**
     * 将上传的Markdown文件转换为Word文档
     *
     * @param file 上传的Markdown文件
     * @return 转换后的Word文档URL
     */
    @PostMapping("/convert/file")
    public ResponseEntity<Map<String, String>> convertMarkdownFile(@RequestParam("file") MultipartFile file) {
        try {
            // 创建临时目录
            Path tempDir = Paths.get(TEMP_DIR);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString();
            Path tempFilePath = tempDir.resolve(fileName + ".md");
            
            // 保存上传的Markdown文件
            file.transferTo(tempFilePath);
            
            // 生成输出文件路径
            Path outputPath = tempDir.resolve(fileName + ".docx");
            
            // 执行转换
            markdownConversionService.convertMarkdownFileToWord(tempFilePath.toString(), outputPath.toString());
            
            // 删除临时的Markdown文件
            Files.deleteIfExists(tempFilePath);
            
            // 构造文件访问URL
            String fileUrl = "/api/markdown/files/" + fileName + ".docx";
            
            // 构造响应
            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 将Markdown文本内容转换为Word文档
     *
     * @param request 包含Markdown文本内容的请求体
     * @return 转换后的Word文档URL
     */
    @PostMapping("/convert/text")
    public ResponseEntity<Map<String, String>> convertMarkdownText(@RequestBody MarkdownTextRequest request) {
        try {
            // 创建临时目录
            Path tempDir = Paths.get(TEMP_DIR);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString();
            Path tempFilePath = tempDir.resolve(fileName + ".md");
            Path outputPath = tempDir.resolve(fileName + ".docx");
            
            // 将Markdown内容写入临时文件
            Files.write(tempFilePath, request.getContent().getBytes());
            
            // 执行转换
            markdownConversionService.convertMarkdownToWord(request.getContent(), outputPath.toString());
            
            // 删除临时的Markdown文件
            Files.deleteIfExists(tempFilePath);
            
            // 构造文件访问URL
            String fileUrl = "/api/markdown/files/" + fileName + ".docx";
            
            // 构造响应
            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 提供文件下载服务
     * 
     * @param fileName 文件名
     * @return Word文档文件
     */
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("fileName") String fileName) {
        try {
            Path filePath = Paths.get(TEMP_DIR, fileName);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = Files.readAllBytes(filePath);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Markdown文本请求体
     */
    public static class MarkdownTextRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}