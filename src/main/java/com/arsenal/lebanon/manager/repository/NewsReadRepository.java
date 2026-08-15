package com.arsenal.lebanon.manager.repository;

import com.arsenal.lebanon.manager.model.NewsRead;
import com.arsenal.lebanon.manager.model.NewsPost;
import com.arsenal.lebanon.manager.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NewsReadRepository extends JpaRepository<NewsRead, Long> {
    Optional<NewsRead> findByMemberAndNewsPost(Member member, NewsPost newsPost);
    @Query("SELECT COUNT(p) FROM NewsPost p WHERE p NOT IN " +
            "(SELECT r.newsPost FROM NewsRead r WHERE r.member = :member)")
    long countUnreadForMember(@Param("member") Member member);
    @Query("SELECT MAX(r.newsPost.createdAt) FROM NewsRead r WHERE r.member = :member")
    Optional<LocalDateTime> findLastReadCreatedAt(@Param("member") Member member);

    boolean existsByMemberAndNewsPost(Member member, NewsPost post);
}
