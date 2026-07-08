package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.dto.LoginRequest;
import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.security.JwtUtil;
import com.arsenal.lebanon.manager.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // Admin member types — determines the role embedded in the JWT
    private static final Set<MemberType> ADMIN_TYPES = Set.of(
            MemberType.President,
            MemberType.Secretary,
            MemberType.Treasurer
    );

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Member> memberOpt = memberRepository.findByEmail(request.email());

        // Same message for both "not found" and "wrong password" — prevents user enumeration
        if (memberOpt.isEmpty() || !passwordEncoder.matches(request.password(), memberOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Invalid email or password.");
        }

        Member member = memberOpt.get();

        // Pending members cannot log in — they must wait for admin approval
        if (member.getStatus().name().equals("Pending")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("⏳ Your membership is pending admin approval. Please check back later.");
        }

        // Banned members cannot log in
        if (member.getStatus().name().equals("Banned")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("🚫 Your account has been banned. Please contact the club.");
        }

        String role = ADMIN_TYPES.contains(member.getMemberType()) ? "ADMIN" : "MEMBER";
        String token = jwtUtil.generateToken(member.getEmail(), role);

        // Return token + role so the frontend can route to the right dashboard
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", role,
                "passwordChangeRequired", member.isPasswordChangeRequired()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        if (memberOpt.isEmpty()) {
            return ResponseEntity.ok("If an account exists for that email, a temporary password has been sent.");
        }

        Member member = memberOpt.get();
        String temporaryPassword = generateTemporaryPassword();
        member.setPassword(passwordEncoder.encode(temporaryPassword));
        member.setPasswordChangeRequired(true);
        memberRepository.save(member);

        try {
            emailService.sendPasswordResetEmail(member, temporaryPassword);
        } catch (Exception e) {
            logger.error("Password reset email failed for {}", member.getEmail(), e);
            return ResponseEntity.internalServerError().body("⚠️ We could not send the password reset email right now. Check server logs for the exact cause.");
        }

        return ResponseEntity.ok("A temporary password has been sent to your email. Please sign in and change it immediately.");
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
