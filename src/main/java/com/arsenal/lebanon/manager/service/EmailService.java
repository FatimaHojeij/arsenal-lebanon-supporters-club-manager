package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

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
}