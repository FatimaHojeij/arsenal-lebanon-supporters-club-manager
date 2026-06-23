package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MembershipService {

    @Autowired
    private MemberRepository memberRepository;

    public void checkAndLapseExpiredMemberships() {

        int updatedCount = memberRepository.updateExpiredMemberships();

        if (updatedCount > 0) {
            System.out.println("🔄 Membership Scan Complete: " + updatedCount + " expired members shifted to LAPSED.");
        } else {
            System.out.println("🔄 Membership Scan Complete: All accounts are currently active and up to date.");
        }
    }
}