package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.dto.LoginRequest;
import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        // 1. Check if the user exists
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        // 2. Verify hashed password match
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            return "❌ Error: Invalid email or password.";
        }

        // 3. Issue the JWT token
        String token = jwtUtil.generateToken(member.getEmail());
        return "Bearer " + token;
    }
}