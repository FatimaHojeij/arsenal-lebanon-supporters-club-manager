package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.Title;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired
    private MemberRepository memberRepository;

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
}