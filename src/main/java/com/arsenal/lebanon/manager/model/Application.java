package com.arsenal.lebanon.manager.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private int ticketsRequested;

    @Column(comment="Number of tickets granted")
    private int ticketsGranted;

    @Column(comment="The calculated priority of this application based on the applicant")
    private int calculatedPriorityScore;

    @Column(comment="If true either allocate all tickets or none")
    private boolean allOrNothing;

    @Column(comment= "null = not yet marked, true = attended, false = defaulted")
    Boolean attended;

    @Enumerated(EnumType.STRING)
    @Column(comment= "The status the member was last emailed about — used to avoid re-notifying unless the outcome changes")
    private ApplicationStatus lastNotifiedStatus;

    @Column(comment= "The ticket count the member was last emailed about — used alongside lastNotifiedStatus to detect changes")
    private Integer lastNotifiedTicketsGranted;

    @ElementCollection
    @CollectionTable(
            name = "application_ticket_holders",
            joinColumns = @JoinColumn(name = "application_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "holder_name")
    @Comment("Names of the ticket holders, required when more than one ticket is requested")
    private List<String> ticketHolderNames = new ArrayList<>();
}