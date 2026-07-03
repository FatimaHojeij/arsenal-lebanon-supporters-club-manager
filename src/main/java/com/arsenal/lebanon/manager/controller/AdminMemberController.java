package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.dto.MemberSummaryDTO;
import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}