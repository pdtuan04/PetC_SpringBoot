package com.hutech.coca.controller;

import com.hutech.coca.model.Article;
import com.hutech.coca.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArticleController {
    private final ArticleService articleService;
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPublishedArticles() {
        List<Article> articles = articleService.getPublishedArticles();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", articles);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getArticleDetail(@PathVariable Long id) {
        try {
            Article article = articleService.getArticleById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", article);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Dành cho Admin: Lấy toàn bộ bài viết
    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> getAllArticlesForAdmin() {
        List<Article> articles = articleService.getAllArticles();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", articles);
        return ResponseEntity.ok(response);
    }

    // Admin: Thêm bài viết mới
    @PostMapping("/admin")
    public ResponseEntity<Map<String, Object>> createArticle(@RequestBody Article article) {
        Article newArticle = articleService.createArticle(article);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thêm bài viết thành công!");
        response.put("data", newArticle);
        return ResponseEntity.ok(response);
    }

    // Admin: Cập nhật bài viết
    @PutMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        try {
            Article updated = articleService.updateArticle(id, article);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật thành công!");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Admin: Xóa bài viết
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã xóa bài viết!");
        return ResponseEntity.ok(response);
    }
}