package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.dto.GameRequest;
import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    private EmailService emailService;

    @GetMapping("/open")
    public List<Game> getOpenGames() {
        return gameRepository.findByApplicationsOpen(true);
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
                "applications", applications
        ));
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

        List<Application> allocated = applicationRepository.findByGameIdAndStatus(gameId, ApplicationStatus.Accepted);
        allocated.addAll(applicationRepository.findByGameIdAndStatus(gameId, ApplicationStatus.Partially_Accepted));
        allocated.forEach(app -> {
            try {
                    emailService.sendTicketAllocationEmail(
                            app.getMember(),
                            game.getOpponent(),
                            app.getTicketsGranted(),
                            game.getMatchDate()
                );
            } catch (Exception e) {
                System.out.println("⚠️ Ticket email failed for " + app.getMember().getEmail() + ": " + e.getMessage());
            }
        });

        return ResponseEntity.ok("🔒 Arsenal vs " + game.getOpponent() +
                " closed. " + pending.size() + " pending application(s) auto-rejected.");
    }

    @PostMapping("/create")
    public ResponseEntity<String> createGame(@Valid @RequestBody GameRequest request) {
        Game game = new Game();
        game.setOpponent(request.opponent());
        game.setCategory(request.category());
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