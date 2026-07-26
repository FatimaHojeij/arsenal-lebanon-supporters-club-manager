package com.arsenal.lebanon.manager.repository;

import com.arsenal.lebanon.manager.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByApplicationsOpen(boolean applicationsOpen);
    List<Game> findByApplicationsOpenOrderByMatchDateAsc(boolean applicationsOpen);
    List<Game> findAllByOrderByMatchDateAsc();

    @Query("SELECT g FROM Game g WHERE g.applicationsOpen = true AND g.deadline < CURRENT_DATE")
    List<Game> findExpiredOpenGames();

    List<Game> findByMatchDateBeforeOrderByMatchDateDesc(LocalDate date);

    @Query("SELECT g FROM Game g WHERE g.matchDate < :today OR (g.applicationsOpen = false AND g.matchDate >= :today) ORDER BY g.matchDate ASC")
    List<Game> findAttendanceGames(LocalDate today);

        @Query("SELECT g FROM Game g WHERE g.applicationsOpen = true " +
            "AND g.deadline >= CURRENT_DATE AND g.deadline <= :cutoff " +
            "ORDER BY g.deadline ASC")
    List<Game> findGamesWithCloseDeadlines(@Param("cutoff") LocalDate cutoff);
}