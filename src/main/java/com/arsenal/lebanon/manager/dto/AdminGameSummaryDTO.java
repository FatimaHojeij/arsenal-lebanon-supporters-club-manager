package com.arsenal.lebanon.manager.dto;

import com.arsenal.lebanon.manager.model.Competition;
import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.model.GameCategory;

import java.time.LocalDate;

public record AdminGameSummaryDTO(
        Long id,
        String opponent,
        LocalDate matchDate,
        LocalDate deadline,
        GameCategory category,
        Competition competition,
        int availableTickets,
        boolean applicationsOpen,
        int applicationCount,
        int ticketsRequestedTotal,
        int pendingApplicationCount
) {
    public static AdminGameSummaryDTO from(Game g, int applicationCount, int ticketsRequestedTotal, int pendingApplicationCount) {
        return new AdminGameSummaryDTO(
                g.getId(),
                g.getOpponent(),
                g.getMatchDate(),
                g.getDeadline(),
                g.getCategory(),
                g.getCompetition(),
                g.getAvailableTickets(),
                g.isApplicationsOpen(),
                applicationCount,
                ticketsRequestedTotal,
                pendingApplicationCount
        );
    }
}