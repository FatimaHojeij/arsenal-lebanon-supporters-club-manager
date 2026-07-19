package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.service.PriorityScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PriorityScoreService priorityScoreService;

    @Transactional
    public void markAttendance(Application app, boolean attended) {
        Member member = app.getMember();
        Game game = app.getGame();

        if (attended) {
            member.setTotalGamesAttended(member.getTotalGamesAttended() + 1);
            member.setGamesAttendedThisSeason(member.getGamesAttendedThisSeason() + 1);
            if (game.getCategory() == com.arsenal.lebanon.manager.model.GameCategory.A) {
                member.setCategoryAGamesThisSeason(member.getCategoryAGamesThisSeason() + 1);
            }
        } else {
            member.setDefaultedGamesCount(member.getDefaultedGamesCount() + 1);
        }

        memberRepository.save(member);

        // Rescore pending applications on open future games
        List<Application> rescorable = applicationRepository.findRescorableApplications(member);
        rescorable.forEach(a -> {
            a.setCalculatedPriorityScore(priorityScoreService.calculate(member, a.getGame().getCategory()));
        });
        applicationRepository.saveAll(rescorable);

        app.setAttended(attended);
        applicationRepository.save(app);
    }
}