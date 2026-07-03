package com.arsenal.lebanon.manager.dto;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MemberType;
import com.arsenal.lebanon.manager.model.MembershipStatus;
import com.arsenal.lebanon.manager.model.Title;

import java.time.LocalDate;

public record MemberSummaryDTO(
        Long id,
        Title title,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        long ALSCMembershipNumber,
        MembershipStatus status,
        MemberType memberType,
        LocalDate joinDate,
        LocalDate expiryDate,
        String country,
        int totalGamesAttended,
        int gamesAttendedThisSeason,
        int categoryAGamesThisSeason,
        int defaultedGamesCount,
        int customPenaltyPoints,
        LocalDate dateOfBirth
) {
    public static MemberSummaryDTO from(Member m) {
        return new MemberSummaryDTO(
                m.getId(),
                m.getTitle(),
                m.getFirstName(),
                m.getLastName(),
                m.getEmail(),
                m.getPhoneNumber(),
                m.getALSCMembershipNumber(),
                m.getStatus(),
                m.getMemberType(),
                m.getJoinDate(),
                m.getExpiryDate(),
                m.getCountry(),
                m.getTotalGamesAttended(),
                m.getGamesAttendedThisSeason(),
                m.getCategoryAGamesThisSeason(),
                m.getDefaultedGamesCount(),
                m.getCustomPenaltyPoints(),
                m.getDateOfBirth()
        );
    }
}