package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.dto.LoginRequest;
import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        Optional<Member> memberOpt = memberRepository.findByEmail(request.email());

        if (memberOpt.isEmpty() || !passwordEncoder.matches(request.password(), memberOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Error: Invalid email or password.");
        }

        String token = jwtUtil.generateToken(memberOpt.get().getEmail());
        return ResponseEntity.ok("Bearer " + token);
    }
}
