package com.hutech.coca.controller;

import com.hutech.coca.dto.CommentResponse;
import com.hutech.coca.model.Article;
import com.hutech.coca.model.User;
import com.hutech.coca.service.ArticleService;
import com.hutech.coca.service.CommentService;
import com.hutech.coca.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArticleController {
    private final ArticleService articleService;
    private final CommentService commentService;
    private final CurrentUserService currentUserService;
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
    @PostMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            User currentUser = currentUserService.getCurrentUser(authorizationHeader);
            String username = currentUser.getUsername();

            String content = payload.get("content") != null ? payload.get("content").toString() : "";
            Long parentId = payload.get("parentId") != null ? Long.valueOf(payload.get("parentId").toString()) : null;

            if (content.trim().isEmpty()) {
                throw new RuntimeException("Nội dung bình luận không được để trống.");
            }

            CommentResponse comment = commentService.addComment(id, username, content, parentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã gửi bình luận");
            response.put("data", comment);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @GetMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> getComments(@PathVariable Long id) {
        List<CommentResponse> comments = commentService.getCommentsByArticle(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", comments);
        return ResponseEntity.ok(response);
    }
}