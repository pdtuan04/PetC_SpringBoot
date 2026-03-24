package com.hutech.coca.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title; // Tiêu đề bài viết

    @Column(length = 500)
    private String summary; // Đoạn mô tả ngắn (hiển thị ở thẻ bài viết ngoài trang chủ)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // Nội dung chi tiết (Có thể lưu HTML từ trình soạn thảo Rich Text)

    @Column(name = "image_url")
    private String imageUrl; // Link ảnh bìa

    @Column(name = "is_published")
    private boolean isPublished = true; // Trạng thái ẩn/hiện bài viết

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}