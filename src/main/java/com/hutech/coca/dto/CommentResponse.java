package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CommentResponse {
    private Long id;
    private String content;
    private String username;
    private Long parentId; // THÊM DÒNG NÀY
    private LocalDateTime createdAt;
}