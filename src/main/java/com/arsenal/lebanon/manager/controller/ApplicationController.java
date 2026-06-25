package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 1. Submit a new ticket application (Only allowed if member is ACTIVE)
    @PostMapping("/apply")
    public String applyForTickets(@RequestParam Long gameId) {
        try {
            // Get logged-in user's email from the security JWT payload context
            String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated member record not found."));

            // 🛑 CRITICAL CHECK: Enforce active membership status restriction
            if (member.getStatus() != MembershipStatus.Active) {
                return "❌ Application Rejected: Your membership status is " + member.getStatus() +
                        ". Only Active members can apply for match tickets.";
            }

            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new IllegalArgumentException("Game match not found."));

            if (!game.isApplicationsOpen()) {
                return "❌ Error: Ticket applications for this match are currently closed.";
            }

            // Create and save the application structure
            Application application = new Application();
            application.setMember(member);
            application.setGame(game);
            application.setStatus(ApplicationStatus.Pending);
            application.setAppliedAt(LocalDateTime.now());

            applicationRepository.save(application);
            return "🎟️ Success! Your application for Arsenal vs " + game.getOpponent() + " has been submitted as PENDING.";

        } catch (Exception e) {
            return "❌ Submission error: " + e.getMessage();
        }
    }

    // 2. Get all applications belonging to the currently logged-in user
    @GetMapping("/my-applications")
    public List<Application> getMyApplications() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = memberRepository.findByEmail(email).orElseThrow();
        return applicationRepository.findByMember(member);
    }
}