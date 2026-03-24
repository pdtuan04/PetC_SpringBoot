package com.hutech.coca.repository;

import com.hutech.coca.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByIsPublishedTrueOrderByCreatedAtDesc();
}