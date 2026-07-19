package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import com.arsenal.lebanon.manager.model.MembershipStatus;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.security.JwtUtil;
import com.arsenal.lebanon.manager.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController controller;

    @Test
    void forgotPasswordShouldGenerateAndSaveTemporaryPassword() {
        Member member = new Member();
        member.setEmail("member@example.com");
        member.setPassword("old-password");
        member.setStatus(MembershipStatus.Active);
        member.setMemberType(MemberType.Default);

        Mockito.when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        Mockito.when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp");

        ResponseEntity<?> response = controller.forgotPassword("member@example.com");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("encoded-temp", member.getPassword());
        assertTrue(response.getBody().toString().contains("temporary password"));
        Mockito.verify(emailService).sendPasswordResetEmail(Mockito.eq(member), anyString());
    }
}
