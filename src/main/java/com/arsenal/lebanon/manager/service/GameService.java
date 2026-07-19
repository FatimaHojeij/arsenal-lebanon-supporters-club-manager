package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.ApplicationStatus;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private NotificationService notificationService;

    public void closeExpiredGames() {
        var expiredGames = gameRepository.findExpiredOpenGames();

        if (expiredGames.isEmpty()) {
            System.out.println("🔄 Game Scan Complete: No games to close.");
            return;
        }

        expiredGames.forEach(game -> {
            List<Application> pending = applicationRepository
                    .findByGameIdAndStatus(game.getId(), ApplicationStatus.Pending);
            pending.forEach(app -> app.setStatus(ApplicationStatus.Rejected));
            applicationRepository.saveAll(pending);
            pending.forEach(notificationService::notifyIfChanged);

            game.setApplicationsOpen(false);
            gameRepository.save(game);

            System.out.println("🔒 Auto-closed: Arsenal vs " + game.getOpponent() +
                    " — " + pending.size() + " pending application(s) rejected.");
        });
    }
}