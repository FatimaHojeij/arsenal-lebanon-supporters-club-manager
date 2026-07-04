package com.arsenal.lebanon.manager.scheduler;

import com.arsenal.lebanon.manager.service.GameService;
import com.arsenal.lebanon.manager.service.MembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@EnableScheduling
public class SchedulerService {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private GameService gameService;

    @Scheduled(cron = "0 0 0 1 8 *")  // midnight, 1st August, every year
    public void runSeasonReset() {
        System.out.println("🔄 Season reset started: " + LocalDate.now());
        membershipService.resetSeasonStats();
        System.out.println("✅ Season reset complete — gamesAttendedThisSeason and categoryAGamesThisSeason cleared.");
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyMaintenance() {
        System.out.println("⏰ Daily maintenance started: " + LocalDate.now());

        membershipService.checkAndLapseExpiredMemberships();
        gameService.closeExpiredGames();

        System.out.println("✅ Daily maintenance complete.");
    }
}