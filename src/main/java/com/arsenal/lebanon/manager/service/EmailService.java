package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.ApplicationStatus;
import com.arsenal.lebanon.manager.model.GameCategory;
import com.arsenal.lebanon.manager.model.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${club.treasurer.whish.phone}")
    private String treasurerWhishPhone;

    public void sendApprovalEmail(Member member) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("the.arsenal.lebanon@gmail.com");
        message.setTo(member.getEmail());
        message.setSubject("Welcome to Arsenal Lebanon Supporters Club 🔴");
        message.setText(
                "Dear " + member.getTitle() + " " + member.getFirstName() + " " + member.getLastName() + ",\n\n" +
                        "We're delighted to inform you that your membership application has been approved!\n\n" +
                        "Your membership details:\n" +
                        "  ALSC Membership Number: " + member.getALSCMembershipNumber() + "\n" +
                        "  Membership valid until: " + member.getExpiryDate() + "\n\n" +
                        "You can now log in to the member portal to apply for match tickets.\n\n" +
                        "Up the Arsenal! 🔴\n" +
                        "Arsenal Lebanon Supporters Club"
        );
        mailSender.send(message);
    }

    public void sendTicketAllocationEmail(Member member, String opponent, int ticketsGranted,
                                          LocalDate matchDate, GameCategory category,
                                          ApplicationStatus previousStatus, Integer previousTicketsGranted) {
        int pricePerTicket = category.getTicketPrice();
        int totalPrice = pricePerTicket * ticketsGranted;

        String changeNotice = buildChangeNotice(previousStatus, previousTicketsGranted);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("the.arsenal.lebanon@gmail.com");
        message.setTo(member.getEmail());
        message.setSubject((changeNotice.isEmpty() ? "" : "[Updated] ") +
                "Your tickets for Arsenal vs " + opponent + " 🎟️");
        message.setText(
                "Dear " + member.getTitle() + " " + member.getFirstName() + " " + member.getLastName() + ",\n\n" +
                        changeNotice +
                        "Great news! Your ticket application has been approved.\n\n" +
                        "Match details:\n" +
                        "  Fixture:   Arsenal vs " + opponent + "\n" +
                        "  Date:      " + matchDate + "\n" +
                        "  Category:  " + category + "\n" +
                        "  Tickets:   " + ticketsGranted + "\n\n" +
                        "Amount due: $" + totalPrice + " (" + ticketsGranted + " x $" + pricePerTicket + " per ticket)\n\n" +
                        "Send the payment via Whish to: " + treasurerWhishPhone + "\n" +
                        "Please make the required payment within a week of receiving this message.\n\n" +

                        "For any questions or concerns please reply to this email.\n\n" +
                        "Up the Arsenal! 🔴\n" +
                        "Arsenal Lebanon Supporters Club"
        );
        mailSender.send(message);
    }

    public void sendRejectionEmail(Member member, String opponent, LocalDate matchDate,
                                   ApplicationStatus previousStatus, Integer previousTicketsGranted) {
        String changeNotice = buildChangeNotice(previousStatus, previousTicketsGranted);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("the.arsenal.lebanon@gmail.com");
        message.setTo(member.getEmail());
        message.setSubject((changeNotice.isEmpty() ? "" : "[Updated] ") +
                "Update on your application for Arsenal vs " + opponent);
        message.setText(
                "Dear " + member.getTitle() + " " + member.getFirstName() + " " + member.getLastName() + ",\n\n" +
                        changeNotice +
                        "Thank you for applying for tickets to the following match:\n\n" +
                        "  Fixture: Arsenal vs " + opponent + "\n" +
                        "  Date:    " + matchDate + "\n\n" +
                        "Unfortunately, we were unable to allocate you tickets for this match. " +
                        "Ticket demand is often higher than the tickets we're allocated, and priority " +
                        "is given based on attendance history and membership standing.\n\n" +
                        "We hope to see your application succeed for a future fixture.\n\n" +
                        "Up the Arsenal! 🔴\n" +
                        "Arsenal Lebanon Supporters Club"
        );
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(Member member, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("the.arsenal.lebanon@gmail.com");
        message.setTo(member.getEmail());
        message.setSubject("Your temporary password for ALSC 🔴");
        message.setText(
                "Dear " + member.getTitle() + " " + member.getFirstName() + " " + member.getLastName() + ",\n\n" +
                        "A temporary password has been generated for your account.\n\n" +
                        "Temporary password: " + temporaryPassword + "\n\n" +
                        "Please sign in immediately and change your password from your profile settings.\n\n" +
                        "Up the Arsenal! 🔴\n" +
                        "Arsenal Lebanon Supporters Club"
        );
        mailSender.send(message);
    }

    // Builds a short note describing the prior outcome so the member knows this
    // email supersedes an earlier one. Returns "" (no notice) for first-time emails.
    private String buildChangeNotice(ApplicationStatus previousStatus, Integer previousTicketsGranted) {
        if (previousStatus == null) {
            return "";
        }

        String previousDescription = switch (previousStatus) {
            case Accepted, Partially_Accepted ->
                    previousTicketsGranted + " ticket(s) allocated";
            case Rejected -> "your application had been rejected";
            default -> null;
        };

        if (previousDescription == null) {
            return "";
        }

        return "⚠️ UPDATE: This replaces our previous email about this match, where you were told: " +
                previousDescription + ". Please disregard that earlier email and go by this one instead.\n\n";
    }
}