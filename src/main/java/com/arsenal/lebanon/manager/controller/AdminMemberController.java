package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminMemberController {

    @Autowired
    private MemberRepository memberRepository;

    @GetMapping("/members/pending")
    public List<Member> getPendingMembers() {
        return memberRepository.findByStatus(MembershipStatus.Pending);
    }

    @PostMapping("/members/{id}/approve")
    public ResponseEntity<String> approveMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        member.setStatus(MembershipStatus.Active);
        memberRepository.save(member);
        return ResponseEntity.ok("✅ " + member.getFirstName() + " " + member.getLastName() + " approved and activated.");
    }

    @DeleteMapping("/members/{id}/reject")
    public ResponseEntity<String> rejectMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        String name = member.getFirstName() + " " + member.getLastName();
        memberRepository.delete(member);
        return ResponseEntity.ok("🗑️ Registration for " + name + " has been rejected and removed.");
    }

    @PostMapping("/members/{id}/ban")
    public ResponseEntity<String> banMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        member.setStatus(MembershipStatus.Banned);
        memberRepository.save(member);
        return ResponseEntity.ok("🚫 " + member.getFirstName() + " " + member.getLastName() + " has been banned.");
    }

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

    @PostMapping("/members/{id}/change-type")
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

    @DeleteMapping("/members/{id}/delete")
    public ResponseEntity<String> deleteMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        String name = member.getFirstName() + " " + member.getLastName();
        memberRepository.delete(member);
        return ResponseEntity.ok("🗑️ Member " + name + " has been permanently deleted.");
    }
}