package cn.edu.zjut.im.controller;

import cn.edu.zjut.im.service.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileController {

    @Value("${im.upload.dir:uploads}")
    private String uploadDir;

    @Value("${im.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @PostMapping
    @Operation(summary = "上传文件")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .success(false).message("文件为空").build());
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .success(false).message("仅支持图片文件").build());
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;
            Path targetPath = uploadPath.resolve(filename);
            file.transferTo(targetPath.toFile());

            String url = urlPrefix + "/" + filename;
            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .success(true).message("上传成功")
                    .data(Map.of("url", url, "filename", filename))
                    .build());
        } catch (IOException e) {
            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .success(false).message("上传失败: " + e.getMessage()).build());
        }
    }
}
