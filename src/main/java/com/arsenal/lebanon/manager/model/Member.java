package com.arsenal.lebanon.manager.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "members")
@Data
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Title title;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private long ALSCMembershipNumber;

    @Enumerated(EnumType.STRING)
    private MembershipStatus status;

    @Enumerated(EnumType.STRING)
    private MemberType memberType;

    private LocalDate joinDate;
    private LocalDate dateOfBirth;
    private LocalDate expiryDate;

    private String country;

    private int totalGamesAttended;
    private int gamesAttendedThisSeason;
    private int categoryAGamesThisSeason;
    private int defaultedGamesCount;
    private int customPenaltyPoints;
}
