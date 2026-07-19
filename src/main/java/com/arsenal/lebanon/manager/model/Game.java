package com.arsenal.lebanon.manager.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "games", comment="Games available")
@Data
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment="Reference")
    private Long id;

    @Column(comment="The team Arsenal are facing")
    private String opponent;

    @Column(unique=true, comment="The date of the game")
    private LocalDate matchDate;

    @Enumerated(EnumType.STRING)
    @Column(comment="Game category: A, B, C, or NA (not yet assigned by Arsenal)")
    private GameCategory category = GameCategory.NA;

    @Enumerated(EnumType.STRING)
    @Column(comment="The Competition the game is played in")
    private Competition competition;

    @Column(comment="The deadline to apply")
    private LocalDate deadline;

    @Column(comment="The tickets we are allocated, zero when " +
            "created will be filled when Arsenal allocate tickets")
    private int availableTickets;

    @Column(comment="If applications for this game are still being accepted")
    private boolean applicationsOpen;
}