package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.repository.ApplicationRepository;
import com.arsenal.lebanon.manager.repository.GameRepository;
import com.arsenal.lebanon.manager.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGameControllerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminGameController controller;

    @Test
    void reopenGameShouldSetApplicationsOpenAndReturnSuccess() {
        Game game = new Game();
        game.setId(1L);
        game.setOpponent("Chelsea");
        game.setApplicationsOpen(false);
        game.setMatchDate(LocalDate.now().plusDays(1));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<String> response = controller.reopenGame(1L);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(game.isApplicationsOpen());
        assertTrue(response.getBody().contains("re-opened"));
        verify(gameRepository).save(game);
    }
}
