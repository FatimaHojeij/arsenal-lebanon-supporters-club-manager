package com.arsenal.lebanon.manager.scheduler;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import com.arsenal.lebanon.manager.repository.MemberRepository;
import com.arsenal.lebanon.manager.service.DailySummaryService;
import com.arsenal.lebanon.manager.service.EmailService;
import com.arsenal.lebanon.manager.service.GameService;
import com.arsenal.lebanon.manager.service.MembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@EnableScheduling
public class SchedulerService {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private GameService gameService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DailySummaryService dailySummaryService;

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 0 0 1 8 *")  // midnight, 1st August, every year
    public void runSeasonReset() {
        System.out.println("🔄 Season reset started: " + LocalDate.now());
        int resetCount = membershipService.resetSeasonStats();
        System.out.println("✅ Season reset complete — gamesAttendedThisSeason and categoryAGamesThisSeason cleared.");

        if (resetCount > 0) {
            String body = "Season Reset — " + LocalDate.now() + "\n\n" +
                    "Season stats (games attended this season, Category A games this season) were reset to 0 for " +
                    resetCount + " member(s).\n\n" +
                    "Up the Arsenal! 🔴";
            sendAdminReport("🔄 ALSC Season Reset Report — " + LocalDate.now(), body);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyMaintenance() {
        System.out.println("⏰ Daily maintenance started: " + LocalDate.now());

        int lapsedCount = membershipService.checkAndLapseExpiredMemberships();
        List<GameService.GameCloseSummary> closedGames = gameService.closeExpiredGames();

        System.out.println("✅ Daily maintenance complete.");

        if (lapsedCount > 0 || !closedGames.isEmpty()) {
            StringBuilder body = new StringBuilder();
            body.append("Daily Maintenance Report — ").append(LocalDate.now()).append("\n\n");

            if (lapsedCount > 0) {
                body.append("• ").append(lapsedCount)
                        .append(" membership(s) expired and were moved to LAPSED.\n\n");
            }

            if (!closedGames.isEmpty()) {
                body.append("• ").append(closedGames.size())
                        .append(" game(s) auto-closed for new applications (deadline passed):\n");
                for (GameService.GameCloseSummary summary : closedGames) {
                    body.append("   - Arsenal vs ").append(summary.opponent())
                            .append(" (").append(summary.matchDate()).append("): ")
                            .append(summary.pendingApplicationsAwaitingAllocation())
                            .append(" pending application(s) still awaiting allocation.\n");
                }
                body.append("\n");
            }

            body.append("Up the Arsenal! 🔴");

            sendAdminReport("⏰ ALSC Daily Maintenance Report — " + LocalDate.now(), body.toString());
        }
    }

    private void sendAdminReport(String subject, String body) {
        try {
            List<Member> admins = memberRepository.findByMemberTypeIn(MemberType.ADMIN_TYPES);
            emailService.sendAdminSummaryEmail(admins, subject, body);
        } catch (Exception e) {
            System.out.println("⚠️ Admin report email failed: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 59 23 * * *")
    public void runDailyApplicationSummary() {
        System.out.println("📋 Sending daily application summary: " + LocalDate.now());
        try {
            dailySummaryService.sendDailyApplicationSummary();
            System.out.println("✅ Daily application summary sent.");
        } catch (Exception e) {
            System.out.println("⚠️ Daily application summary failed: " + e.getMessage());
        }
    }
}