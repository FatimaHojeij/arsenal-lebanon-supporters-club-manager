package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.repository.GameRepository;
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

    // Create a new game — POST with a JSON body, restricted to admins in SecurityConfig
    // Example body: { "opponent": "Chelsea", "category": "A", "availableTickets": 25 }
    @PostMapping("/create")
    public ResponseEntity<String> createGame(@RequestBody Game gameRequest) {
        Game game = new Game();
        game.setOpponent(gameRequest.getOpponent());
        game.setCategory(gameRequest.getCategory());
        game.setAvailableTickets(gameRequest.getAvailableTickets());
        game.setCompetition(gameRequest.getCompetition());
        game.setMatchDate(gameRequest.getMatchDate() != null ? gameRequest.getMatchDate() : LocalDate.now().plusDays(14));
        game.setDeadline(gameRequest.getDeadline());
        game.setApplicationsOpen(true);

        gameRepository.save(game);
        return ResponseEntity.ok("⚽ Match successfully created: Arsenal vs " + game.getOpponent() +
                " (Category " + game.getCategory() + ") with ID: " + game.getId());
    }
}
