package com.arsenal.lebanon.manager.dto;

import com.arsenal.lebanon.manager.model.Competition;
import com.arsenal.lebanon.manager.model.GameCategory;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record GameRequest(

        @NotBlank(message = "Opponent name is required")
        @Size(max = 100, message = "Opponent name must not exceed 100 characters")
        String opponent,

        GameCategory category,

        @NotNull(message = "Competition is required")
        Competition competition,

        @NotNull(message = "Match date is required")
        @Future(message = "Match date must be in the future")
        LocalDate matchDate,

        @NotNull(message = "Application deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDate deadline,

        @Min(value = 0, message = "Available tickets cannot be negative")
        Integer availableTickets

) {
        public int ticketsOrDefault() {
                return availableTickets != null ? availableTickets : 0;
        }

        public GameCategory categoryOrDefault() {
                return category != null ? category : GameCategory.NA;
        }
}