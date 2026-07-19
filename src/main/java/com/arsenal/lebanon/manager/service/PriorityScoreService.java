package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.GameCategory;
import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import org.springframework.stereotype.Service;

@Service
public class PriorityScoreService {

    public int calculate(Member member, GameCategory gameCategory) {
        int score = 0;

        if(member.getTotalGamesAttended() == 0)
        {
            score +=100;
        }
        score += (member.getTotalGamesAttended() * 2);
        score -= (member.getGamesAttendedThisSeason() * 2);
        if(gameCategory == GameCategory.A && member.getCategoryAGamesThisSeason()>0)
        {
            score -= 100;
        }
        score -= (member.getDefaultedGamesCount() * 5);
        score -= member.getCustomPenaltyPoints();
        score += member.getMemberType()!= MemberType.Default? 20 : 0;

        return score;
    }
}