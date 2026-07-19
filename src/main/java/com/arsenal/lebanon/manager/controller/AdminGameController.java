package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.dto.GameRequest;
import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/games")
public class AdminGameController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/open")
    public List<Game> getOpenGames() {
        return gameRepository.findByApplicationsOpenOrderByMatchDateAsc(true);
    }

    @GetMapping("/past")
    public List<Game> getPastGames() {
        return gameRepository.findAttendanceGames(LocalDate.now());
    }

    @PostMapping("/{gameId}/set-tickets")
    public ResponseEntity<String> setAvailableTickets(@PathVariable Long gameId, @RequestParam int tickets) {
        if (tickets < 0) {
            return ResponseEntity.badRequest().body("❌ Ticket count cannot be negative.");
        }
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found."));
        game.setAvailableTickets(tickets);
        gameRepository.save(game);
        return ResponseEntity.ok("🎟️ Available tickets for Arsenal vs " + game.getOpponent() + " set to " + tickets + ".");
    }

    @PostMapping("/{gameId}/set-category")
    public ResponseEntity<String> setCategory(@PathVariable Long gameId, @RequestParam GameCategory category) {
        if (category == GameCategory.NA) {
            return ResponseEntity.badRequest().body("❌ Category must be set to A, B, or C.");
        }
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found."));
        game.setCategory(category);
        gameRepository.save(game);
        return ResponseEntity.ok("🏷️ Category for Arsenal vs " + game.getOpponent() + " set to " + category + ".");
    }

    @GetMapping("/{gameId}/applications")
    public ResponseEntity<?> getApplicationsForGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found."));

        List<Application> applications = applicationRepository.findByGameId(gameId);
        applications.sort(Comparator.comparingInt(Application::getCalculatedPriorityScore).reversed()
                .thenComparing(Application::getAppliedAt));

        return ResponseEntity.ok(Map.of(
                "availableTickets", game.getAvailableTickets(),
                "opponent", game.getOpponent(),
                "matchDate", game.getMatchDate(),
                "category", game.getCategory(),
                "applications", applications
        ));
    }

    @Transactional
    @PostMapping("/{gameId}/reopen")
    public ResponseEntity<String> reopenGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found."));

        if (game.isApplicationsOpen()) {
            return ResponseEntity.badRequest().body("❌ This game is already open for applications.");
        }

        if (!game.getMatchDate().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body("❌ Cannot re-open a game that has already passed its match date.");
        }

        game.setApplicationsOpen(true);
        gameRepository.save(game);

        return ResponseEntity.ok("🔓 Arsenal vs " + game.getOpponent() + " has been re-opened for allocation.");
    }

    @Transactional
    @PostMapping("/{gameId}/close")
    public ResponseEntity<String> closeGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found."));

        List<Application> pending = applicationRepository.findByGameIdAndStatus(gameId, ApplicationStatus.Pending);
        pending.forEach(app -> app.setStatus(ApplicationStatus.Rejected));
        applicationRepository.saveAll(pending);

        game.setApplicationsOpen(false);
        gameRepository.save(game);

        // Notify everyone with a final outcome — Accepted, Partially Accepted, or Rejected —
        // but notifyIfChanged silently skips anyone already emailed about this exact outcome.
        List<Application> toNotify = applicationRepository.findByGameIdAndStatus(gameId, ApplicationStatus.Accepted);
        toNotify.addAll(applicationRepository.findByGameIdAndStatus(gameId, ApplicationStatus.Partially_Accepted));
        toNotify.addAll(applicationRepository.findByGameIdAndStatus(gameId, ApplicationStatus.Rejected));
        toNotify.forEach(notificationService::notifyIfChanged);

        return ResponseEntity.ok("🔒 Arsenal vs " + game.getOpponent() +
                " closed. " + pending.size() + " pending application(s) auto-rejected.");
    }

    @PostMapping("/create")
    public ResponseEntity<String> createGame(@Valid @RequestBody GameRequest request) {
        Game game = new Game();
        game.setOpponent(request.opponent());
        game.setCategory(request.categoryOrDefault());
        game.setAvailableTickets(request.ticketsOrDefault());
        game.setCompetition(request.competition());
        game.setMatchDate(request.matchDate());
        game.setDeadline(request.deadline());
        game.setApplicationsOpen(true);

        gameRepository.save(game);
        return ResponseEntity.ok("⚽ Match successfully created: Arsenal vs " + game.getOpponent() +
                " (Category " + game.getCategory() + ") with ID: " + game.getId());
    }

}