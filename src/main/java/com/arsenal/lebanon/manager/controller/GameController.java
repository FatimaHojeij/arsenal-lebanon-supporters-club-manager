package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    // 1. Get all matches
    // URL: http://localhost:8080/api/games
    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    // 2. Quick create a game via browser URL parameters
    // URL: http://localhost:8080/api/games/create?opponent=Chelsea&tier=A&tickets=25
    @GetMapping("/create")
    public String createGame(
            @RequestParam String opponent,
            @RequestParam String category,
            @RequestParam int tickets) {

        Game game = new Game();
        game.setOpponent(opponent);
        game.setCategory(category);
        game.setAvailableTickets(tickets);
        game.setMatchDate(LocalDate.now().plusDays(14)); // Sets match date to 2 weeks from today
        game.setApplicationsOpen(true);

        gameRepository.save(game);
        return "⚽ Match successfully created: Arsenal vs " + opponent + " (Category " + category + ") with ID: " + game.getId();
    }
}