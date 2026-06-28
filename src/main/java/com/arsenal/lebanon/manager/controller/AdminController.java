package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private GameRepository gameRepository;

    // ─── Member Management ────────────────────────────────────────────────────

    // All members with Pending status awaiting approval
    @GetMapping("/members/pending")
    public List<Member> getPendingMembers() {
        return memberRepository.findByStatus(MembershipStatus.Pending);
    }

    // Approve a pending member → sets status to Active
    @PostMapping("/members/{id}/approve")
    public ResponseEntity<String> approveMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        member.setStatus(MembershipStatus.Active);
        memberRepository.save(member);
        return ResponseEntity.ok("✅ " + member.getFirstName() + " " + member.getLastName() + " approved and activated.");
    }

    // Reject a pending member → permanently deletes from DB
    @DeleteMapping("/members/{id}/reject")
    public ResponseEntity<String> rejectMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        String name = member.getFirstName() + " " + member.getLastName();
        memberRepository.delete(member);
        return ResponseEntity.ok("🗑️ Registration for " + name + " has been rejected and removed.");
    }

    // Ban an existing member
    @PostMapping("/members/{id}/ban")
    public ResponseEntity<String> banMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        member.setStatus(MembershipStatus.Banned);
        memberRepository.save(member);
        return ResponseEntity.ok("🚫 " + member.getFirstName() + " " + member.getLastName() + " has been banned.");
    }

    // Add custom penalty points to a member
    @PostMapping("/members/{id}/penalize")
    public ResponseEntity<String> penalizeMember(@PathVariable Long id, @RequestParam int points) {
        if (points <= 0) {
            return ResponseEntity.badRequest().body("❌ Penalty points must be a positive number.");
        }
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        member.setCustomPenaltyPoints(member.getCustomPenaltyPoints() + points);
        memberRepository.save(member);
        return ResponseEntity.ok("⚠️ " + points + " penalty point(s) added to " +
                member.getFirstName() + " " + member.getLastName() +
                ". Total: " + member.getCustomPenaltyPoints());
    }

    // ─── Game Management ──────────────────────────────────────────────────────

    // All games that still have applications open
    @GetMapping("/games/open")
    public List<Game> getOpenGames() {
        return gameRepository.findByApplicationsOpen(true);
    }

    // Set available tickets for a game (admin enters this manually when info arrives)
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

    // All applications for a game, ordered by priority score descending
    // Also returns the current availableTickets count so the frontend can display it
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

    // ─── Application Allocation ───────────────────────────────────────────────

    // Allocate tickets to one application manually
    // ticketsGranted must equal app.tickets if allOrNothing is true
    @PostMapping("/applications/{appId}/allocate")
    public ResponseEntity<String> allocateApplication(
            @PathVariable Long appId,
            @RequestParam int ticketsGranted) {

        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        Game game = app.getGame();

        if (app.getMember().getStatus() != MembershipStatus.Active) {
            return ResponseEntity.badRequest()
                    .body("❌ Member is not Active — cannot allocate tickets.");
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

        // Deduct from pool and accept
        game.setAvailableTickets(game.getAvailableTickets() - ticketsGranted);
        app.setTicketsGranted(ticketsGranted); // Record the actual granted amount

        if (ticketsGranted == app.getTicketsRequested()) {
            app.setStatus(ApplicationStatus.Accepted);
        } else if(ticketsGranted < app.getTicketsRequested())
        {
            app.setStatus(ApplicationStatus.Partially_Accepted);
        }

        gameRepository.save(game);
        applicationRepository.save(app);

        return ResponseEntity.ok("✅ Allocated " + ticketsGranted + " ticket(s) to " +
                app.getMember().getFirstName() + " " + app.getMember().getLastName() +
                ". Remaining pool: " + game.getAvailableTickets());
    }

    // Deallocate (reverse) an accepted application back to Pending — returns tickets to pool
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

    // Reject an application outright (no ticket change needed)
    @PostMapping("/applications/{appId}/reject")
    public ResponseEntity<String> rejectApplication(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        app.setStatus(ApplicationStatus.Rejected);
        applicationRepository.save(app);
        return ResponseEntity.ok("❌ Application rejected for " +
                app.getMember().getFirstName() + " " + app.getMember().getLastName() + ".");
    }

    // Close a game — locks applications and finalises allocation
    @PostMapping("/games/{gameId}/close")
    public ResponseEntity<String> closeGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found."));

        // Auto-reject any applications still Pending when admin closes the game
        List<Application> pending = applicationRepository.findByGameIdAndStatus(gameId, ApplicationStatus.Pending);
        pending.forEach(app -> app.setStatus(ApplicationStatus.Rejected));
        applicationRepository.saveAll(pending);

        game.setApplicationsOpen(false);
        gameRepository.save(game);

        return ResponseEntity.ok("🔒 Arsenal vs " + game.getOpponent() +
                " closed. " + pending.size() + " pending application(s) auto-rejected.");
    }
}
