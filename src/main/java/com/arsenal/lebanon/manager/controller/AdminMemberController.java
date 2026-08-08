package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.dto.MemberSummaryDTO;
import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/pending")
    public List<MemberSummaryDTO> getPendingMembers() {
        return memberRepository.findByStatus(MembershipStatus.Pending)
                .stream()
                .map(MemberSummaryDTO::from)
                .toList();
    }

    @GetMapping("/lapsed")
    public List<MemberSummaryDTO> getLapsedMembers() {
        return memberRepository.findByStatus(MembershipStatus.Lapsed)
                .stream()
                .map(MemberSummaryDTO::from)
                .toList();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approveMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        member.setStatus(MembershipStatus.Active);
        memberRepository.save(member);

        try {
            emailService.sendApprovalEmail(member);
        } catch (Exception e) {
            System.out.println("⚠️ Approval email failed for " + member.getEmail() + ": " + e.getMessage());
        }

        return ResponseEntity.ok("✅ " + member.getFirstName() + " " + member.getLastName() + " approved and activated.");
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<String> renewMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        if (member.getStatus() != MembershipStatus.Lapsed) {
            return ResponseEntity.badRequest().body("❌ Only Lapsed members can be renewed.");
        }
        member.setStatus(MembershipStatus.Active);
        member.setExpiryDate(LocalDate.now().plusYears(1));
        memberRepository.save(member);

        try {
            emailService.sendApprovalEmail(member);
        } catch (Exception e) {
            System.out.println("⚠️ Approval email failed for " + member.getEmail() + ": " + e.getMessage());
        }

        return ResponseEntity.ok("✅ " + member.getFirstName() + " " + member.getLastName() +
                " renewed and set to Active. New expiry: " + member.getExpiryDate() + ".");
    }

    @DeleteMapping("/{id}/reject")
    public ResponseEntity<String> rejectMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        String name = member.getFirstName() + " " + member.getLastName();
        memberRepository.delete(member);
        return ResponseEntity.ok("🗑️ Registration for " + name + " has been rejected and removed.");
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<String> banMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        member.setStatus(MembershipStatus.Banned);
        memberRepository.save(member);
        return ResponseEntity.ok("🚫 " + member.getFirstName() + " " + member.getLastName() + " has been banned.");
    }

    @PostMapping("/{id}/penalize")
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

    @PostMapping("/{id}/change-type")
    public ResponseEntity<String> changeMemberType(@PathVariable Long id, @RequestParam String memberType) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        try {
            MemberType type = MemberType.valueOf(memberType);
            member.setMemberType(type);
            memberRepository.save(member);
            return ResponseEntity.ok("✅ " + member.getFirstName() + " " + member.getLastName() +
                    "'s member type updated to " + type.name() + ".");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("❌ Invalid member type: " + memberType);
        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<String> deleteMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        String name = member.getFirstName() + " " + member.getLastName();
        memberRepository.delete(member);
        return ResponseEntity.ok("🗑️ Member " + name + " has been permanently deleted.");
    }

    @PostMapping("/{id}/reset-penalty")
    public ResponseEntity<String> resetPenaltyPoints(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        if (member.getCustomPenaltyPoints() == 0) {
            return ResponseEntity.badRequest().body("❌ " + member.getFirstName() + " " + member.getLastName() +
                    " has no penalty points to reset.");
        }

        member.setCustomPenaltyPoints(0);
        memberRepository.save(member);
        return ResponseEntity.ok("✅ Penalty points reset for " +
                member.getFirstName() + " " + member.getLastName() + ".");
    }

    @PostMapping("/email")
    public ResponseEntity<String> sendEmailToMembers(@RequestBody java.util.Map<String, String> payload) {
        String filter  = payload.getOrDefault("filter", "All");
        String subject = payload.getOrDefault("subject", "");
        String body    = payload.getOrDefault("body", "");

        List<Member> recipients;
        switch (filter) {
            case "Active":
                recipients = memberRepository.findByStatus(MembershipStatus.Active);
                break;
            case "Lapsed":
                recipients = memberRepository.findByStatus(MembershipStatus.Lapsed);
                break;
            case "Pending":
                recipients = memberRepository.findByStatus(MembershipStatus.Pending);
                break;
            case "Banned":
                recipients = memberRepository.findByStatus(MembershipStatus.Banned);
                break;
            case "All":
                recipients = memberRepository.findAll();
                break;
            default:
                return ResponseEntity.badRequest().body("❌ Invalid filter: " + filter);
        }

        try {
            emailService.sendBulkEmail(recipients, subject, body);
            return ResponseEntity.ok("📧 Emails queued to " + recipients.size() + " member(s).");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Failed to send emails: " + e.getMessage());
        }
    }
}