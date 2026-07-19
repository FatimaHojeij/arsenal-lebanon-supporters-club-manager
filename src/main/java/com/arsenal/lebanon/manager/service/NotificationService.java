package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.ApplicationStatus;
import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationRepository applicationRepository;

    /**
     * Emails the member about their application's current outcome, but only if
     * that outcome differs from the last one they were emailed about.
     *
     * Reverting to Pending (e.g. via deallocate/undo-reject) is never emailed,
     * and does not overwrite lastNotifiedStatus — so if the application later
     * lands on a new final outcome, it's still compared against the last thing
     * the member was actually told, and a fresh email goes out if it differs.
     */
    public void notifyIfChanged(Application app) {
        ApplicationStatus current = app.getStatus();

        if (current == app.getLastNotifiedStatus()) {
            return; // outcome hasn't actually changed since we last told them
        }

        if (current != ApplicationStatus.Accepted &&
                current != ApplicationStatus.Partially_Accepted &&
                current != ApplicationStatus.Rejected) {
            return; // Pending isn't a final outcome — nothing to email
        }

        Game game = app.getGame();

        try {
            if (current == ApplicationStatus.Rejected) {
                emailService.sendRejectionEmail(app.getMember(), game.getOpponent(), game.getMatchDate());
            } else {
                emailService.sendTicketAllocationEmail(
                        app.getMember(),
                        game.getOpponent(),
                        app.getTicketsGranted(),
                        game.getMatchDate(),
                        game.getCategory());
            }
            app.setLastNotifiedStatus(current);
            applicationRepository.save(app);
        } catch (Exception e) {
            System.out.println("⚠️ Notification email failed for application " + app.getId() + ": " + e.getMessage());
        }
    }
}