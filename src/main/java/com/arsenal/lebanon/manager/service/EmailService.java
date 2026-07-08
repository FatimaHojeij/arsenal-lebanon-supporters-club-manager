package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Member;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Autowired
    private JavaMailSender mailSender;

    @Value("${gmail.client-id:}")
    private String gmailClientId;

    @Value("${gmail.client-secret:}")
    private String gmailClientSecret;

    @Value("${gmail.refresh-token:}")
    private String gmailRefreshToken;

    @Value("${gmail.user-email:}")
    private String gmailUserEmail;

    private Gmail gmailService;

    @PostConstruct
    private void initGmailClient() {
        if (gmailClientId.isBlank() || gmailClientSecret.isBlank() || gmailRefreshToken.isBlank() || gmailUserEmail.isBlank()) {
            logger.info("Gmail API not configured; using SMTP mail sender.");
            return;
        }

        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(gmailClientId)
                    .setClientSecret(gmailClientSecret)
                    .setRefreshToken(gmailRefreshToken)
                    .build();

            gmailService = new Gmail.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Arsenal Lebanon Supporters Club Manager")
                    .build();

            logger.info("Gmail API mail sender initialized successfully.");
        } catch (IOException | GeneralSecurityException e) {
            logger.warn("Could not initialize Gmail API client; falling back to SMTP.", e);
            gmailService = null;
        }
    }

    public void sendApprovalEmail(Member member) {
        String subject = "Welcome to Arsenal Lebanon Supporters Club 🔴";
        String body = "Dear " + member.getTitle() + " " + member.getFirstName() + " " + member.getLastName() + ",\n\n" +
                "We're delighted to inform you that your membership application has been approved!\n\n" +
                "Your membership details:\n" +
                "  ALSC Membership Number: " + member.getALSCMembershipNumber() + "\n" +
                "  Membership valid until: " + member.getExpiryDate() + "\n\n" +
                "You can now log in to the member portal to apply for match tickets.\n\n" +
                "Up the Arsenal! 🔴\n" +
                "Arsenal Lebanon Supporters Club";
        sendEmail(member.getEmail(), subject, body);
    }

    public void sendTicketAllocationEmail(Member member, String opponent, int ticketsGranted, LocalDate matchDate) {
        String subject = "Your tickets for Arsenal vs " + opponent + " 🎟️";
        String body = "Dear " + member.getTitle() + " " + member.getFirstName() + " " + member.getLastName() + ",\n\n" +
                "Great news! Your ticket application has been approved.\n\n" +
                "Match details:\n" +
                "  Fixture:  Arsenal vs " + opponent + "\n" +
                "  Date:     " + matchDate + "\n" +
                "  Tickets:  " + ticketsGranted + "\n\n" +
                "Please make the required payment within a week of receiving this message.\n\n" +
                "Up the Arsenal! 🔴\n" +
                "Arsenal Lebanon Supporters Club";
        sendEmail(member.getEmail(), subject, body);
    }

    public void sendPasswordResetEmail(Member member, String temporaryPassword) {
        String subject = "Your temporary password for ALSC 🔴";
        String body = "Dear " + member.getTitle() + " " + member.getFirstName() + " " + member.getLastName() + ",\n\n" +
                "A temporary password has been generated for your account.\n\n" +
                "Temporary password: " + temporaryPassword + "\n\n" +
                "Please sign in immediately and change your password from your profile settings.\n\n" +
                "Up the Arsenal! 🔴\n" +
                "Arsenal Lebanon Supporters Club";
        sendEmail(member.getEmail(), subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        if (gmailService != null) {
            sendWithGmailApi(to, subject, body);
            return;
        }
        sendWithSmtp(to, subject, body);
    }

    private void sendWithSmtp(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(gmailUserEmail.isBlank() ? "the.arsenal.lebanon@gmail.com" : gmailUserEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void sendWithGmailApi(String to, String subject, String body) {
        try {
            MimeMessage email = createEmail(to, gmailUserEmail, subject, body);
            Message message = createMessageWithEmail(email);
            gmailService.users().messages().send("me", message).execute();
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send email via Gmail API", e);
        }
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            emailContent.writeTo(buffer);
            String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
            Message message = new Message();
            message.setRaw(encodedEmail);
            return message;
        }
    }
}
