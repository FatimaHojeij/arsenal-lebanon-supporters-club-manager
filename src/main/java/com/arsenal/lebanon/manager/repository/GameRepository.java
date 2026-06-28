package com.arsenal.lebanon.manager.repository;

import com.arsenal.lebanon.manager.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByApplicationsOpen(boolean applicationsOpen);
    @Query("SELECT g FROM Game g WHERE g.applicationsOpen = true AND g.deadline < CURRENT_DATE")
    List<Game> findExpiredOpenGames();
}
