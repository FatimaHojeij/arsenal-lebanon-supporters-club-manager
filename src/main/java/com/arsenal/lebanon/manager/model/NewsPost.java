package com.arsenal.lebanon.manager.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_posts")
@Data
public class NewsPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content; // stored as HTML

    @Lob
    private byte[] imageData;

    private String imageContentType;

    private LocalDateTime createdAt;

    @ManyToOne
    private Member author;
}
