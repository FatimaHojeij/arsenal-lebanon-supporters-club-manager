package com.arsenal.lebanon.manager.dto;

import com.arsenal.lebanon.manager.model.Title;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(

        @NotNull(message = "Title is required")
        Title title,

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$",
                message = "Phone number must start with a country code (e.g. +96170123456) " +
                          "and contain 7–15 digits total"
        )
        String phoneNumber,   // optional — no @NotBlank

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country

) {}