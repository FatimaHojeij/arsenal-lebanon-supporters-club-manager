package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import com.arsenal.lebanon.manager.model.MembershipStatus;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Admin only — enforced by SecurityConfig
    @GetMapping
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @GetMapping("/filter")
    public Optional<Member> getMembersByALSCNumber(@RequestParam long number) {
        return memberRepository.findByALSCMembershipNumber(number);
    }

    private long createNewALSCMembershipNumber(int year){
        return (year * 10000L) + memberRepository.countByRegistrationYear(year) + 1;
    }

    // Any authenticated user — returns their own profile (no password field)
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        return ResponseEntity.ok(Map.ofEntries(
                new SimpleEntry<>("firstName",              member.getFirstName()),
                new SimpleEntry<>("lastName",               member.getLastName()),
                new SimpleEntry<>("email",                  member.getEmail()),
                new SimpleEntry<>("phoneNumber",            member.getPhoneNumber() != null ? member.getPhoneNumber() : ""),
                new SimpleEntry<>("alscMembershipNumber",   member.getALSCMembershipNumber()),
                new SimpleEntry<>("status",                 member.getStatus().name()),
                new SimpleEntry<>("memberType",             member.getMemberType().name()),
                new SimpleEntry<>("joinDate",               member.getJoinDate().toString()),
                new SimpleEntry<>("expiryDate",             member.getExpiryDate().toString()),
                new SimpleEntry<>("country",                member.getCountry() != null ? member.getCountry() : ""),
                new SimpleEntry<>("totalGamesAttended",     member.getTotalGamesAttended()),
                new SimpleEntry<>("gamesAttendedThisSeason",member.getGamesAttendedThisSeason()),
                new SimpleEntry<>("categoryAGamesThisSeason",member.getCategoryAGamesThisSeason()),
                new SimpleEntry<>("defaultedGamesCount",    member.getDefaultedGamesCount())
        ));
    }

    // Public — no auth required (permitAll in SecurityConfig)
    @PostMapping("/register")
    public ResponseEntity<String> registerMember(@RequestBody Member newMember) {
        try {
            if (memberRepository.findByEmail(newMember.getEmail()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body("❌ A member with email " + newMember.getEmail() + " already exists.");
            }

            newMember.setPassword(passwordEncoder.encode(newMember.getPassword()));
            newMember.setStatus(MembershipStatus.Pending); // Awaits admin approval
            newMember.setMemberType(MemberType.Default);
            newMember.setJoinDate(LocalDate.now());
            newMember.setExpiryDate(LocalDate.now().plusYears(1));
            newMember.setALSCMembershipNumber(createNewALSCMembershipNumber(LocalDate.now().getYear()));
            newMember.setTotalGamesAttended(0);
            newMember.setGamesAttendedThisSeason(0);
            newMember.setCategoryAGamesThisSeason(0);
            newMember.setDefaultedGamesCount(0);
            newMember.setCustomPenaltyPoints(0);

            Member saved = memberRepository.save(newMember);
            return ResponseEntity.ok("🔴 Registration submitted for " + saved.getFirstName() +
                    " " + saved.getLastName() + ". Awaiting admin approval.");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("❌ Registration failed: " + e.getMessage());
        }
    }
}
