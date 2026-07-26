package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.ApplicationStatus;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private NotificationService notificationService;

    public record GameCloseSummary(String opponent, LocalDate matchDate, int pendingApplicationsAwaitingAllocation) {}

    public List<GameCloseSummary> closeExpiredGames() {
        var expiredGames = gameRepository.findExpiredOpenGames();
        List<GameCloseSummary> summaries = new ArrayList<>();

        if (expiredGames.isEmpty()) {
            System.out.println("🔄 Game Scan Complete: No games to close.");
            return summaries;
        }

        expiredGames.forEach(game -> {
            // Deadline passing only stops NEW applications — it does not
            // touch existing Pending applications. Those stay Pending and
            // remain visible to admins in the Allocation panel (tagged
            // "Applications Closed") so tickets can still be allocated
            // once Arsenal confirm the club's allocation.
            List<Application> pending = applicationRepository
                    .findByGameIdAndStatus(game.getId(), ApplicationStatus.Pending);

            game.setApplicationsOpen(false);
            gameRepository.save(game);

            summaries.add(new GameCloseSummary(game.getOpponent(), game.getMatchDate(), pending.size()));

            System.out.println("🔒 Auto-closed (deadline passed): Arsenal vs " + game.getOpponent() +
                    " — " + pending.size() + " pending application(s) awaiting admin allocation.");
        });

        return summaries;
    }
}