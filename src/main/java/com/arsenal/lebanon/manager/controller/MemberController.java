package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import com.arsenal.lebanon.manager.model.MembershipStatus;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = "http://localhost:3000")
public class MemberController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. Get all members
    // URL: http://localhost:8080/api/members
    @GetMapping
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    // 2. Filter members by Title (Mr, Mrs, Dr, etc.)
    // URL: http://localhost:8080/api/members/filter?title=MR
    @GetMapping("/filter")
    public Optional<Member> getMembersByALSCNumber(@RequestParam long number) {
        return memberRepository.findByALSCMembershipNumber(number);
    }

    @PostMapping("/register")
    public String registerMember(@RequestBody Member newMember) {
        try {
            // 1. Validation Check: Ensure email doesn't already exist
            if (memberRepository.findByEmail(newMember.getEmail()).isPresent()) {
                return "❌ Error: A member with email " + newMember.getEmail() + " already exists.";
            }

            // 2. Automatically apply default registration rules
            String rawPassword = newMember.getPassword();
            newMember.setPassword(passwordEncoder.encode(rawPassword));
            newMember.setStatus(MembershipStatus.Active);
            newMember.setMemberType(MemberType.Default);
            newMember.setJoinDate(LocalDate.now());
            newMember.setExpiryDate(LocalDate.now().plusYears(1)); // Membership valid for 1 year

            // Initialize point system to zero
            newMember.setTotalGamesAttended(0);
            newMember.setGamesAttendedThisSeason(0);
            newMember.setCategoryAGamesThisSeason(0);
            newMember.setDefaultedGamesCount(0);
            newMember.setCustomPenaltyPoints(0);

            // 3. Save to Supabase
            Member savedMember = memberRepository.save(newMember);
            return "🔴 Success: Registered " + savedMember.getFirstName() + " " + savedMember.getLastName() +
                    " with ALSC ID: " + savedMember.getALSCMembershipNumber();

        } catch (Exception e) {
            return "❌ Registration failed: " + e.getMessage();
        }
    }
}