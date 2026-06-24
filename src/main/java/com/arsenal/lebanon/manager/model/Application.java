package com.arsenal.lebanon.manager.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications", comment="Game applications by members")
@Data
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment="Reference")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", comment="The member applying", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "game_id", comment="The game of the application", nullable = false)
    private Game game;

    @Column(comment="Time of applying")
    private LocalDateTime appliedAt;

    @Column(comment="Current status of the application")
    private ApplicationStatus status;

    @Column(comment="Number of tickets requested")
    private int tickets;

    @Column(comment="The calculated priority of this application based on the applicant")
    private int calculatedPriorityScore;

    @Column(comment="If true either allocate all tickets or none")
    private boolean allOrNothing;
}
