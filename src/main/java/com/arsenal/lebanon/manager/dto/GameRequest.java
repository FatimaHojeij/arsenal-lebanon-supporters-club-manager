package com.arsenal.lebanon.manager.dto;

import com.arsenal.lebanon.manager.model.Competition;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record GameRequest(

        @NotBlank(message = "Opponent name is required")
        @Size(max = 100, message = "Opponent name must not exceed 100 characters")
        String opponent,

        @NotBlank(message = "Category is required")
        @Pattern(regexp = "^[ABCD]$", message = "Category must be A, B, C, or D")
        String category,

        @NotNull(message = "Competition is required")
        Competition competition,

        @NotNull(message = "Match date is required")
        @Future(message = "Match date must be in the future")
        LocalDate matchDate,

        @NotNull(message = "Application deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDate deadline,

        @Min(value = 0, message = "Available tickets cannot be negative")
        int availableTickets

) {}