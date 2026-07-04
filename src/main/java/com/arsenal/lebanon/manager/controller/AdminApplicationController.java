package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.ApplicationStatus;
import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.model.MembershipStatus;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/applications")
public class AdminApplicationController {
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private AttendanceService attendanceService;

    @Transactional
    @PostMapping("/{appId}/deallocate")
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

    @PostMapping("/{appId}/reject")
    public ResponseEntity<String> rejectApplication(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        app.setStatus(ApplicationStatus.Rejected);
        applicationRepository.save(app);
        return ResponseEntity.ok("❌ Application rejected for " +
                app.getMember().getFirstName() + " " + app.getMember().getLastName() + ".");
    }

    @PostMapping("/{appId}/unreject")
    public ResponseEntity<String> unrejectApplication(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (!app.getGame().isApplicationsOpen()) {
            return ResponseEntity.badRequest().body("❌ Cannot restore — this game's applications are closed.");
        }

        if (app.getStatus() != ApplicationStatus.Rejected) {
            return ResponseEntity.badRequest().body("❌ Only Rejected applications can be restored.");
        }

        app.setStatus(ApplicationStatus.Pending);
        applicationRepository.save(app);

        return ResponseEntity.ok("↩️ Application restored to Pending for " +
                app.getMember().getFirstName() + " " + app.getMember().getLastName() + ".");
    }

    @Transactional
    @PostMapping("/{appId}/allocate")
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
    @PostMapping("/{appId}/mark-attendance")
    public ResponseEntity<String> markAttendance(
            @PathVariable Long appId,
            @RequestParam boolean attended) {

        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (app.getStatus() != ApplicationStatus.Accepted &&
                app.getStatus() != ApplicationStatus.Partially_Accepted) {
            return ResponseEntity.badRequest()
                    .body("❌ Attendance can only be marked for Accepted or Partially Accepted applications.");
        }

        if (app.getGame().getMatchDate().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body("❌ Cannot mark attendance — match has not taken place yet.");
        }

        if (app.getAttended() != null) {
            return ResponseEntity.badRequest()
                    .body("❌ Attendance has already been recorded for this application.");
        }

        attendanceService.markAttendance(app, attended);

        String outcome = attended ? "Attended" : "Defaulted";
        return ResponseEntity.ok("✅ " + app.getMember().getFirstName() + " " +
                app.getMember().getLastName() + " marked as " + outcome + ".");
    }

    @Transactional
    @DeleteMapping("/{appId}/cancel")
    public ResponseEntity<String> cancelApplication(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (app.getStatus() != ApplicationStatus.Accepted &&
                app.getStatus() != ApplicationStatus.Partially_Accepted) {
            return ResponseEntity.badRequest()
                    .body("❌ Only Accepted or Partially Accepted applications can be cancelled.");
        }

        if (!app.getGame().getMatchDate().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body("❌ Cannot cancel — the match date has already passed.");
        }

        Game game = app.getGame();
        game.setAvailableTickets(game.getAvailableTickets() + app.getTicketsGranted());
        gameRepository.save(game);

        String memberName = app.getMember().getFirstName() + " " +
                app.getMember().getLastName();
        applicationRepository.delete(app);

        return ResponseEntity.ok("🚫 Application for " + memberName +
                " cancelled. " + app.getTicketsGranted() +
                " ticket(s) returned to pool. Remaining: " + game.getAvailableTickets());
    }
}
