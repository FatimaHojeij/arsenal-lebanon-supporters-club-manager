package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.dto.GameRequest;
import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.repository.GameRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    // Get all matches
    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    // Get only games currently open for applications
    @GetMapping("/open")
    public List<Game> getOpenGames() {
        return gameRepository.findByApplicationsOpen(true);
    }

    @PostMapping("/create")
    public ResponseEntity<String> createGame(@Valid @RequestBody GameRequest request) {
        Game game = new Game();
        game.setOpponent(request.opponent());
        game.setCategory(request.category());
        game.setAvailableTickets(request.availableTickets());
        game.setCompetition(request.competition());
        game.setMatchDate(request.matchDate());
        game.setDeadline(request.deadline());
        game.setApplicationsOpen(true);

        gameRepository.save(game);
        return ResponseEntity.ok("⚽ Match successfully created: Arsenal vs " + game.getOpponent() +
                " (Category " + game.getCategory() + ") with ID: " + game.getId());
    }
}
