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
     * the outcome differs from what they were last told — either the status
     * itself changed (e.g. Partially_Accepted -> Accepted, or -> Rejected),
     * or the status is the same but the ticket count changed (e.g. still
     * Partially_Accepted but granted tickets went from 2 to 1).
     *
     * When it's a genuine change (not a first-time notification), the email
     * itself is told what the member was previously informed, so they know
     * which of the two emails to actually go by.
     *
     * Reverting to Pending (e.g. via deallocate/undo-reject) is never emailed,
     * and does not overwrite lastNotifiedStatus/lastNotifiedTicketsGranted —
     * so if the application later lands on a new final outcome, it's still
     * compared against the last thing the member was actually told.
     */
    public void notifyIfChanged(Application app) {
        ApplicationStatus current = app.getStatus();

        if (current != ApplicationStatus.Accepted &&
                current != ApplicationStatus.Partially_Accepted &&
                current != ApplicationStatus.Rejected) {
            return; // Pending isn't a final outcome — nothing to email
        }

        boolean isTicketedOutcome = current == ApplicationStatus.Accepted
                || current == ApplicationStatus.Partially_Accepted;

        ApplicationStatus previousStatus = app.getLastNotifiedStatus();
        Integer previousTickets = app.getLastNotifiedTicketsGranted();

        boolean statusUnchanged = current == previousStatus;
        boolean ticketsUnchanged = !isTicketedOutcome
                || Integer.valueOf(app.getTicketsGranted()).equals(previousTickets);

        if (statusUnchanged && ticketsUnchanged) {
            return; // outcome hasn't actually changed since we last told them
        }

        Game game = app.getGame();

        try {
            if (current == ApplicationStatus.Rejected) {
                emailService.sendRejectionEmail(app.getMember(), game.getOpponent(), game.getMatchDate(),
                        previousStatus, previousTickets);
            } else {
                emailService.sendTicketAllocationEmail(
                        app.getMember(),
                        game.getOpponent(),
                        app.getTicketsGranted(),
                        game.getMatchDate(),
                        game.getCategory(),
                        previousStatus,
                        previousTickets);
            }
            app.setLastNotifiedStatus(current);
            app.setLastNotifiedTicketsGranted(isTicketedOutcome ? app.getTicketsGranted() : null);
            applicationRepository.save(app);
        } catch (Exception e) {
            System.out.println("⚠️ Notification email failed for application " + app.getId() + ": " + e.getMessage());
        }
    }
}