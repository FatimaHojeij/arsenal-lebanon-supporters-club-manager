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

    public record GameCloseSummary(String opponent, LocalDate matchDate, int pendingApplicationsRejected) {}

    public List<GameCloseSummary> closeExpiredGames() {
        var expiredGames = gameRepository.findExpiredOpenGames();
        List<GameCloseSummary> summaries = new ArrayList<>();

        if (expiredGames.isEmpty()) {
            System.out.println("🔄 Game Scan Complete: No games to close.");
            return summaries;
        }

        expiredGames.forEach(game -> {
            List<Application> pending = applicationRepository
                    .findByGameIdAndStatus(game.getId(), ApplicationStatus.Pending);
            pending.forEach(app -> app.setStatus(ApplicationStatus.Rejected));
            applicationRepository.saveAll(pending);
            pending.forEach(notificationService::notifyIfChanged);

            game.setApplicationsOpen(false);
            gameRepository.save(game);

            summaries.add(new GameCloseSummary(game.getOpponent(), game.getMatchDate(), pending.size()));

            System.out.println("🔒 Auto-closed: Arsenal vs " + game.getOpponent() +
                    " — " + pending.size() + " pending application(s) rejected.");
        });

        return summaries;
    }
}