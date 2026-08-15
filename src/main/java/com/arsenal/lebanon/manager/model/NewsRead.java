package com.arsenal.lebanon.manager.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_reads", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id","news_post_id"}))
@Data
public class NewsRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Member member;

    @ManyToOne
    private NewsPost newsPost;

    private LocalDateTime readAt;
}
