package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import com.arsenal.lebanon.manager.model.MembershipStatus;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.service.PriorityScoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PriorityScoreService priorityScoreService;

    @InjectMocks
    private ApplicationController controller;

    @Test
    void applyForTicketsShouldRejectWhenArsenalMembershipNumberIsMissing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("member@example.com", null, List.of())
        );

        Member member = new Member();
        member.setEmail("member@example.com");
        member.setStatus(MembershipStatus.Active);
        member.setMemberType(MemberType.Default);
        member.setArsenalMembershipNumber(null);

        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));

        ResponseEntity<String> response = controller.applyForTickets(1L, 2, true, null);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().contains("email the club"));
    }
}
