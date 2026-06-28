package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.service.PriorityScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PriorityScoreService priorityScoreService;

    // Submit a ticket application — calculates and persists priority score immediately
    @PostMapping("/apply")
    public ResponseEntity<String> applyForTickets(
            @RequestParam Long gameId,
            @RequestParam int ticketsRequested,
            @RequestParam boolean allOrNothing) {

        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member record not found."));

        if (member.getStatus() != MembershipStatus.Active) {
            return ResponseEntity.badRequest().body(
                    "❌ Only Active members can apply. Your status is: " + member.getStatus());
        }

        if (ticketsRequested < 1 || ticketsRequested > 2) {
            return ResponseEntity.badRequest().body("❌ You may request 1 or 2 tickets only.");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found."));

        if (!game.isApplicationsOpen()) {
            return ResponseEntity.badRequest().body("❌ Applications for this match are closed.");
        }

        if (applicationRepository.existsByMemberAndGame(member, game)) {
            return ResponseEntity.badRequest().body(
                    "❌ You have already applied for Arsenal vs " + game.getOpponent() + ".");
        }

        Application application = new Application();
        application.setMember(member);
        application.setGame(game);
        application.setStatus(ApplicationStatus.Pending);
        application.setAppliedAt(LocalDateTime.now());
        application.setTicketsRequested(ticketsRequested);
        application.setAllOrNothing(allOrNothing);

        // Calculate and persist priority score at submission time
        int score = priorityScoreService.calculate(member);
        application.setCalculatedPriorityScore(score);

        applicationRepository.save(application);
        return ResponseEntity.ok("🎟️ Application submitted for Arsenal vs " + game.getOpponent() +
                " (" + ticketsRequested + " ticket(s)). Priority score: " + score);
    }

    // Logged-in member's own application history
    @GetMapping("/my-applications")
    public List<Application> getMyApplications() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = memberRepository.findByEmail(email).orElseThrow();
        return applicationRepository.findByMember(member);
    }
}
