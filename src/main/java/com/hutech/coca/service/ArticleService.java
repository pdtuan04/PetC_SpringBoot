package com.hutech.coca.service;

import com.hutech.coca.model.Article;
import com.hutech.coca.repository.IArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final IArticleRepository articleRepository;
    public List<Article> getPublishedArticles() {
        return articleRepository.findByIsPublishedTrueOrderByCreatedAtDesc();
    }
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }
    public Article getArticleById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết!"));
    }
    public Article createArticle(Article article) {
        return articleRepository.save(article);
    }
    public Article updateArticle(Long id, Article updatedData) {
        Article existing = getArticleById(id);
        existing.setTitle(updatedData.getTitle());
        existing.setSummary(updatedData.getSummary());
        existing.setContent(updatedData.getContent());
        existing.setImageUrl(updatedData.getImageUrl());
        existing.setPublished(updatedData.isPublished());

        return articleRepository.save(existing);
    }
    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }
}