package com.hutech.coca.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UploadController {
    
    private static final String UPLOAD_DIR = "uploads/";
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate file không rỗng
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate file extension (bảo mật)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                response.put("success", false);
                response.put("message", "File không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif");
            
            if (!allowedExtensions.contains(extension)) {
                response.put("success", false);
                response.put("message", "Chỉ chấp nhận file: JPG, JPEG, PNG, WEBP, GIF");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate Content-Type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "File không phải là ảnh");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate file size (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "Kích thước file tối đa 5MB");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Tạo thư mục nếu chưa có
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // Tạo tên file unique với extension đã validate
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // Lưu file
            Path path = Paths.get(UPLOAD_DIR + newFilename);
            Files.write(path, file.getBytes());
            
            // Return URL
            String fileUrl = "/uploads/" + newFilename;
            
            response.put("success", true);
            response.put("message", "Upload thành công");
            response.put("data", fileUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Upload thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
