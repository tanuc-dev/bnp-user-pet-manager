package com.example.demo.dto;

import com.example.demo.model.Gender;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new user.
 * <p>
 * Contains the necessary fields required to create a user entry.
 *
 * @param name      the name of the user; must not be blank
 * @param firstName the first name of the user; must not be blank
 * @param age      the age of the user; must be between 0 and 150
 * @param gender   the gender of the user; must not be null
 */
public record UserCreateDto(
        @NotBlank String name,
        @NotBlank String firstName,
        @Min(0) @Max(150) Integer age,
        @NotNull Gender gender,
        @NotNull AddressCreateDto address) {
}
