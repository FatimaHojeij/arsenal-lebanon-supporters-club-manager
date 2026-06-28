package com.arsenal.lebanon.manager.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "members", comment="Arsenal Lebanon supporters club members")
@Data
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment="Reference")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(comment="Title of member")
    private Title title;

    @Column(nullable = false, comment="First name of member")
    private String firstName;
    @Column(nullable = false, comment="Last name of member")
    private String lastName;

    @Column(unique = true, nullable = false, comment="Contact email of member")
    private String email;

    @Column(comment="password used to login")
    private String password;

    @Column(unique = true, comment="Contact phone number of member")
    private String phoneNumber;

    @Column(unique = true, comment="Internal membership number of member")
    private long ALSCMembershipNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, comment="Membership status of the member")
    private MembershipStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, comment="Type of member, default if not a special member")
    private MemberType memberType;

    @Column(comment="Original date first joined")
    private LocalDate joinDate;
    @Column(comment="Date of birth")
    private LocalDate dateOfBirth;
    @Column(comment="Membership expiry date")
    private LocalDate expiryDate;

    @Column(comment="Country of residence")
    private String country;

    @Column(comment="Total games attended")
    private int totalGamesAttended;
    @Column(comment="Total games attended this season, to be cleared at the start of each season")
    private int gamesAttendedThisSeason;
    @Column(comment="Total games attended this season that are Category A")
    private int categoryAGamesThisSeason;
    @Column(comment="Total games that member cancelled or defaulted on after being allocated tickets")
    private int defaultedGamesCount;
    @Column(comment="Penalty points set by admin for specific behaviour")
    private int customPenaltyPoints;
}
