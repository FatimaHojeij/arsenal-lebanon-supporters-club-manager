package com.arsenal.lebanon.manager.repository;

import com.arsenal.lebanon.manager.model.NewsPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsPostRepository extends JpaRepository<NewsPost, Long> {
    List<NewsPost> findAllByOrderByCreatedAtDesc();
    long countByCreatedAtAfter(LocalDateTime time);
}
