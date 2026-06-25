package com.arsenal.lebanon.manager.repository;

import com.arsenal.lebanon.manager.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByApplicationsOpen(boolean applicationsOpen);
}
