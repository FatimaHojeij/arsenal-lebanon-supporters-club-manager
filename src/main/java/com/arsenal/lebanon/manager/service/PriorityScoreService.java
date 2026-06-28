package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Member;
import org.springframework.stereotype.Service;

@Service
public class PriorityScoreService {

    /**
     * Calculates a member's priority score for ticket allocation.
     * Called at application submission time and persisted immediately.
     *
     * Scoring weights:
     *   +2  per total game attended (loyalty)
     *   +5  per game attended this season (current engagement)
     *   +10 per Category A game attended this season (high-demand commitment)
     *   -15 per defaulted game (cancelled after receiving tickets)
     *   -1  per custom penalty point (admin-assigned)
     */
    public int calculate(Member member) {
        int score = 0;

        score += (member.getTotalGamesAttended() * 2);
        score += (member.getGamesAttendedThisSeason() * 5);
        score += (member.getCategoryAGamesThisSeason() * 10);
        score -= (member.getDefaultedGamesCount() * 15);
        score -= member.getCustomPenaltyPoints();

        return score;
    }
}
