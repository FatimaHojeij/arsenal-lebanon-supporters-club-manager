package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.model.GameCategory;
import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.MemberRepository;
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

    /** Called when tickets are allocated: the member is assumed to attend. */
    @Transactional
    public void recordAttendance(Application app) {
        if (app.getAttended() != null) return; // already recorded

        Member member = app.getMember();
        Game game = app.getGame();

        member.setTotalGamesAttended(member.getTotalGamesAttended() + 1);
        member.setGamesAttendedThisSeason(member.getGamesAttendedThisSeason() + 1);
        if (game.getCategory() == GameCategory.A) {
            member.setCategoryAGamesThisSeason(member.getCategoryAGamesThisSeason() + 1);
        }

        app.setAttended(true);
        applicationRepository.save(app);
        saveAndRescore(member);
    }

    /** Called when an allocation is undone or cancelled: undo the automatic attendance. */
    @Transactional
    public void revertAttendance(Application app) {
        if (!Boolean.TRUE.equals(app.getAttended())) return;

        Member member = app.getMember();
        decrementAttendedStats(member, app.getGame());

        app.setAttended(null);
        applicationRepository.save(app);
        saveAndRescore(member);
    }

    /** Admin marks a member as having not shown up. */
    @Transactional
    public void markDefaulted(Application app) {
        Member member = app.getMember();

        // Only reverse the attendance if it was actually counted.
        // (Legacy applications with attended == null were never counted.)
        if (Boolean.TRUE.equals(app.getAttended())) {
            decrementAttendedStats(member, app.getGame());
        }
        member.setDefaultedGamesCount(member.getDefaultedGamesCount() + 1);

        app.setAttended(false);
        applicationRepository.save(app);
        saveAndRescore(member);
    }

    private void decrementAttendedStats(Member member, Game game) {
        member.setTotalGamesAttended(Math.max(0, member.getTotalGamesAttended() - 1));
        // max(0, …) because season stats may have been reset on 1 Aug since the match
        member.setGamesAttendedThisSeason(Math.max(0, member.getGamesAttendedThisSeason() - 1));
        if (game.getCategory() == GameCategory.A) {
            member.setCategoryAGamesThisSeason(Math.max(0, member.getCategoryAGamesThisSeason() - 1));
        }
    }

    private void saveAndRescore(Member member) {
        memberRepository.save(member);

        List<Application> rescorable = applicationRepository.findRescorableApplications(member);
        rescorable.forEach(a ->
                a.setCalculatedPriorityScore(priorityScoreService.calculate(member, a.getGame().getCategory())));
        applicationRepository.saveAll(rescorable);
    }
}