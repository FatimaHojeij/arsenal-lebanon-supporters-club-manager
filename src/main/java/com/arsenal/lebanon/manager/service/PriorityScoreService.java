package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import org.springframework.stereotype.Service;

@Service
public class PriorityScoreService {

    /**
     * Calculates a member's priority score for ticket allocation.
     * Called at application submission time and persisted immediately.
     *
     * Scoring weights:
     *   +100 first time applicants
     *   +2  per total game attended (loyalty)
     *   -2  per game attended this season
     *   separate Category A calculation
     *   -5 per defaulted game (cancelled after receiving tickets)
     *   -1  per custom penalty point (admin-assigned)
     *   +20 for special members
     */
    public int calculate(Member member, String gameCategory) {
        int score = 0;

        if(member.getTotalGamesAttended() == 0)
        {
            score +=100;
        }
        score += (member.getTotalGamesAttended() * 2);
        score -= (member.getGamesAttendedThisSeason() * 2);
        if("A".equals(gameCategory) && member.getCategoryAGamesThisSeason()>0)
        {
            score -= 100;
        }
        score -= (member.getDefaultedGamesCount() * 5);
        score -= member.getCustomPenaltyPoints();
        score += member.getMemberType()!= MemberType.Default? 20 : 0;

        return score;
    }
}
