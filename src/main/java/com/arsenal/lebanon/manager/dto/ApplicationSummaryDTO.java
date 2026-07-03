package com.arsenal.lebanon.manager.dto;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.ApplicationStatus;
import com.arsenal.lebanon.manager.model.Game;

import java.time.LocalDateTime;

public record ApplicationSummaryDTO(
        Long id,
        MemberSummaryDTO member,
        Game game,
        LocalDateTime appliedAt,
        ApplicationStatus status,
        int ticketsRequested,
        int ticketsGranted,
        int calculatedPriorityScore,
        boolean allOrNothing
) {
    public static ApplicationSummaryDTO from(Application a) {
        return new ApplicationSummaryDTO(
                a.getId(),
                MemberSummaryDTO.from(a.getMember()),
                a.getGame(),
                a.getAppliedAt(),
                a.getStatus(),
                a.getTicketsRequested(),
                a.getTicketsGranted(),
                a.getCalculatedPriorityScore(),
                a.isAllOrNothing()
        );
    }
}