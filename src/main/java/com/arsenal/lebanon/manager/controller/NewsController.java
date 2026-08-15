package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.NewsPost;
import com.arsenal.lebanon.manager.model.NewsRead;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.repository.NewsPostRepository;
import com.arsenal.lebanon.manager.repository.NewsReadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class NewsController {

    @Autowired
    private NewsPostRepository newsPostRepository;

    @Autowired
    private NewsReadRepository newsReadRepository;

    @Autowired
    private MemberRepository memberRepository;

    // Admin creates a news post (optional image)
    @PostMapping(path = "/admin/news", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createNews(@RequestParam String title,
                                             @RequestParam String content,
                                             @RequestPart(required = false) MultipartFile image) throws Exception {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member author = memberRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Member not found"));

        NewsPost post = new NewsPost();
        post.setTitle(title);
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(author);

        if (image != null && !image.isEmpty()) {
            post.setImageData(image.getBytes());
            post.setImageContentType(image.getContentType());
        }

        newsPostRepository.save(post);
        return ResponseEntity.ok("✅ News posted.");
    }

    // List news posts — include author name only for admins
    @GetMapping("/news")
    public List<Map<String, Object>> listNews() {
        String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().endsWith("ADMIN"));

        List<NewsPost> posts = newsPostRepository.findAllByOrderByCreatedAtDesc();

        return posts.stream().map(p -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("title", p.getTitle());
            item.put("content", p.getContent());
            item.put("createdAt", p.getCreatedAt().toString());
            item.put("hasImage", p.getImageData() != null);
            item.put("authorName", isAdmin && p.getAuthor() != null ? (p.getAuthor().getFirstName() + " " + p.getAuthor().getLastName()) : null);
            return item;
        }).collect(Collectors.toList());
    }

    @GetMapping("/news/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        NewsPost post = newsPostRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("News not found"));
        if (post.getImageData() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, post.getImageContentType()).body(post.getImageData());
    }

    // Mark a post as read by current user
    @PostMapping("/{id}/mark-read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = memberRepository.findByEmail(email).orElseThrow();
        NewsPost post = newsPostRepository.findById(id).orElseThrow();

        if (!newsReadRepository.existsByMemberAndNewsPost(member, post)) {
            NewsRead read = new NewsRead();
            read.setMember(member);
            read.setNewsPost(post);
            read.setReadAt(LocalDateTime.now());
            newsReadRepository.save(read);
        }
        return ResponseEntity.ok().build();
    }

    // Unread count for current user
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = memberRepository.findByEmail(email).orElseThrow();

        long unread = newsReadRepository.findLastReadCreatedAt(member)
                .map(newsPostRepository::countByCreatedAtAfter)
                .orElse(0L);

        return ResponseEntity.ok(unread);
    }
}
