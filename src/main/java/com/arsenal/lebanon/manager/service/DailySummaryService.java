package com.arsenal.lebanon.manager.service;

import com.arsenal.lebanon.manager.model.*;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DailySummaryService {

    // Any game with a deadline within this many days (inclusive) is flagged as "close"
    private static final int CLOSE_DEADLINE_DAYS = 5;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EmailService emailService;

    public void sendDailyApplicationSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        List<Application> todaysApplications =
                applicationRepository.findByAppliedAtBetween(startOfDay, startOfTomorrow);

        long totalOpenApplications = applicationRepository.countByStatus(ApplicationStatus.Pending);

        List<Game> closeDeadlineGames =
                gameRepository.findGamesWithCloseDeadlines(today.plusDays(CLOSE_DEADLINE_DAYS));

        // game -> (pending application count, days until deadline)
        Map<Game, Long> closeDeadlineCounts = new LinkedHashMap<>();
        for (Game game : closeDeadlineGames) {
            long pendingCount = applicationRepository
                    .findByGameIdAndStatus(game.getId(), ApplicationStatus.Pending)
                    .size();
            if (pendingCount > 0) {
                closeDeadlineCounts.put(game, pendingCount);
            }
        }

        List<Member> treasurers = memberRepository.findByMemberTypeIn(Set.of(MemberType.Treasurer));

        if (treasurers.isEmpty()) {
            System.out.println("⚠️ Daily application summary skipped: no Treasurer members found.");
            return;
        }

        emailService.sendDailyApplicationSummaryEmail(
                treasurers, todaysApplications, totalOpenApplications, closeDeadlineCounts);
    }
}