package com.arsenal.lebanon.manager.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "games")
@Data
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String opponent;
    private LocalDate matchDate;

    private String category; // "A", "B", or "C"
    private Competition competition;
    private int availableTickets;

    private boolean applicationsOpen;
}
