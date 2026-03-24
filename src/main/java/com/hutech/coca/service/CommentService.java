package com.hutech.coca.service;

import com.hutech.coca.dto.CommentResponse;
import com.hutech.coca.model.Article;
import com.hutech.coca.model.Comment;
import com.hutech.coca.model.User;
import com.hutech.coca.repository.IArticleRepository;
import com.hutech.coca.repository.ICommentRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final ICommentRepository commentRepository;
    private final IArticleRepository articleRepository;
    private final IUserRepository userRepository;

    @Transactional
    public CommentResponse addComment(Long articleId, String username, String content, Long parentId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Comment comment = Comment.builder()
                .content(content)
                .article(article)
                .user(user)
                .parentId(parentId) // LƯU PARENT ID
                .build();

        comment = commentRepository.save(comment);

        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setUsername(user.getUsername());
        response.setParentId(comment.getParentId());
        response.setCreatedAt(comment.getCreatedAt());

        return response;
    }

    public List<CommentResponse> getCommentsByArticle(Long articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId).stream().map(c -> {
            CommentResponse dto = new CommentResponse();
            dto.setId(c.getId());
            dto.setContent(c.getContent());
            dto.setUsername(c.getUser().getUsername());
            dto.setParentId(c.getParentId());
            dto.setCreatedAt(c.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}