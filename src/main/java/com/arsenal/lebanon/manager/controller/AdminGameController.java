package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminGameController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/games/open")
    public List<Game> getOpenGames() {
        return gameRepository.findByApplicationsOpen(true);
    }

    @PostMapping("/games/{gameId}/set-tickets")
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

    @GetMapping("/games/{gameId}/applications")
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
    @PostMapping("/applications/{appId}/allocate")
    public ResponseEntity<String> allocateApplication(
            @PathVariable Long appId,
            @RequestParam int ticketsGranted) {

        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        Game game = app.getGame();

        if (app.getMember().getStatus() != MembershipStatus.Active) {
            return ResponseEntity.badRequest().body("❌ Member is not Active — cannot allocate tickets.");
        }
        if (ticketsGranted < 1) {
            return ResponseEntity.badRequest().body("❌ Must grant at least 1 ticket.");
        }
        if (app.isAllOrNothing() && ticketsGranted != app.getTicketsRequested()) {
            return ResponseEntity.badRequest().body(
                    "❌ This application is All-or-Nothing: must grant exactly " +
                            app.getTicketsRequested() + " ticket(s) or reject it.");
        }
        if (ticketsGranted > app.getTicketsRequested()) {
            return ResponseEntity.badRequest().body(
                    "❌ Cannot grant more tickets than requested (" + app.getTicketsRequested() + ").");
        }
        if (game.getAvailableTickets() < ticketsGranted) {
            return ResponseEntity.badRequest().body(
                    "❌ Not enough tickets remaining. Available: " + game.getAvailableTickets());
        }

        game.setAvailableTickets(game.getAvailableTickets() - ticketsGranted);
        app.setTicketsGranted(ticketsGranted);
        app.setStatus(ticketsGranted == app.getTicketsRequested()
                ? ApplicationStatus.Accepted
                : ApplicationStatus.Partially_Accepted);

        gameRepository.save(game);
        applicationRepository.save(app);

        return ResponseEntity.ok("✅ Allocated " + ticketsGranted + " ticket(s) to " +
                app.getMember().getFirstName() + " " + app.getMember().getLastName() +
                ". Remaining pool: " + game.getAvailableTickets());
    }

    @Transactional
    @PostMapping("/applications/{appId}/deallocate")
    public ResponseEntity<String> deallocateApplication(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (app.getStatus() != ApplicationStatus.Accepted && app.getStatus() != ApplicationStatus.Partially_Accepted) {
            return ResponseEntity.badRequest().body("❌ Only Accepted or Partially Accepted applications can be deallocated.");
        }

        Game game = app.getGame();
        game.setAvailableTickets(game.getAvailableTickets() + app.getTicketsGranted());
        app.setStatus(ApplicationStatus.Pending);

        gameRepository.save(game);
        applicationRepository.save(app);

        return ResponseEntity.ok("↩️ Allocation reversed for " +
                app.getMember().getFirstName() + " " + app.getMember().getLastName() +
                ". Tickets returned to pool. Remaining: " + game.getAvailableTickets());
    }

    @PostMapping("/applications/{appId}/reject")
    public ResponseEntity<String> rejectApplication(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        app.setStatus(ApplicationStatus.Rejected);
        applicationRepository.save(app);
        return ResponseEntity.ok("❌ Application rejected for " +
                app.getMember().getFirstName() + " " + app.getMember().getLastName() + ".");
    }

    @Transactional
    @PostMapping("/games/{gameId}/close")
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
}