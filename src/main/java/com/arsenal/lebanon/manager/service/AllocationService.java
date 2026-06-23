package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AllocationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private GameRepository gameRepository;

    @Transactional
    public void allocateTicketsForGame(Long gameId) {
        // 1. Fetch the game details from the database
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with ID: " + gameId));

        // 2. Pull all PENDING applications for this specific match
        List<Application> pendingApplications = applicationRepository.findByGameIdAndStatus(gameId, ApplicationStatus.Pending);

        // 3. Compute and cache priority scores for every single applicant
        for (Application app : pendingApplications) {
            int score = calculatePriorityScore(app.getMember());
            app.setCalculatedPriorityScore(score);
        }

        // 4. Sort applications: Highest score first.
        // Tie-breaker: If scores match, order by the earlier submission timestamp (First-Come, First-Served)
        pendingApplications.sort(
                Comparator.comparing(Application::getCalculatedPriorityScore).reversed()
                        .thenComparing(Application::getAppliedAt)
        );

        // 5. Allocate tickets up to the maximum available capacity limit
        int ticketsLeft = game.getAvailableTickets();

        for (Application app : pendingApplications) {
            // Safety Check: Reject anyone whose membership status is not strictly Active
            if (app.getMember().getStatus() != MembershipStatus.Active) {
                app.setStatus(ApplicationStatus.Rejected);
                continue;
            }

            if (ticketsLeft > 0) {
                app.setStatus(ApplicationStatus.Accepted);
                ticketsLeft--;
            } else {
                app.setStatus(ApplicationStatus.Rejected); // No ticket inventory remaining
            }
        }

        // 6. Bulk save updates safely straight back to your Supabase cloud instance
        applicationRepository.saveAll(pendingApplications);
        System.out.println("🎟️ Ticket allocation finalized for match against " + game.getOpponent() + "!");
    }

    private int calculatePriorityScore(Member member) {
        int score = 0;

        // Apply positive weight metrics
        score += (member.getTotalGamesAttended() * 2);
        score += (member.getGamesAttendedThisSeason() * 5);
        score += (member.getCategoryAGamesThisSeason() * 10);

        // Apply strict deduction penalty thresholds
        score -= (member.getDefaultedGamesCount() * 15);
        score -= member.getCustomPenaltyPoints();

        return score;
    }
}
